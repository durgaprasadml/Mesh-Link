package com.meshlink.trust

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityFingerprintTest {

    @Test
    fun testComputeAndFormatHex() {
        val testInput = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1234567890abcdef"
        val fp = IdentityFingerprint.compute(testInput)
        
        assertTrue(fp.isNotEmpty())
        // Verify 4-character block grouping format
        assertTrue(fp.contains(" "))
        val parts = fp.split(" ")
        assertTrue(parts.all { it.length == 4 })
    }

    @Test
    fun testFingerprintMatching() {
        val fp1 = "AB34 CD91 8F20 DAA1"
        val fp2 = "ab34cd918f20daa1"
        val fp3 = "AB34 CD91 8F20 9999"

        assertTrue(IdentityFingerprint.matches(fp1, fp2))
        assertFalse(IdentityFingerprint.matches(fp1, fp3))
    }
}
