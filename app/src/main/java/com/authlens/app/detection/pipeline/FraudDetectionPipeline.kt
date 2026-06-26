package com.authlens.app.detection.pipeline

import android.content.Context
import com.authlens.app.detection.ImageUtils
import com.authlens.app.detection.ela.ElaAnalyzer
import com.authlens.app.detection.metadata.MetadataAnalyzer
import com.authlens.app.detection.noise.NoiseAnalyzer
import com.authlens.app.detection.alignment.TemplateMatcher
import com.authlens.app.detection.scoring.FraudScoreCalculator
import com.authlens.app.detection.template.DocumentTemplateStore
import com.authlens.app.domain.model.CheckResult
import com.authlens.app.domain.model.DetectionStage
import com.authlens.app.domain.model.DocumentInput
import com.authlens.app.domain.model.FraudResult
import com.authlens.app.domain.model.ImageFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full fraud-detection flow, matching the system architecture:
 *
 * ```
 *  [Document]
 *     │
 *     ▼
 *  1. Metadata ──► (editing software? → terminal HIGH RISK)
 *     │
 *     ▼
 *  2. Template matching ──► (fake layout? → reject signal)
 *     │
 *     ▼
 *  3. Format detection
 *     ├── JPEG → ELA (brightness anomalies)
 *     └── PNG  → Noise analysis (inconsistent grain)
 *     │
 *     ▼
 *  4. Score aggregation ──► [Final Fraud Score]
 * ```
 *
 * Stages run in order; a terminal metadata hit or a hard template reject short-circuits
 * the pipeline (subsequent stages are skipped).
 */
@Singleton
class FraudDetectionPipeline @Inject constructor(
    private val metadataAnalyzer: MetadataAnalyzer,
    private val templateMatcher: TemplateMatcher,
    private val elaAnalyzer: ElaAnalyzer,
    private val noiseAnalyzer: NoiseAnalyzer,
    private val templateStore: DocumentTemplateStore,
    private val scoreCalculator: FraudScoreCalculator,
) {

    fun run(context: Context, input: DocumentInput): FraudResult {
        val format = ImageUtils.detectFormat(context, input.uri)
        val bitmap = ImageUtils.loadBoundedBitmap(context, input.uri)
            ?: throw IllegalStateException("Unable to decode image")

        val checks = mutableListOf<CheckResult>()

        // 1) Metadata -----------------------------------------------------
        val metadataFindings = metadataAnalyzer.analyze(context, input.uri)
        val (metadataScore, metadataDetails) = metadataAnalyzer.buildResult(metadataFindings)
        val metadataTerminal = metadataFindings.any { it.isFlagged }
        checks.add(
            CheckResult(
                stage = DetectionStage.METADATA,
                score = metadataScore,
                isTerminal = metadataTerminal,
                details = metadataDetails,
            )
        )
        if (metadataTerminal) {
            return finalize(checks, format)
        }

        // 2) Template / layout matching -----------------------------------
        val templates = templateStore.templatesFor(input.type)
        val templateOutcome = templateMatcher.match(bitmap, templates)
        checks.add(
            CheckResult(
                stage = DetectionStage.TEMPLATE,
                score = templateOutcome.score,
                isTerminal = false, // template can still inform the score without rejecting outright
                details = templateOutcome.details,
            )
        )

        // 3) Format-aware integrity analysis ------------------------------
        val integrity = when (format) {
            ImageFormat.JPEG -> {
                val ela = elaAnalyzer.analyze(bitmap)
                CheckResult(
                    stage = DetectionStage.INTEGRITY,
                    score = ela.score,
                    details = buildList {
                        add("Method: ELA (JPEG re-encode at quality ${com.authlens.app.core.Constants.ELA_JPEG_QUALITY})")
                        addAll(ela.details)
                    },
                    heatmapBytes = ela.heatmapBytes,
                )
            }
            ImageFormat.PNG -> {
                val noise = noiseAnalyzer.analyze(bitmap)
                CheckResult(
                    stage = DetectionStage.INTEGRITY,
                    score = noise.score,
                    details = buildList {
                        add("Method: Noise/grain consistency (lossless PNG)")
                        addAll(noise.details)
                    },
                    heatmapBytes = noise.heatmapBytes,
                )
            }
            ImageFormat.OTHER -> {
                // Unknown/lossless formats fall back to ELA (most generic tampering signal).
                val ela = elaAnalyzer.analyze(bitmap)
                CheckResult(
                    stage = DetectionStage.INTEGRITY,
                    score = ela.score,
                    details = buildList {
                        add("Method: ELA (format defaulted to JPEG-style analysis)")
                        addAll(ela.details)
                    },
                    heatmapBytes = ela.heatmapBytes,
                )
            }
        }
        checks.add(integrity)

        return finalize(checks, format)
    }

    private fun finalize(checks: List<CheckResult>, format: ImageFormat): FraudResult {
        val score = scoreCalculator.calculate(checks)
        return FraudResult(
            score = score,
            detectedFormat = format,
            checks = checks,
            analyzedAt = System.currentTimeMillis(),
        )
    }
}
