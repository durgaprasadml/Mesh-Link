package com.meshlink.transfer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class IntegrityVerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var verifier: IntegrityVerifier

    @Before
    fun setup() {
        verifier = IntegrityVerifier()
    }

    @Test
    fun `test file SHA-256 calculation and verification`() {
        val testFile = tempFolder.newFile("test_payload.txt")
        testFile.writeText("Mesh Link Media Subsystem Zero-Copy Streaming Test Data")

        val hash = verifier.calculateFileChecksum(testFile)
        assertNotNull("File SHA-256 should not be null", hash)
        assertEquals(64, hash?.length)

        assertTrue("Valid checksum must pass verification", verifier.verifyFileChecksum(testFile, hash))
        assertFalse("Mismatched checksum must fail verification", verifier.verifyFileChecksum(testFile, "invalid_hash_12345"))
    }
}
