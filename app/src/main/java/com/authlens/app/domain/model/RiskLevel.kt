package com.authlens.app.domain.model

/**
 * Final risk classification derived from the aggregated fraud score.
 *
 * @param scoreUpperBound inclusive upper bound of this bucket's score range.
 */
enum class RiskLevel(
    val label: String,
    val description: String,
    val scoreUpperBound: Int,
) {
    CLEAN(
        label = "Authentic",
        description = "No signs of tampering detected.",
        scoreUpperBound = 29,
    ),
    SUSPICIOUS(
        label = "Suspicious",
        description = "Minor anomalies found. Recommend manual review.",
        scoreUpperBound = 54,
    ),
    LIKELY_FRAUD(
        label = "Likely Fraud",
        description = "Strong indicators of tampering detected.",
        scoreUpperBound = 79,
    ),
    HIGH_RISK(
        label = "High Risk Fraud",
        description = "Severe tampering or editing software detected. Reject.",
        scoreUpperBound = 100,
    );

    companion object {
        /** Maps a 0..100 fraud score to its risk bucket. */
        fun fromScore(score: Int): RiskLevel {
            val clamped = score.coerceIn(0, 100)
            return entries.first { clamped <= it.scoreUpperBound }
        }
    }
}
