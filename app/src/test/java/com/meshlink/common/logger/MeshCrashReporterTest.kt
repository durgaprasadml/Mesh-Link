package com.meshlink.common.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshCrashReporterTest {

    @Test
    fun `sanitizeIdentifier rejects email addresses and blank strings`() {
        assertEquals("", MeshCrashReporter.sanitizeIdentifier("durgaprasadmadikeri@gmail.com"))
        assertEquals("", MeshCrashReporter.sanitizeIdentifier("durgaprasad@meshlink.com"))
        assertEquals("", MeshCrashReporter.sanitizeIdentifier("   "))
    }

    @Test
    fun `sanitizeIdentifier preserves anonymous non-PII identifiers`() {
        val anon = "anon-a1b2c3d4e5f6"
        assertEquals(anon, MeshCrashReporter.sanitizeIdentifier(anon))
        assertTrue(!MeshCrashReporter.sanitizeIdentifier(anon).contains("@"))
    }
}

