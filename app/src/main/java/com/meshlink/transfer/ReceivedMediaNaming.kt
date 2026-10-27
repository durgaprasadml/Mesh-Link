package com.meshlink.transfer

import java.io.File
import java.io.IOException

/**
 * Builds receiver-local file names for incoming media.
 *
 * File names and transfer ids arrive from remote peers (BLE META packets and the Wi-Fi Direct
 * stream handshake) and MUST be treated as untrusted. Using them directly as path components
 * allowed a peer to write outside the media directory (e.g. `../databases/...`) and let two
 * concurrent transfers that shared a file name overwrite each other.
 */
object ReceivedMediaNaming {

    private const val MAX_COMPONENT_LENGTH = 96
    private const val MAX_TRANSFER_ID_LENGTH = 128
    private const val NAME_SEPARATOR = "__"

    /**
     * Reduces an untrusted value to a single safe path component:
     * no directory separators, no leading dots, only `[A-Za-z0-9._-]`, bounded length.
     * The file extension (if any) survives truncation so image/audio decoders keep working.
     */
    fun sanitizeComponent(raw: String?, fallback: String, maxLength: Int = MAX_COMPONENT_LENGTH): String {
        val lastSegment = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\')
        val cleaned = buildString(lastSegment.length) {
            for (ch in lastSegment) {
                append(if (ch.isLetterOrDigit() && ch.code < 128 || ch == '.' || ch == '_' || ch == '-') ch else '_')
            }
        }.trimStart('.')

        if (cleaned.isBlank() || cleaned.all { it == '_' || it == '.' }) return fallback
        if (cleaned.length <= maxLength) return cleaned

        val dot = cleaned.lastIndexOf('.')
        val extension = if (dot > 0 && cleaned.length - dot <= 8) cleaned.substring(dot) else ""
        return cleaned.take(maxLength - extension.length) + extension
    }

    /** Safe form of a remote transfer id, suitable for embedding in a file name. */
    fun safeTransferId(transferId: String?): String =
        sanitizeComponent(transferId, fallback = "transfer", maxLength = MAX_TRANSFER_ID_LENGTH)

    /**
     * Final receiver-local file name. The transfer id prefix makes the name unique per transfer,
     * so concurrent transfers (e.g. the SOS Front and Rear images) can never overwrite each other
     * even if the sender chose identical file names.
     */
    fun receivedFileName(transferId: String?, fileName: String?): String {
        val safeId = safeTransferId(transferId)
        val safeName = sanitizeComponent(fileName, fallback = "media.bin")
        return "$safeId$NAME_SEPARATOR$safeName"
    }

    /** Resolves [name] inside [directory] and refuses anything that would escape it. */
    @Throws(IOException::class)
    fun resolveInside(directory: File, name: String): File {
        val candidate = File(directory, name)
        val dirPath = directory.canonicalPath
        val candidatePath = candidate.canonicalPath
        if (candidatePath != dirPath && !candidatePath.startsWith(dirPath + File.separator)) {
            throw IOException("Refusing to resolve '$name' outside ${directory.name}")
        }
        return candidate
    }
}
