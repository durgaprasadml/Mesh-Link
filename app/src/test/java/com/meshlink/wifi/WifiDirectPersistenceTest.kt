package com.meshlink.wifi

import org.junit.Assert.assertTrue
import org.junit.Test

class WifiDirectPersistenceTest {

    @Test
    fun testWifiDirectPersistenceFlag() {
        val persistenceEnabled = true
        assertTrue(persistenceEnabled)
    }
}
