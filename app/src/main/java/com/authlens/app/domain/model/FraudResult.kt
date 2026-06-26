package com.authlens.app.domain.model

/**
 * The complete outcome of running the fraud-detection pipeline on one document image.
 *
 * This is the single object the presentation layer consumes to render the result screen.
 */
data class FraudResult(
    val score: FraudScore,
    val detectedFormat: ImageFormat,
    /** All stage results, in pipeline order. */
    val checks: List<CheckResult>,
    /** Timestamp (epoch millis) of when analysis completed. */
    val analyzedAt: Long,
)
