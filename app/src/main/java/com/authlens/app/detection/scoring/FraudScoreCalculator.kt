package com.authlens.app.detection.scoring

import com.authlens.app.core.Constants
import com.authlens.app.domain.model.CheckResult
import com.authlens.app.domain.model.DetectionStage
import com.authlens.app.domain.model.FraudScore
import com.authlens.app.domain.model.RiskLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage 4: aggregates per-stage fraud contributions into the final headline score.
 *
 * Weights are defined in [Constants.Scoring]. Terminal signals (a metadata editor hit)
 * force a HIGH_RISK result regardless of other stages, since a document advertising
 * Photoshop is never authentic.
 */
@Singleton
class FraudScoreCalculator @Inject constructor() {

    fun calculate(checks: List<CheckResult>): FraudScore {
        require(checks.isNotEmpty()) { "Cannot score an empty check list" }

        val byStage: Map<DetectionStage, Int> = checks.associate { it.stage to it.score }

        // Short-circuit: a terminal stage (metadata editor hit) overrides everything.
        val terminal = checks.firstOrNull { it.isTerminal }
        if (terminal != null) {
            val score = terminal.score.coerceAtLeast(Constants.Scoring.HIGH_RISK_THRESHOLD)
            return FraudScore(
                score = score.coerceIn(0, 100),
                level = RiskLevel.HIGH_RISK,
                perStageScores = byStage,
                wasTerminatedEarly = true,
            )
        }

        val metadataScore = byStage[DetectionStage.METADATA] ?: 0
        val templateScore = byStage[DetectionStage.TEMPLATE] ?: 0
        val integrityScore = byStage[DetectionStage.INTEGRITY] ?: 0

        val weighted = (metadataScore * Constants.Scoring.WEIGHT_METADATA +
            templateScore * Constants.Scoring.WEIGHT_TEMPLATE +
            integrityScore * Constants.Scoring.WEIGHT_FORMAT_ANALYSIS)

        val score = weighted.toInt().coerceIn(0, 100)
        return FraudScore(
            score = score,
            level = RiskLevel.fromScore(score),
            perStageScores = byStage,
            wasTerminatedEarly = false,
        )
    }
}
