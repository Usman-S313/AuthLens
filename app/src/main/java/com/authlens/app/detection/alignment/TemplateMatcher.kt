package com.authlens.app.detection.alignment

import android.graphics.Bitmap
import com.authlens.app.core.Constants
import com.authlens.app.detection.ImageUtils
import com.authlens.app.domain.model.DetectionStage
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.KeyPoint
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Stage 2 of the pipeline: verifies the document's layout matches a reference template.
 *
 * Uses ORB feature detection + brute-force Hamming matching, then a RANSAC homography
 * check to count inliers. A genuinely aligned document yields many geometrically
 * consistent matches; a fabricated/spliced layout yields few or none.
 */
@Singleton
class TemplateMatcher @Inject constructor() {

    private val detector: ORB by lazy {
        ORB.create(
            Constants.TEMPLATE_MAX_FEATURES,
            1.2f,
            8,
            31,
            0,
            2,
            ORB.HARRIS_SCORE,
            31,
            20,
        )
    }

    private val matcher: DescriptorMatcher by lazy {
        DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
    }

    /**
     * Matches the document [bitmap] against a list of candidate reference [templates].
     *
     * @return match score 0..100 (higher = more confident the layout is authentic)
     *         and a human-readable summary.
     */
    fun match(bitmap: Bitmap, templates: List<Bitmap>): MatchOutcome {
        if (templates.isEmpty()) {
            // No references available — skip the check (neutral, not penalizing).
            return MatchOutcome(
                score = 50,
                goodMatches = 0,
                details = listOf("No reference templates available for this document type — layout check skipped."),
            )
        }

        val docMat = ImageUtils.bitmapToBgrMat(bitmap)
        val docGray = Mat()
        Imgproc.cvtColor(docMat, docGray, Imgproc.COLOR_BGR2GRAY)

        val docKp = MatOfKeyPoint()
        val docDesc = Mat()
        detector.detectAndCompute(docGray, Mat(), docKp, docDesc)

        var bestGoodMatches = 0
        var bestInliers = 0
        var bestTotalMatches = 0

        templates.forEach { tpl ->
            val tplMat = ImageUtils.bitmapToBgrMat(tpl)
            val tplGray = Mat()
            Imgproc.cvtColor(tplMat, tplGray, Imgproc.COLOR_BGR2GRAY)
            val tplKp = MatOfKeyPoint()
            val tplDesc = Mat()
            detector.detectAndCompute(tplGray, Mat(), tplKp, tplDesc)

            if (!tplDesc.empty() && !docDesc.empty()) {
                val matches = MatOfDMatch()
                matcher.match(tplDesc, docDesc, matches)
                val rawList = matches.toList()
                bestTotalMatches = max(bestTotalMatches, rawList.size)

                // Lowe-style good match filter using a distance threshold (Hamming).
                val good = rawList.filter { it.distance < GOOD_MATCH_DISTANCE }
                if (good.size >= MIN_MATCHES_FOR_HOMOGRAPHY && tplKp.rows() > 0 && docKp.rows() > 0) {
                    val inliers = ransacInliers(good, tplKp, docKp)
                    if (inliers > bestInliers) {
                        bestInliers = inliers
                        bestGoodMatches = good.size
                    }
                } else if (good.size > bestGoodMatches) {
                    bestGoodMatches = good.size
                }
            }

            tplMat.release(); tplGray.release(); tplDesc.release()
        }

        docMat.release(); docGray.release(); docDesc.release()

        val score = scoreFromMatches(bestInliers, bestGoodMatches)
        val passed = bestInliers >= Constants.TEMPLATE_MIN_GOOD_MATCHES

        val details = buildList {
            add("Reference templates compared: ${templates.size}")
            add("Best raw matches: $bestTotalMatches")
            add("Geometrically consistent (inliers): $bestInliers")
            add(if (passed) "✓ Layout consistency above authenticity threshold." else "✗ Layout does not match references convincingly.")
        }

        return MatchOutcome(
            score = score,
            goodMatches = bestGoodMatches,
            inliers = bestInliers,
            passed = passed,
            details = details,
        )
    }

    /**
     * Estimates a homography between template and document keypoints via RANSAC and
     * returns the number of inlier matches.
     */
    private fun ransacInliers(
        good: List<org.opencv.core.DMatch>,
        tplKp: MatOfKeyPoint,
        docKp: MatOfKeyPoint,
    ): Int {
        val tplArr = tplKp.toArray()
        val docArr = docKp.toArray()
        val srcPts = good.mapNotNull { d -> docArr.get(d.trainIdx) }.toTypedArray()
        val dstPts = good.mapNotNull { d -> tplArr.get(d.queryIdx) }.toTypedArray()
        if (srcPts.size < 4 || dstPts.size < 4) return 0

        val src = MatOfPoint2f(*srcPts.map { kp: KeyPoint -> Point(kp.pt.x, kp.pt.y) }.toTypedArray())
        val dst = MatOfPoint2f(*dstPts.map { kp: KeyPoint -> Point(kp.pt.x, kp.pt.y) }.toTypedArray())
        val mask = Mat()
        runCatching {
            // 5-arg overload: src, dst, method, ransacReprojThreshold, output mask.
            Calib3d.findHomography(src, dst, Calib3d.RANSAC, 5.0, mask)
        }
        val inliers = if (mask.empty()) 0 else Core.countNonZero(mask)
        mask.release(); src.release(); dst.release()
        return inliers
    }

    /** Maps inlier count to a 0..100 authenticity score (templated). */
    private fun scoreFromMatches(inliers: Int, goodMatches: Int): Int {
        val byInliers = (inliers.toFloat() / Constants.TEMPLATE_MIN_GOOD_MATCHES).coerceIn(0f, 1f)
        val byGood = (goodMatches.toFloat() / (Constants.TEMPLATE_MIN_GOOD_MATCHES * 3f)).coerceIn(0f, 1f)
        // Authenticity score; we want HIGH score = authentic = LOW fraud contribution.
        val authenticity = (0.7 * byInliers + 0.3 * byGood)
        // Convert to fraud score (inverted): low authenticity → high fraud score.
        val fraudScore = ((1.0 - authenticity) * 100).toInt()
        return fraudScore.coerceIn(0, 100)
    }

    /** Outcome of the template-matching stage. */
    data class MatchOutcome(
        /** 0..100 fraud contribution (higher = more suspicious). */
        val score: Int,
        val goodMatches: Int,
        val inliers: Int = 0,
        val passed: Boolean = false,
        val details: List<String>,
    ) {
        /** When layout fails badly, the pipeline treats this as a reject signal. */
        val isReject: Boolean get() = goodMatches < MIN_MATCHES_FOR_HOMOGRAPHY
    }

    companion object {
        val STAGE = DetectionStage.TEMPLATE
        private const val GOOD_MATCH_DISTANCE = 64.0
        private const val MIN_MATCHES_FOR_HOMOGRAPHY = 10

        /** Convenience: rough distance between two OpenCV points. */
        @Suppress("unused")
        private fun distance(a: Point, b: Point): Double =
            sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))
    }
}
