package com.meshlink.transfer

import android.content.Context
import android.net.Uri
import com.meshlink.common.logger.MeshLogger
import com.meshlink.media.data.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class CompressionResult(
    val data: ByteArray,
    val isCompressed: Boolean,
    val compressionType: String,
    val originalSize: Long,
    val compressedSize: Long
) {
    val compressionRatio: Float
        get() = if (originalSize > 0) compressedSize.toFloat() / originalSize.toFloat() else 1.0f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CompressionResult
        return data.contentEquals(other.data) && isCompressed == other.isCompressed && compressionType == other.compressionType
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + isCompressed.hashCode()
        result = 31 * result + compressionType.hashCode()
        return result
    }
}

@Singleton
class CompressionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "CompressionEngine"
        private const val IMAGE_COMPRESS_SIZE_THRESHOLD_BYTES = 500 * 1024L // 500 KB

        private val PRE_COMPRESSED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif",
            "mp4", "mkv", "avi", "mov", "webm",
            "mp3", "m4a", "aac", "ogg", "flac",
            "zip", "gz", "tgz", "rar", "7z", "apk", "pdf"
        )
    }

    /**
     * Determines whether a file extension or MIME type is already compressed.
     */
    fun isAlreadyCompressed(file: File, mimeType: String): Boolean {
        val ext = file.extension.lowercase()
        if (PRE_COMPRESSED_EXTENSIONS.contains(ext)) return true
        if (mimeType.startsWith("image/") || mimeType.startsWith("video/") || mimeType.startsWith("audio/")) {
            return true
        }
        if (mimeType.contains("zip") || mimeType.contains("compressed") || mimeType.contains("pdf")) {
            return true
        }
        return false
    }

    /**
     * Processes file compression intelligently based on format and file size.
     */
    fun compressFileIfNeeded(file: File, mimeType: String): CompressionResult {
        val originalSize = file.length()

        if (!file.exists() || originalSize == 0L) {
            return CompressionResult(ByteArray(0), false, "NONE", originalSize, 0L)
        }

        // Image optimization case
        if (mimeType.startsWith("image/") && originalSize > IMAGE_COMPRESS_SIZE_THRESHOLD_BYTES) {
            try {
                val compressedBytes = ImageCompressor.compress(context, Uri.fromFile(file))
                if (compressedBytes != null && compressedBytes.size < originalSize) {
                    MeshLogger.d(TAG, "Compressed image ${file.name}: ${originalSize / 1024}KB -> ${compressedBytes.size / 1024}KB")
                    return CompressionResult(
                        data = compressedBytes,
                        isCompressed = true,
                        compressionType = "IMAGE_JPEG",
                        originalSize = originalSize,
                        compressedSize = compressedBytes.size.toLong()
                    )
                }
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Image compression failed, using raw file: ${e.message}")
            }
        }

        // Check if pre-compressed (documents, audio/video, zips)
        if (isAlreadyCompressed(file, mimeType)) {
            val bytes = file.readBytes()
            return CompressionResult(
                data = bytes,
                isCompressed = false,
                compressionType = "NONE",
                originalSize = originalSize,
                compressedSize = originalSize
            )
        }

        // GZIP compression for plain text / docs / JSON / CSV
        return try {
            val rawBytes = file.readBytes()
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).use { gzip ->
                gzip.write(rawBytes)
            }
            val gzipBytes = bos.toByteArray()

            // Only use GZIP if it yields at least 15% size reduction
            if (gzipBytes.size < originalSize * 0.85f) {
                MeshLogger.d(TAG, "GZIP compressed ${file.name}: ${originalSize}B -> ${gzipBytes.size}B")
                CompressionResult(
                    data = gzipBytes,
                    isCompressed = true,
                    compressionType = "GZIP",
                    originalSize = originalSize,
                    compressedSize = gzipBytes.size.toLong()
                )
            } else {
                CompressionResult(
                    data = rawBytes,
                    isCompressed = false,
                    compressionType = "NONE",
                    originalSize = originalSize,
                    compressedSize = originalSize
                )
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "GZIP compression failed for ${file.name}: ${e.message}")
            val bytes = file.readBytes()
            CompressionResult(bytes, false, "NONE", originalSize, originalSize)
        }
    }

    /**
     * Decompresses bytes if GZIP compressed.
     */
    fun decompressIfNeeded(data: ByteArray, compressionType: String): ByteArray {
        if (compressionType != "GZIP" || data.isEmpty()) return data
        return try {
            GZIPInputStream(ByteArrayInputStream(data)).use { gzip ->
                gzip.readBytes()
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Decompression failed: ${e.message}")
            data
        }
    }
}
