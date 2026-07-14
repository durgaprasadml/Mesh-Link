package com.meshlink.transfer

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class FileMetadata(
    val fileName: String,
    val mimeType: String,
    val totalBytes: Long,
    val sha256Checksum: String? = null
)

@Singleton
class FileMetadataManager @Inject constructor() {

    fun generateMetaPayload(metadata: FileMetadata): String {
        // Format: "MEDIA:{mimeType}:{sha256}:{totalBytes}:{fileName}"
        val checksum = metadata.sha256Checksum ?: ""
        return "MEDIA:${metadata.mimeType}:$checksum:${metadata.totalBytes}:${metadata.fileName}"
    }

    fun parseMetaPayload(payload: String): FileMetadata? {
        if (!payload.startsWith("MEDIA:")) return null
        val parts = payload.split(":", limit = 5)
        if (parts.size < 2) return null
        
        val mimeType = parts[1]
        val checksum = if (parts.size > 2 && parts[2].isNotBlank()) parts[2] else null
        val totalBytes = if (parts.size > 3) parts[3].toLongOrNull() ?: 0L else 0L
        val fileName = if (parts.size > 4) parts[4] else generateDefaultFileName(mimeType)
        
        return FileMetadata(
            fileName = fileName,
            mimeType = mimeType,
            totalBytes = totalBytes,
            sha256Checksum = checksum
        )
    }

    private fun generateDefaultFileName(mimeType: String): String {
        val extension = when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
            mimeType.contains("png") -> ".png"
            mimeType.contains("webp") -> ".webp"
            mimeType.contains("audio") -> ".m4a"
            mimeType.contains("pdf") -> ".pdf"
            mimeType.contains("zip") -> ".zip"
            mimeType.contains("json") -> ".json"
            mimeType.contains("csv") -> ".csv"
            else -> ".bin"
        }
        return "received_${System.currentTimeMillis()}$extension"
    }
    
    fun getMimeTypeForFile(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "json" -> "application/json"
            "csv" -> "text/csv"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
