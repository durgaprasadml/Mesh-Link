package com.meshlink.regression

import com.meshlink.routing.engine.TransportMetrics
import com.meshlink.routing.engine.TransportPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoutingRegressionTest {

    @Test
    fun testTransportMetricsResetAndCounters() {
        val metrics = TransportMetrics()
        metrics.recordBlePacket(100)
        metrics.recordWifiPacket(500)
        assertEquals(1L, metrics.blePacketCount)
        assertEquals(1L, metrics.wifiPacketCount)
        assertEquals(600L, metrics.totalBytesTransferred)

        metrics.reset()
        assertEquals(0L, metrics.blePacketCount)
        assertEquals(0L, metrics.wifiPacketCount)
        assertEquals(0L, metrics.totalBytesTransferred)
    }

    @Test
    fun testTransportPolicyDefaults() {
        val classifier = com.meshlink.routing.engine.TransportPacketClassifier()
        val policy = TransportPolicy(classifier)
        assertNotNull(policy)
    }
}
