package com.authlens.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.authlens.app.core.Constants
import com.authlens.app.domain.model.ImageFormat
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

/**
 * Shared image helpers used across the detection analyzers.
 *
 * Keeps the heavy lifting (decode + bound + convert) in one place so analyzers stay
 * focused on their algorithm.
 */
object ImageUtils {

    /**
     * Loads a [Bitmap] from a content [uri], downscaled so the longest side is at most
     * [Constants.MAX_IMAGE_DIMENSION]. Returns null if the image cannot be decoded.
     */
    fun loadBoundedBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (maxDim / sample > Constants.MAX_IMAGE_DIMENSION) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }?.let { if (maxDim / sample > Constants.MAX_IMAGE_DIMENSION) scaleToBound(it) else it }
    }

    /** Scales [src] so its longest side equals [Constants.MAX_IMAGE_DIMENSION]. */
    private fun scaleToBound(src: Bitmap): Bitmap {
        val ratio = Constants.MAX_IMAGE_DIMENSION.toFloat() / maxOf(src.width, src.height)
        return Bitmap.createScaledBitmap(src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true)
    }

    /**
     * Sniffs the image format by reading the first bytes of [uri].
     * JPEG starts with FF D8 FF, PNG with 89 50 4E 47.
     */
    fun detectFormat(context: Context, uri: Uri): ImageFormat {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(8)
                val read = input.read(header)
                if (read >= 3 && (header[0].toInt() and 0xFF) == 0xFF &&
                    (header[1].toInt() and 0xFF) == 0xD8 && (header[2].toInt() and 0xFF) == 0xFF
                ) {
                    ImageFormat.JPEG
                } else if (read >= 4 && (header[0].toInt() and 0xFF) == 0x89 &&
                    (header[1].toInt() and 0xFF) == 0x50 &&
                    (header[2].toInt() and 0xFF) == 0x4E &&
                    (header[3].toInt() and 0xFF) == 0x47
                ) {
                    ImageFormat.PNG
                } else {
                    ImageFormat.OTHER
                }
            } ?: ImageFormat.OTHER
        } catch (_: Exception) {
            ImageFormat.OTHER
        }
    }

    /** Converts an ARGB [Bitmap] to an OpenCV BGR [Mat]. */
    fun bitmapToBgrMat(bitmap: Bitmap): Mat {
        val rgba = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, rgba)
        val bgr = Mat()
        Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR)
        rgba.release()
        return bgr
    }

    /** Converts a single-channel [Mat] (0..255) to an ARGB [Bitmap]. */
    fun grayMatToBitmap(gray: Mat): Bitmap {
        val rgba = Mat()
        Imgproc.applyColorMap(gray, rgba, Imgproc.COLORMAP_JET)
        val bmp = Bitmap.createBitmap(gray.cols(), gray.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, bmp)
        rgba.release()
        return bmp
    }

    /** Re-encodes [bitmap] to JPEG at [quality] and returns the bytes + the decoded bitmap. */
    fun reencodeJpeg(bitmap: Bitmap, quality: Int): Pair<ByteArray, Bitmap> {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val bytes = out.toByteArray()
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return bytes to (decoded ?: bitmap)
    }

    /** Normalizes a 0..1 OpenCV [Mat] to 0..255 (CV_8U). Caller passes a pre-allocated [dst]. */
    fun normalizeTo8U(src: Mat, dst: Mat, maxValue: Double) {
        val safeMax = if (maxValue <= 0.0) 1.0 else maxValue
        src.convertTo(dst, CvType.CV_8U, 255.0 / safeMax)
    }

    /** Applies a median blur of the given kernel [ksize] (must be odd). */
    fun medianBlur(src: Mat, ksize: Int): Mat {
        require(ksize % 2 == 1) { "Kernel size must be odd: $ksize" }
        val dst = Mat()
        Imgproc.medianBlur(src, dst, ksize)
        return dst
    }

    private const val TAG = "ImageUtils"
    @Suppress("unused") private fun log(s: String) {} // reserved for future logging
}
