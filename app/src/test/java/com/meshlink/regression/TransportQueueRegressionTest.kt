package com.meshlink.regression

import com.meshlink.routing.engine.TransportMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportQueueRegressionTest {

    @Test
    fun testTransportQueueDepthAndUtilization() {
        val metrics = TransportMetrics()
        metrics.recordQueueDepth(25, 100)

        assertEquals(25, metrics.queueDepth)
        assertEquals(25.0f, metrics.queueUtilizationPct, 0.1f)
    }
}
