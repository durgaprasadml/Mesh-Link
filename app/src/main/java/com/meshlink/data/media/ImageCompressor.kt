package com.meshlink.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val MAX_DIMENSION = 800       // Requirement: max 800px
    private const val MAX_SIZE_BYTES = 200_000  // Requirement: ≤200KB
    private const val MIN_QUALITY = 15

    /**
     * Compress an image URI to a JPEG byte array ≤200KB, max 800×800px.
     * Strategy:
     *   1. Decode with inSampleSize to avoid loading multi-megapixel images into memory.
     *   2. Scale to MAX_DIMENSION using createScaledBitmap.
     *   3. Iteratively reduce JPEG quality from 45 → MIN_QUALITY until ≤200KB.
     *   4. If still too large at minimum quality, halve dimensions and repeat (max 3 passes).
     * Returns null if compression fails or image cannot be decoded.
     */
    fun compress(context: Context, uri: Uri): ByteArray? {
        return try {
            // Step 1: Decode bounds only (no memory allocation)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.e(TAG, "Cannot decode image bounds from URI")
                return null
            }

            // Step 2: Decode with inSampleSize to limit initial memory usage
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, MAX_DIMENSION, MAX_DIMENSION)
                inPreferredConfig = Bitmap.Config.RGB_565 // 2 bytes/pixel vs 4 for ARGB_8888
            }
            var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: run {
                Log.e(TAG, "Failed to decode bitmap from URI")
                return null
            }

            // Step 3: Scale to MAX_DIMENSION
            bitmap = scaleBitmapToMax(bitmap, MAX_DIMENSION)

            // Step 4: Iterative quality compression with dimension fallback
            var result: ByteArray? = null
            var pass = 0

            while (pass < 3) {
                result = compressToTarget(bitmap, MAX_SIZE_BYTES)
                if (result != null) break

                // Still too large — halve dimensions and retry
                val newW = (bitmap.width * 0.7f).toInt().coerceAtLeast(100)
                val newH = (bitmap.height * 0.7f).toInt().coerceAtLeast(100)
                val smaller = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                if (smaller !== bitmap) bitmap.recycle()
                bitmap = smaller
                pass++
                Log.d(TAG, "Pass $pass: rescaled to ${bitmap.width}×${bitmap.height}, retrying compression")
            }

            bitmap.recycle()

            if (result == null) {
                Log.e(TAG, "Could not compress image to ≤${MAX_SIZE_BYTES / 1000}KB after $pass passes")
            } else {
                Log.d(TAG, "Compressed to ${result.size / 1000}KB in $pass pass(es)")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "compress() failed: ${e.message}", e)
            null
        }
    }

    /**
     * Iteratively compress bitmap as JPEG, starting at quality 45, stepping down by 5
     * until size ≤ maxSizeBytes or MIN_QUALITY is reached.
     * Returns the byte array if target met, null if impossible at MIN_QUALITY.
     */
    private fun compressToTarget(bitmap: Bitmap, maxSizeBytes: Int): ByteArray? {
        var quality = 45
        var bytes: ByteArray
        val baos = ByteArrayOutputStream()
        do {
            baos.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            bytes = baos.toByteArray()
            quality -= 5
        } while (bytes.size > maxSizeBytes && quality >= MIN_QUALITY)
        baos.close()

        return if (bytes.size <= maxSizeBytes) bytes else null
    }

    private fun scaleBitmapToMax(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap

        val ratio = minOf(maxDimension.toFloat() / w, maxDimension.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
