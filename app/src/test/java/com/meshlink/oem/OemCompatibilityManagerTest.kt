package com.meshlink.oem

import com.meshlink.common.oem.OemCompatibilityManager
import com.meshlink.common.oem.OemVendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OemCompatibilityManagerTest {

    @Test
    fun testOemVendorValues() {
        val vendors = OemVendor.values()
        assertEquals(10, vendors.size)
        assertEquals(OemVendor.SAMSUNG, OemVendor.valueOf("SAMSUNG"))
    }

    @Test
    fun testOemCompatibilityManagerInstantiation() {
        val manager = OemCompatibilityManager()
        assertNotNull(manager.currentVendor)
        assertNotNull(manager.strategy)
    }
}
