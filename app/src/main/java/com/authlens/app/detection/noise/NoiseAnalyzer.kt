package com.authlens.app.detection.noise

import android.graphics.Bitmap
import com.authlens.app.core.Constants
import com.authlens.app.detection.ImageUtils
import com.authlens.app.domain.model.DetectionStage
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Stage 3b: Noise/grain consistency analysis for PNG (lossless) images.
 *
 * Because PNG is lossless, sensor noise survives intact — and it should be *uniform*
 * across the whole authentic capture. A spliced region (pasted from another photo)
 * typically brings its own noise fingerprint, creating inconsistent grain patches.
 *
 * We extract the noise residual via median filtering, partition it into a grid, and
 * measure the variance of per-cell noise energy. High variance ⇒ inconsistent grain ⇒ fraud.
 */
@Singleton
class NoiseAnalyzer @Inject constructor() {

    fun analyze(bitmap: Bitmap): NoiseOutcome {
        val bgr = ImageUtils.bitmapToBgrMat(bitmap)
        val gray = Mat()
        Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY)

        // Denoise to get the "clean" signal; residual = original − denoised = noise.
        val denoised = Mat()
        Imgproc.GaussianBlur(gray, denoised, org.opencv.core.Size(GAUSSIAN_K.toDouble(), GAUSSIAN_K.toDouble()), 0.0)
        val noise = Mat()
        Core.subtract(gray, denoised, noise)

        // Absolute noise magnitude.
        val absNoise = Mat()
        Core.absdiff(noise, Scalar(0.0), absNoise)

        val cellEnergies = gridCellEnergies(absNoise, Constants.NOISE_GRID_SIZE)
        val mean = cellEnergies.average()
        val stddev = sampleStdDev(cellEnergies, mean)
        val coefficientOfVariation = if (mean <= 0.0) 0.0 else stddev / mean

        // Heatmap = noise residual upscaled to 0..255.
        val viz = Mat()
        val scale = if (mean <= 0.0) 1.0 else (255.0 / (mean * VISUAL_SCALE)).coerceAtMost(20.0)
        Core.multiply(absNoise, Scalar(scale), viz)
        Core.min(viz, Scalar(255.0), viz)
        viz.convertTo(viz, CvType.CV_8U)

        val heatmapBytes = runCatching {
            val bmp = ImageUtils.grayMatToBitmap(viz)
            val baos = java.io.ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }.getOrNull()

        val score = computeScore(coefficientOfVariation)

        bgr.release(); gray.release(); denoised.release(); noise.release(); absNoise.release(); viz.release()

        val details = buildList {
            add("Grid: ${Constants.NOISE_GRID_SIZE}×${Constants.NOISE_GRID_SIZE}")
            add("Mean noise energy: %.2f".format(mean))
            add("Grain variation (CoV): %.3f".format(coefficientOfVariation))
            add(
                when {
                    score >= LIKELY_FRAUD_SCORE -> "Grain is inconsistent across regions — consistent with splicing."
                    score >= SUSPICIOUS_SCORE -> "Some noise variation detected between regions."
                    else -> "Noise/grain is uniform across the image."
                }
            )
        }

        return NoiseOutcome(score = score, heatmapBytes = heatmapBytes, details = details)
    }

    /** Splits [noise] into [grid]×[grid] cells, returns per-cell mean energy. */
    private fun gridCellEnergies(noise: Mat, grid: Int): DoubleArray {
        val rows = noise.rows()
        val cols = noise.cols()
        val cellH = rows / grid
        val cellW = cols / grid
        if (cellH == 0 || cellW == 0) return doubleArrayOf(Core.mean(noise).`val`[0])

        val energies = DoubleArray(grid * grid)
        var idx = 0
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val roi = Mat(noise, Rect(c * cellW, r * cellH, cellW, cellH))
                energies[idx++] = Core.mean(roi).`val`[0]
                roi.release()
            }
        }
        return energies
    }

    private fun computeScore(cov: Double): Int {
        // Authentic captures usually have CoV < ~0.3. Tampered images push past 0.6.
        val part = ((cov - CLEAN_COV_FLOOR) / (FRAUD_COV_CEIL - CLEAN_COV_FLOOR))
            .coerceIn(0.0, 1.0)
        return (part * 100).toInt().coerceIn(0, 100)
    }

    private fun sampleStdDev(values: DoubleArray, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        var sum = 0.0
        for (v in values) sum += (v - mean) * (v - mean)
        return sqrt(sum / values.size)
    }

    data class NoiseOutcome(
        val score: Int,
        val heatmapBytes: ByteArray?,
        val details: List<String>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NoiseOutcome) return false
            return score == other.score && details == other.details
        }
        override fun hashCode(): Int = score * 31 + details.hashCode()
    }

    companion object {
        val STAGE = DetectionStage.INTEGRITY
        private const val GAUSSIAN_K = 5
        private const val VISUAL_SCALE = 4.0
        private const val CLEAN_COV_FLOOR = 0.30
        private const val FRAUD_COV_CEIL = 0.70
        private const val SUSPICIOUS_SCORE = 30
        private const val LIKELY_FRAUD_SCORE = 55
    }
}
