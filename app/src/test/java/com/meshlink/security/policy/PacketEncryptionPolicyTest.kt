package com.meshlink.security.policy

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PacketEncryptionPolicyTest {

    @Test
    fun `test all REQUIRED packet types`() {
        val requiredTypes = listOf(
            PacketType.TEXT,
            PacketType.LOCATION,
            PacketType.MEDIA_META,
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_ACK,
            PacketType.MEDIA_NACK,
            PacketType.VOICE_SIGNAL,
            PacketType.VOICE_FRAME,
            PacketType.VIDEO_SIGNAL,
            PacketType.VIDEO_FRAME,
            PacketType.WIFI_NEGOTIATION,
            PacketType.DELIVERY_ACK,
            PacketType.READ_RECEIPT,
            PacketType.INCIDENT_REPORT,
            PacketType.CHECK_IN,
            PacketType.FORM_SYNC,
            PacketType.RESOURCE_SYNC,
            PacketType.MAP_SYNC
        )

        for (type in requiredTypes) {
            assertEquals(
                "Expected REQUIRED for $type",
                EncryptionRequirement.REQUIRED,
                PacketEncryptionPolicy.getRequirement(type)
            )

            // Encrypted variants are always valid
            assertTrue(
                "Encrypted $type should be valid",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = true),
                    strictMode = true,
                    hasSecureSession = true
                )
            )

            // Plaintext variants are always rejected
            assertFalse(
                "Plaintext $type should be rejected even without strict mode",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = false),
                    strictMode = false,
                    hasSecureSession = false
                )
            )
        }
    }

    @Test
    fun `test all OPTIONAL packet types`() {
        val optionalTypes = listOf(
            PacketType.SESSION_REKEY
        )

        for (type in optionalTypes) {
            assertEquals(
                "Expected OPTIONAL for $type",
                EncryptionRequirement.OPTIONAL,
                PacketEncryptionPolicy.getRequirement(type)
            )

            // Encrypted variants are always valid
            assertTrue(
                "Encrypted $type should be valid",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = true),
                    strictMode = true,
                    hasSecureSession = true
                )
            )

            // Plaintext variants in Strict Mode should be rejected
            assertFalse(
                "Plaintext $type in strict mode should be rejected",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = false),
                    strictMode = true,
                    hasSecureSession = false
                )
            )

            // Plaintext variants with an active secure session should be rejected
            assertFalse(
                "Plaintext $type with secure session active should be rejected",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = false),
                    strictMode = false,
                    hasSecureSession = true
                )
            )

            // Plaintext variants without strict mode and without secure session should be allowed (e.g. bootstrap/recovery)
            assertTrue(
                "Plaintext $type allowed without strict mode and no session",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = false),
                    strictMode = false,
                    hasSecureSession = false
                )
            )
        }
    }

    @Test
    fun `test all BOOTSTRAP_ONLY packet types`() {
        val bootstrapTypes = listOf(
            PacketType.KEY_EXCHANGE,
            PacketType.SOS,
            PacketType.BEACON
        )

        for (type in bootstrapTypes) {
            assertEquals(
                "Expected BOOTSTRAP_ONLY for $type",
                EncryptionRequirement.BOOTSTRAP_ONLY,
                PacketEncryptionPolicy.getRequirement(type)
            )

            // Encrypted variants are valid
            assertTrue(
                "Encrypted $type should be valid",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = true),
                    strictMode = true,
                    hasSecureSession = true
                )
            )

            // Plaintext variants are valid even in strict mode
            assertTrue(
                "Plaintext $type should be valid in strict mode",
                PacketEncryptionPolicy.validatePacketEncryption(
                    createPacket(type, encrypted = false),
                    strictMode = true,
                    hasSecureSession = true
                )
            )
        }
    }

    private fun createPacket(type: PacketType, encrypted: Boolean): MeshPacket {
        return MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = "sender123",
            targetId = "target123",
            payload = "test payload",
            type = type,
            encrypted = encrypted
        )
    }
}
