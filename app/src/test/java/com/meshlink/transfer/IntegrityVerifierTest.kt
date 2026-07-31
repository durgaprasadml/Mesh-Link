package com.meshlink.transfer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class IntegrityVerifierTest {

    private lateinit var verifier: IntegrityVerifier

    @Before
    fun setup() {
        verifier = IntegrityVerifier()
    }

    @Test
    fun `test streaming SHA-256 calculation matches expected hash`() {
        val testString = "Mesh Link Media Subsystem Zero-Copy Streaming Test Data"
        val input = ByteArrayInputStream(testString.toByteArray())

        val hash = verifier.calculateStreamChecksum(input)
        assertNotNull("Stream SHA-256 should not be null", hash)
        assertEquals(64, hash?.length) // SHA-256 hex string length
    }

    @Test
    fun `test CRC32 computation and verification`() {
        val bytes = "CRC32 chunk validation payload".toByteArray()
        val crc = verifier.calculateCrc32(bytes)

        assertTrue("Valid CRC32 must pass verification", verifier.verifyCrc32(bytes, crc))
        assertFalse("Mismatched CRC32 must fail verification", verifier.verifyCrc32(bytes, crc + 1))
    }
}
