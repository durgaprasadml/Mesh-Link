package com.meshlink.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ReceivedMediaNamingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `sanitizeComponent strips path traversal sequences`() {
        val sanitizedUnix = ReceivedMediaNaming.sanitizeComponent("../../../evil.sh", fallback = "fallback.bin")
        assertEquals("evil.sh", sanitizedUnix)

        val sanitizedWindows = ReceivedMediaNaming.sanitizeComponent("..\\..\\evil.exe", fallback = "fallback.bin")
        assertEquals("evil.exe", sanitizedWindows)

        val sanitizedOnlyDots = ReceivedMediaNaming.sanitizeComponent("....", fallback = "fallback.bin")
        assertEquals("fallback.bin", sanitizedOnlyDots)
    }

    @Test
    fun `sanitizeComponent preserves extension on long names`() {
        val longName = "a".repeat(120) + ".jpg"
        val sanitized = ReceivedMediaNaming.sanitizeComponent(longName, fallback = "fallback.bin", maxLength = 96)
        assertTrue(sanitized.endsWith(".jpg"))
        assertTrue(sanitized.length <= 96)
    }

    @Test
    fun `receivedFileName isolates two transfers with identical file names`() {
        val frontName = ReceivedMediaNaming.receivedFileName("sos_event_123_front_peerA", "sos_photo.jpg")
        val rearName = ReceivedMediaNaming.receivedFileName("sos_event_123_rear_peerA", "sos_photo.jpg")

        assertNotEquals(frontName, rearName)
        assertTrue(frontName.contains("front"))
        assertTrue(rearName.contains("rear"))
        assertTrue(frontName.endsWith("__sos_photo.jpg"))
        assertTrue(rearName.endsWith("__sos_photo.jpg"))
    }

    @Test
    fun `resolveInside succeeds for names within directory`() {
        val root = tempFolder.newFolder("media")
        val resolved = ReceivedMediaNaming.resolveInside(root, "valid__image.jpg")
        assertEquals(File(root, "valid__image.jpg").canonicalPath, resolved.canonicalPath)
    }

    @Test
    fun `resolveInside throws IOException for traversal attempt`() {
        val root = tempFolder.newFolder("media")
        try {
            ReceivedMediaNaming.resolveInside(root, "../outside.txt")
            fail("Expected IOException for traversal attempt")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Refusing to resolve") == true)
        }
    }
}
