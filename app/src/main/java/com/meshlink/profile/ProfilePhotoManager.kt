package com.meshlink.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePhotoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ProfilePhotoManager"
        private const val MAX_DIMENSION_PX = 512
        private const val COMPRESSION_QUALITY = 80
        private const val MAX_FILE_SIZE_BYTES = 100 * 1024 // 100 KB
        private const val PROFILE_PHOTOS_DIR = "profile_photos"
    }

    private val profilePhotosDirectory: File by lazy {
        File(context.filesDir, PROFILE_PHOTOS_DIR).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun getProfilePhotoFile(meshId: String): File {
        val safeMeshId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId).ifBlank { meshId }
        return File(profilePhotosDirectory, "$safeMeshId.webp")
    }

    /**
     * Process an input Uri/File for a profile picture:
     * Resizes longest edge to 512px, compresses to WEBP lossy (80%), ensures size <= 100KB,
     * computes SHA-256 hash, and saves directly to context.filesDir/profile_photos/<meshId>.webp
     * Returns Pair<File, String> where File is local destination file and String is SHA-256 hash.
     */
    suspend fun processAndSavePhoto(meshId: String, sourceUri: Uri): Pair<File, String>? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: run {
                MeshLogger.e(TAG, "Failed to decode bitmap from source URI")
                return@withContext null
            }
            try {
                inputStream.close()
            } catch (_: Exception) {}

            // Correct orientation if available
            val rotatedBitmap = rotateBitmapIfRequired(sourceUri, bitmap)
            val resizedBitmap = scaleBitmapToMaxEdge(rotatedBitmap, MAX_DIMENSION_PX)

            // Compress to WEBP
            var quality = COMPRESSION_QUALITY
            var byteArrayOutputStream = ByteArrayOutputStream()
            var compressedBytes: ByteArray

            do {
                byteArrayOutputStream.reset()
                resizedBitmap.compress(Bitmap.CompressFormat.WEBP, quality, byteArrayOutputStream)
                compressedBytes = byteArrayOutputStream.toByteArray()
                if (compressedBytes.size <= MAX_FILE_SIZE_BYTES || quality <= 30) {
                    break
                }
                quality -= 10
            } while (quality > 20)

            if (resizedBitmap != rotatedBitmap) resizedBitmap.recycle()
            if (rotatedBitmap != bitmap) rotatedBitmap.recycle()
            bitmap.recycle()

            if (compressedBytes.size > MAX_FILE_SIZE_BYTES) {
                MeshLogger.e(TAG, "Compressed profile photo exceeds 100 KB limit (${compressedBytes.size} bytes)")
                return@withContext null
            }

            val destFile = getProfilePhotoFile(meshId)
            destFile.writeBytes(compressedBytes)

            val hash = computeSha256(destFile)
            MeshLogger.d(TAG, "Saved profile photo for $meshId to ${destFile.absolutePath} (size: ${destFile.length()} bytes, hash: $hash)")

            Pair(destFile, hash)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error processing profile photo for $meshId: ${e.message}", e)
            null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Save raw image bytes received from network peer if valid
     */
    suspend fun validateAndSavePeerPhoto(meshId: String, imageFile: File, expectedHash: String? = null): Pair<File, String>? = withContext(Dispatchers.IO) {
        try {
            if (!imageFile.exists() || imageFile.length() == 0L) {
                MeshLogger.w(TAG, "Peer image file is empty or missing")
                return@withContext null
            }

            if (imageFile.length() > MAX_FILE_SIZE_BYTES) {
                MeshLogger.w(TAG, "Rejecting peer photo: size ${imageFile.length()} exceeds 100 KB limit")
                imageFile.delete()
                return@withContext null
            }

            val decodedBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (decodedBitmap == null) {
                MeshLogger.w(TAG, "Rejecting peer photo: Invalid or corrupt image format")
                imageFile.delete()
                return@withContext null
            }
            decodedBitmap.recycle()

            val computedHash = computeSha256(imageFile)
            if (expectedHash != null && expectedHash.isNotBlank() && !expectedHash.equals(computedHash, ignoreCase = true)) {
                MeshLogger.w(TAG, "Rejecting peer photo: Hash mismatch (expected $expectedHash, got $computedHash)")
                imageFile.delete()
                return@withContext null
            }

            val destFile = getProfilePhotoFile(meshId)
            if (imageFile.absolutePath != destFile.absolutePath) {
                imageFile.copyTo(destFile, overwrite = true)
                imageFile.delete()
            }

            Pair(destFile, computedHash)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed validating peer photo for $meshId: ${e.message}")
            null
        }
    }

    fun computeSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun scaleBitmapToMaxEdge(bitmap: Bitmap, maxEdgePx: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxEdgePx && height <= maxEdgePx) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxEdgePx
            newHeight = (maxEdgePx / ratio).toInt()
        } else {
            newHeight = maxEdgePx
            newWidth = (maxEdgePx * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun rotateBitmapIfRequired(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    else -> return bitmap
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }
}
