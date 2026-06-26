package com.authlens.app.domain.model

/**
 * The aggregated fraud score, built by [com.authlens.app.detection.scoring.FraudScoreCalculator].
 *
 * [score] is the headline 0..100 number. [level] is the bucket the UI surfaces.
 */
data class FraudScore(
    /** Aggregated fraud score 0..100. */
    val score: Int,
    /** Risk bucket derived from [score]. */
    val level: RiskLevel,
    /** Per-stage contributions, ordered by [DetectionStage.order]. */
    val perStageScores: Map<DetectionStage, Int>,
    /** True when detection was cut short by a terminal signal (metadata hit / template reject). */
    val wasTerminatedEarly: Boolean,
)
