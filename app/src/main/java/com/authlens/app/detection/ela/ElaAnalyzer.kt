package com.authlens.app.detection.ela

import android.graphics.Bitmap
import com.authlens.app.core.Constants
import com.authlens.app.detection.ImageUtils
import com.authlens.app.domain.model.DetectionStage
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Stage 3a: Error Level Analysis for JPEG images.
 *
 * ELA re-saves the image at a fixed quality, then compares it to the original.
 * Tampered/edited regions — which were typically re-encoded at a *different* quality
 * than the surrounding authentic pixels — stand out as localized bright anomalies in
 * the error map.
 *
 * Produces both a numeric anomaly score and a heatmap PNG preview.
 */
@Singleton
class ElaAnalyzer @Inject constructor() {

    /**
     * @param original the decoded JPEG bitmap
     * @return [ElaOutcome] with score + heatmap bytes
     */
    fun analyze(original: Bitmap): ElaOutcome {
        // Re-encode at fixed quality then decode back to a bitmap of matching geometry.
        val (_, reencoded) = ImageUtils.reencodeJpeg(original, Constants.ELA_JPEG_QUALITY)

        val origMat = ImageUtils.bitmapToBgrMat(original)
        val reencMat = ImageUtils.bitmapToBgrMat(reencoded)

        // Absolute per-pixel difference across all channels.
        val diff = Mat()
        Core.absdiff(origMat, reencMat, diff)

        // Convert to grayscale magnitude (max of B,G,R) to emphasize any-channel error.
        val gray = Mat()
        Imgproc.cvtColor(diff, gray, Imgproc.COLOR_BGR2GRAY)

        val mean = Core.mean(gray).`val`[0]
        val stddev = scalarStdDev(gray, mean)

        // Scaled visualization (0..255) for the heatmap.
        val viz = Mat()
        val scale = if (mean <= 0.0) 1.0 else (255.0 / (mean * VISUAL_SCALE)).coerceAtMost(20.0)
        Core.multiply(gray, Scalar(scale), viz)
        Core.min(viz, Scalar(255.0), viz)
        viz.convertTo(viz, CvType.CV_8U)

        val heatmapBytes = runCatching {
            val bmp = ImageUtils.grayMatToBitmap(viz)
            val baos = java.io.ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }.getOrNull()

        // Score: combine global mean error (low for clean images) with local hotspot count.
        val hotspotRatio = hotspotRatio(gray, mean + stddev * HOTSPOT_STD_FACTOR)
        val score = computeScore(mean, hotspotRatio)

        origMat.release(); reencMat.release(); diff.release(); gray.release(); viz.release()

        val details = buildList {
            add("Mean pixel error: %.2f".format(mean))
            add("Error deviation: %.2f".format(stddev))
            add("Anomaly hotspot ratio: %.1f%%".format(hotspotRatio * 100))
            add(
                if (score >= LIKELY_FRAUD_SCORE) "Localized error-level anomalies detected — consistent with editing."
                else if (score >= SUSPICIOUS_SCORE) "Some error-level variation present."
                else "Error levels are uniform across the image."
            )
        }

        return ElaOutcome(score = score, heatmapBytes = heatmapBytes, details = details)
    }

    /** Ratio of pixels whose error exceeds [threshold]. */
    private fun hotspotRatio(gray: Mat, threshold: Double): Double {
        val total = gray.rows() * gray.cols()
        if (total == 0) return 0.0
        val mask = Mat()
        Imgproc.threshold(gray, mask, threshold, 255.0, Imgproc.THRESH_BINARY)
        val hot = Core.countNonZero(mask)
        mask.release()
        return hot.toDouble() / total
    }

    private fun computeScore(meanError: Double, hotspotRatio: Double): Int {
        // Mean error near 0 for clean images; > 5 typically indicates prior JPEG re-saves / edits.
        val meanPart = (meanError / CLEAN_MEAN_CEIL).coerceIn(0.0, 1.0) * 0.55
        val hotspotPart = hotspotRatio.coerceIn(0.0, 1.0) * 0.45
        return ((meanPart + hotspotPart) * 100).toInt().coerceIn(0, 100)
    }

    private fun scalarStdDev(gray: Mat, mean: Double): Double {
        val tmp = Mat()
        Core.subtract(gray, Scalar(mean), tmp)
        Core.multiply(tmp, tmp, tmp)
        val meanSq = Core.mean(tmp).`val`[0]
        tmp.release()
        return sqrt(meanSq)
    }

    data class ElaOutcome(
        /** 0..100 fraud contribution. */
        val score: Int,
        val heatmapBytes: ByteArray?,
        val details: List<String>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ElaOutcome) return false
            return score == other.score && details == other.details
        }
        override fun hashCode(): Int = score * 31 + details.hashCode()
    }

    companion object {
        val STAGE = DetectionStage.INTEGRITY
        private const val VISUAL_SCALE = 6.0
        private const val HOTSPOT_STD_FACTOR = 3.0
        private const val CLEAN_MEAN_CEIL = 6.0
        private const val SUSPICIOUS_SCORE = 30
        private const val LIKELY_FRAUD_SCORE = 55
    }
}
