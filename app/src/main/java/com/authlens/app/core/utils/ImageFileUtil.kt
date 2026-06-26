package com.authlens.app.core.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Small helpers for staging captured/processed images in the app cache and exposing
 * them via a [FileProvider] content URI.
 */
object ImageFileUtil {

    private const val DIR = "images"

    /**
     * Creates a new empty JPEG file under the app cache and returns a content:// URI
     * suitable for CameraX / ACTION_IMAGE_CAPTURE output.
     */
    fun newCacheUri(context: Context, name: String = "capture_${System.currentTimeMillis()}"): Uri {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val file = File(dir, "$name.jpg")
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /** Returns the cache images directory (creating it if needed). */
    fun cacheDir(context: Context): File =
        File(context.cacheDir, DIR).apply { mkdirs() }
}
