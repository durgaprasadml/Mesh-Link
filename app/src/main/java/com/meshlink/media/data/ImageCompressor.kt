package com.meshlink.media.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.meshlink.common.logger.MeshLogger
import java.io.ByteArrayOutputStream

object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val MAX_DIMENSION = 480       // Requirement: max 480px
    private const val MAX_SIZE_BYTES = 80_000   // Target: 40KB - 80KB
    private const val ABSOLUTE_MAX_BYTES = 100_000 // Absolute max 100KB

    /**
     * Compress an image URI to a JPEG byte array. Target ≤ 80KB. Max 100KB.
     * Strategy:
     *   1. Decode with inSampleSize to avoid loading full res image.
     *   2. Scale to MAX_DIMENSION (480px) using createScaledBitmap.
     *   3. Compress using JPEG quality 20%. If > 80KB, retry with 15%.
     *   4. If still > 100KB, resize by 0.8x and repeat.
     */
    fun compress(context: Context, uri: Uri): ByteArray? {
        return try {
            // Step 1: Decode bounds only (no memory allocation)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                MeshLogger.e(TAG, "Cannot decode image bounds from URI")
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
                MeshLogger.e(TAG, "Failed to decode bitmap from URI")
                return null
            }

            // Step 3: Scale to MAX_DIMENSION
            bitmap = scaleBitmapToMax(bitmap, MAX_DIMENSION)

            // Step 4: Iterative quality compression with dimension fallback
            var result: ByteArray? = null
            var pass = 0

            while (pass < 3) {
                result = compressToTarget(bitmap)
                if (result != null) break

                // Still too large (> 100KB) — resize by 0.8x and retry
                val newW = (bitmap.width * 0.8f).toInt().coerceAtLeast(100)
                val newH = (bitmap.height * 0.8f).toInt().coerceAtLeast(100)
                val smaller = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                if (smaller !== bitmap) bitmap.recycle()
                bitmap = smaller
                pass++
                MeshLogger.d(TAG, "Pass $pass: rescaled to ${bitmap.width}×${bitmap.height}, retrying compression")
            }

            bitmap.recycle()

            if (result == null) {
                MeshLogger.e(TAG, "Could not compress image to ≤${ABSOLUTE_MAX_BYTES / 1000}KB after $pass passes")
            } else {
                MeshLogger.d(TAG, "Compressed to ${result.size / 1000}KB in $pass pass(es)")
            }

            result
        } catch (e: Exception) {
            MeshLogger.e(TAG, "compress() failed: ${e.message}", e)
            null
        }
    }

    /**
     * Compress bitmap as JPEG at quality 20. If > 80KB, compress at quality 15.
     * Returns the byte array if size ≤ ABSOLUTE_MAX_BYTES, else null.
     */
    private fun compressToTarget(bitmap: Bitmap): ByteArray? {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos)
        var bytes = baos.toByteArray()
        
        if (bytes.size > MAX_SIZE_BYTES) {
            baos.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 15, baos)
            bytes = baos.toByteArray()
        }
        baos.close()

        return if (bytes.size <= ABSOLUTE_MAX_BYTES) bytes else null
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

    /**
     * Generates a 120px thumbnail encoded as a Base64 JPEG string.
     */
    fun generateThumbnailBase64(context: Context, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, 120, 120)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val scaled = scaleBitmapToMax(bitmap, 120)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            
            if (scaled !== bitmap) {
                scaled.recycle()
            }
            bitmap.recycle()

            android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "generateThumbnailBase64() failed: ${e.message}", e)
            null
        }
    }
}
