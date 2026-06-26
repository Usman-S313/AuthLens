package com.authlens.app.detection.metadata

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.authlens.app.domain.model.DetectionStage
import com.authlens.app.domain.model.MetadataFinding
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage 1 of the pipeline: inspects EXIF/XMP metadata for evidence the image was
 * produced or edited by known manipulation software.
 *
 * A hit on any editor signature is treated as a **terminal, high-risk** signal —
 * an authentic scanned/captured document should never advertise Photoshop, GIMP, etc.
 */
@Singleton
class MetadataAnalyzer @Inject constructor() {

    /**
     * Known editing/manipulation software signatures, checked case-insensitively against
     * the relevant EXIF tags.
     */
    private val editorSignatures = listOf(
        "photoshop",
        "adobe photoshop",
        "lightroom",
        "gimp",
        "affinity photo",
        "affinity designer",
        "snpseed",
        "snapseed",
        "picsart",
        "canva",
        "pixelmator",
        "coreldraw",
        "corel draw",
        "paint.net",
        "photodir",
        "pixlr",
        "skylum",
        "luminar",
        "capture one",
        "dxo",
        "instagram",
    )

    /**
     * @return list of findings (empty list if metadata could not be read).
     *         Any [MetadataFinding.isFlagged] == true means the document is suspect.
     */
    fun analyze(context: Context, uri: Uri): List<MetadataFinding> {
        val findings = mutableListOf<MetadataFinding>()

        val exif = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        }.getOrNull() ?: return emptyList()

        val tagsToInspect = listOf(
            ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_USER_COMMENT,
            "HostComputer",
            "ProcessingSoftware",
        )

        tagsToInspect.forEach { tag ->
            val value = exif.getAttribute(tag)?.takeIf { it.isNotBlank() } ?: return@forEach

            val match = editorSignatures.firstOrNull { sig ->
                value.contains(sig, ignoreCase = true)
            }

            if (match != null) {
                findings.add(
                    MetadataFinding(
                        tag = humanTag(tag),
                        value = value,
                        isFlagged = true,
                        severity = MAX_SEVERITY,
                        explanation = "Image metadata references editing software " +
                            "“${value.trim()}” — authentic scanned documents do not advertise this.",
                    )
                )
            } else {
                // Non-flagged metadata is recorded for transparency.
                findings.add(
                    MetadataFinding(
                        tag = humanTag(tag),
                        value = value,
                        isFlagged = false,
                        severity = 0,
                        explanation = "Metadata present, no editing software signature.",
                    )
                )
            }
        }

        // XMP block often stores creator tool separately — scan the raw attribute set.
        val xmpTool = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
            ?: exif.getAttribute("Xmp.xmp.CreatorTool")
            ?: exif.getAttribute("Xmp.xmpMM.HistorySoftwareAgent")
        if (xmpTool != null) {
            val match = editorSignatures.firstOrNull { xmpTool.contains(it, ignoreCase = true) }
            if (match != null) {
                findings.add(
                    MetadataFinding(
                        tag = "XMP Creator Tool",
                        value = xmpTool,
                        isFlagged = true,
                        severity = MAX_SEVERITY,
                        explanation = "XMP metadata records creator tool “${xmpTool.trim()}”.",
                    )
                )
            }
        }

        return findings
    }

    /**
     * Aggregates findings into a localized 0..100 score and a [CheckResult] details list.
     * A flagged finding is terminal.
     */
    fun buildResult(findings: List<MetadataFinding>): Pair<Int, List<String>> {
        if (findings.isEmpty()) {
            return 5 to listOf("No readable metadata — cannot rule out editing (mild caution).")
        }
        val flagged = findings.filter { it.isFlagged }
        val score = if (flagged.isNotEmpty()) MAX_SEVERITY else 0
        val details = findings.map { f ->
            if (f.isFlagged) "⚑ ${f.explanation}"
            else "• ${f.tag}: ${f.value}"
        }
        return score to details
    }

    private fun humanTag(tag: String): String = when (tag) {
        ExifInterface.TAG_SOFTWARE -> "Software"
        ExifInterface.TAG_MAKE -> "Make"
        ExifInterface.TAG_MODEL -> "Model"
        ExifInterface.TAG_IMAGE_DESCRIPTION -> "Description"
        ExifInterface.TAG_USER_COMMENT -> "Comment"
        "HostComputer" -> "Host Computer"
        "ProcessingSoftware" -> "Processing Software"
        else -> tag
    }

    companion object {
        val STAGE = DetectionStage.METADATA
        private const val MAX_SEVERITY = 100
    }
}
