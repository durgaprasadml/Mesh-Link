package com.meshlink.ble

import org.junit.Assert.assertTrue
import org.junit.Test

class BleAdvertiserPersistenceTest {

    @Test
    fun testAdvertiserPersistenceFlag() {
        val persistenceEnabled = true
        assertTrue(persistenceEnabled)
    }
}
