package com.authlens.app.detection.template

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.authlens.app.domain.model.DocumentType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads reference document templates used by [com.authlens.app.detection.alignment.TemplateMatcher].
 *
 * Templates are stored under `assets/templates/<document_type>/`. Each directory may hold
 * multiple templates (e.g. front/back, multiple issuers). If none are bundled, the matcher
 * falls back to a neutral score so the rest of the pipeline still runs.
 *
 * NOTE: This project ships with placeholder template directories only — drop real
 * reference images into the matching asset folder to enable layout verification.
 */
@Singleton
class DocumentTemplateStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val cache = mutableMapOf<DocumentType, List<Bitmap>>()

    /**
     * Returns reference bitmaps for [type]. Loads lazily and caches for the app lifetime.
     */
    @Synchronized
    fun templatesFor(type: DocumentType): List<Bitmap> {
        if (!DocumentType.templated.contains(type)) return emptyList()
        cache[type]?.let { return it }

        val dir = "templates/${type.name.lowercase()}"
        val files = runCatching {
            context.assets.list(dir)?.filter { it.endsWith(".jpg") || it.endsWith(".png") }
        }.getOrNull() ?: emptyList()

        val bitmaps = files.mapNotNull { name ->
            runCatching {
                context.assets.open("$dir/$name").use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
        }

        cache[type] = bitmaps
        return bitmaps
    }

    /** Clears the in-memory cache (used when reference templates are updated). */
    fun clear() {
        cache.values.flatten().forEach { it.recycle() }
        cache.clear()
    }
}
