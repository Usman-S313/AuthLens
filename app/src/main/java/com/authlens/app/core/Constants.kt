package com.authlens.app.core

/**
 * App-wide tunable constants.
 *
 * Thresholds were chosen as sensible defaults; tweak here to tune detection sensitivity
 * without touching the analyzer implementations.
 */
object Constants {

    /** ELA re-encode quality. 95 is the standard for Error Level Analysis. */
    const val ELA_JPEG_QUALITY = 95

    /** Max image dimension (px) before analysis. Keeps memory + runtime bounded. */
    const val MAX_IMAGE_DIMENSION = 1600

    /** Grid size (NxN cells) used by the noise analyzer to measure grain uniformity. */
    const val NOISE_GRID_SIZE = 8

    /** ORB features cap used by template matching. */
    const val TEMPLATE_MAX_FEATURES = 1000

    /** Minimum good-match count for a layout to be considered authentic. */
    const val TEMPLATE_MIN_GOOD_MATCHES = 25

    object Scoring {
        // Weights — must sum to 1.0
        const val WEIGHT_METADATA = 0.35
        const val WEIGHT_TEMPLATE = 0.30
        const val WEIGHT_FORMAT_ANALYSIS = 0.35

        // Risk bucket boundaries (fraud score 0..100)
        const val SUSPICIOUS_THRESHOLD = 30
        const val LIKELY_FRAUD_THRESHOLD = 55
        const val HIGH_RISK_THRESHOLD = 80
    }
}
