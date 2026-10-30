package com.meshlink.ble.data

import com.meshlink.util.MeshIdNormalizer
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * Compact Stage A SOS Announcement for direct connectionless BLE advertising.
 *
 * Payload format (24 bytes total):
 * - Bytes 0..7   (8 bytes): Canonical sender Mesh ID (ASCII, padded/truncated to 8 chars)
 * - Byte 8       (1 byte) : Capabilities flag (with bit 7 0x80 = SOS_ACTIVE)
 * - Byte 9       (1 byte) : Protocol version (0x01)
 * - Bytes 10..17 (8 bytes): Unique SOS identifier (derived from SOS UUID)
 * - Bytes 18..21 (4 bytes): Unix epoch timestamp in seconds (big-endian 32-bit unsigned)
 * - Bytes 22..23 (2 bytes): Integrity tag (first 2 bytes of SHA-256 over bytes 0..21)
 */
data class SosAnnouncement(
    val senderMeshId: String,
    val capabilities: Byte,
    val version: Byte,
    val sosId: ByteArray,
    val timestampSeconds: Long,
    val integrityTag: Short
) {
    val sosIdHex: String
        get() = sosId.joinToString("") { "%02x".format(it) }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(ANNOUNCEMENT_PAYLOAD_SIZE)
        val canonicalSender = MeshIdNormalizer.canonicalize(senderMeshId).padEnd(CANONICAL_ID_LENGTH, ' ').take(CANONICAL_ID_LENGTH)
        val senderBytes = canonicalSender.toByteArray(Charsets.UTF_8)
        buffer.put(senderBytes, 0, minOf(senderBytes.size, CANONICAL_ID_LENGTH))

        // Ensure bit 7 is set to indicate SOS announcement
        val capWithSos = (capabilities.toInt() or (CAPABILITY_SOS_ACTIVE.toInt() and 0xFF)).toByte()
        buffer.put(capWithSos)
        buffer.put(version)
        buffer.put(sosId, 0, minOf(sosId.size, SOS_ID_LENGTH))
        buffer.putInt(timestampSeconds.toInt())

        val tag = computeIntegrityTag(buffer.array(), 0, 22)
        buffer.putShort(tag)

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SosAnnouncement

        if (senderMeshId != other.senderMeshId) return false
        if (capabilities != other.capabilities) return false
        if (version != other.version) return false
        if (!sosId.contentEquals(other.sosId)) return false
        if (timestampSeconds != other.timestampSeconds) return false
        if (integrityTag != other.integrityTag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = senderMeshId.hashCode()
        result = 31 * result + capabilities
        result = 31 * result + version
        result = 31 * result + sosId.contentHashCode()
        result = 31 * result + timestampSeconds.hashCode()
        result = 31 * result + integrityTag.toInt()
        return result
    }

    companion object {
        const val ANNOUNCEMENT_PAYLOAD_SIZE = 24
        const val CANONICAL_ID_LENGTH = 8
        const val SOS_ID_LENGTH = 8
        const val CAPABILITY_SOS_ACTIVE: Byte = 0x80.toByte()
        const val PROTOCOL_VERSION: Byte = 0x01

        fun computeIntegrityTag(data: ByteArray, offset: Int = 0, length: Int = 22): Short {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(data, offset, length)
            val hash = digest.digest()
            val b0 = hash[0].toInt() and 0xFF
            val b1 = hash[1].toInt() and 0xFF
            return ((b0 shl 8) or b1).toShort()
        }

        fun deriveSosIdBytes(sosEventId: String): ByteArray {
            return try {
                val uuid = UUID.fromString(sosEventId)
                val buf = ByteBuffer.allocate(8)
                buf.putLong(uuid.mostSignificantBits)
                buf.array()
            } catch (_: Exception) {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(sosEventId.toByteArray(Charsets.UTF_8))
                hash.copyOfRange(0, 8)
            }
        }

        fun deriveSosIdHex(sosEventId: String): String {
            return deriveSosIdBytes(sosEventId).joinToString("") { "%02x".format(it) }
        }

        fun create(
            senderMeshId: String,
            sosEventId: String,
            capabilities: Byte = 0,
            timestampSeconds: Long = System.currentTimeMillis() / 1000L
        ): SosAnnouncement {
            val sosId = deriveSosIdBytes(sosEventId)
            val capWithSos = (capabilities.toInt() or (CAPABILITY_SOS_ACTIVE.toInt() and 0xFF)).toByte()
            val canonicalSender = MeshIdNormalizer.canonicalize(senderMeshId).padEnd(CANONICAL_ID_LENGTH, ' ').take(CANONICAL_ID_LENGTH)

            val buffer = ByteBuffer.allocate(ANNOUNCEMENT_PAYLOAD_SIZE)
            val senderBytes = canonicalSender.toByteArray(Charsets.UTF_8)
            buffer.put(senderBytes, 0, minOf(senderBytes.size, CANONICAL_ID_LENGTH))
            buffer.put(capWithSos)
            buffer.put(PROTOCOL_VERSION)
            buffer.put(sosId, 0, minOf(sosId.size, SOS_ID_LENGTH))
            buffer.putInt(timestampSeconds.toInt())

            val tag = computeIntegrityTag(buffer.array(), 0, 22)

            return SosAnnouncement(
                senderMeshId = canonicalSender.trim(),
                capabilities = capWithSos,
                version = PROTOCOL_VERSION,
                sosId = sosId,
                timestampSeconds = timestampSeconds,
                integrityTag = tag
            )
        }

        fun isSosAnnouncement(data: ByteArray?): Boolean {
            if (data == null || data.size < ANNOUNCEMENT_PAYLOAD_SIZE) return false
            val cap = data[8].toInt() and 0xFF
            val ver = data[9].toInt() and 0xFF
            return (cap and (CAPABILITY_SOS_ACTIVE.toInt() and 0xFF)) != 0 && ver == PROTOCOL_VERSION.toInt()
        }

        fun parse(data: ByteArray): SosAnnouncement? {
            if (!isSosAnnouncement(data)) return null
            if (data.size < ANNOUNCEMENT_PAYLOAD_SIZE) return null

            val actualTag = ((data[22].toInt() and 0xFF shl 8) or (data[23].toInt() and 0xFF)).toShort()
            val expectedTag = computeIntegrityTag(data, 0, 22)
            if (actualTag != expectedTag) {
                return null // Integrity verification failed: tampered or corrupted
            }

            val senderMeshId = String(data, 0, CANONICAL_ID_LENGTH, Charsets.UTF_8).trim()
            val capabilities = data[8]
            val version = data[9]
            val sosId = data.copyOfRange(10, 18)
            val tsInt = ByteBuffer.wrap(data, 18, 4).int
            val tsSeconds = tsInt.toLong() and 0xFFFFFFFFL

            return SosAnnouncement(
                senderMeshId = senderMeshId,
                capabilities = capabilities,
                version = version,
                sosId = sosId,
                timestampSeconds = tsSeconds,
                integrityTag = actualTag
            )
        }
    }
}

