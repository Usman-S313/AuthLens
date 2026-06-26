package com.authlens.app.domain.model

/**
 * The detected image format, which decides which integrity analyzer runs
 * (ELA for JPEG, noise for PNG). WebP / others fall back to ELA.
 */
enum class ImageFormat {
    JPEG,
    PNG,
    OTHER;
}

/**
 * Stage identifiers matching the detection pipeline flow.
 *
 * The numeric [order] lets the UI render findings in pipeline order.
 */
enum class DetectionStage(val order: Int, val title: String) {
    METADATA(order = 0, title = "Metadata Check"),
    TEMPLATE(order = 1, title = "Layout / Template Matching"),
    INTEGRITY(order = 2, title = "Pixel Integrity Analysis");

    companion object {
        val ordered: List<DetectionStage> get() = entries.sortedBy { it.order }
    }
}
