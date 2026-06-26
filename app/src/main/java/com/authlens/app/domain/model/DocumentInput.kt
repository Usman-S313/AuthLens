package com.authlens.app.domain.model

import android.net.Uri

/**
 * The user's input to the pipeline: a document image plus its type.
 */
data class DocumentInput(
    val uri: Uri,
    val type: DocumentType = DocumentType.GENERIC,
)
