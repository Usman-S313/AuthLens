package com.authlens.app.domain.model

/**
 * A single piece of evidence uncovered during the metadata check.
 *
 * For example: "Editing software detected: Adobe Photoshop" → severity HIGH.
 */
data class MetadataFinding(
    /** Human-readable tag name, e.g. "Software", "Make", "XMP Creator". */
    val tag: String,
    /** Raw value found in the tag (may be null if the tag was simply absent/expected). */
    val value: String?,
    /** True when the value matches a known tampering/editing signature. */
    val isFlagged: Boolean,
    /** How serious this finding is, 0..100 contribution toward the metadata score. */
    val severity: Int,
    /** User-facing explanation of why this matters. */
    val explanation: String,
)
