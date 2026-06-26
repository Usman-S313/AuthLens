package com.authlens.app.domain.model

/**
 * The outcome of a single detection stage (metadata, template, or integrity).
 *
 * Each stage produces:
 *  - a localized fraud contribution [score] (0..100)
 *  - a list of human-readable [details] explaining the score
 *  - an optional [heatmapBytes] PNG preview (only the integrity stage produces one)
 */
data class CheckResult(
    val stage: DetectionStage,
    /** 0..100 localized score for this stage. 0 = clean, 100 = severe. */
    val score: Int,
    /** True when the stage short-circuits the pipeline (e.g. metadata software hit). */
    val isTerminal: Boolean = false,
    /** Free-form bullet points explaining what was found. */
    val details: List<String> = emptyList(),
    /** Optional PNG byte array rendered as an anomaly heatmap (ELA/noise stages). */
    val heatmapBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CheckResult) return false
        return stage == other.stage &&
            score == other.score &&
            isTerminal == other.isTerminal &&
            details == other.details
        // heatmapBytes intentionally excluded from equality
    }

    override fun hashCode(): Int {
        var result = stage.hashCode()
        result = 31 * result + score
        result = 31 * result + isTerminal.hashCode()
        result = 31 * result + details.hashCode()
        return result
    }
}
