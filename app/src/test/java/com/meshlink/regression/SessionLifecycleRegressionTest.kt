package com.meshlink.regression

import com.meshlink.metrics.RuntimePerformanceMonitor
import com.meshlink.routing.engine.TransportMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLifecycleRegressionTest {

    @Test
    fun testSessionLifecycleTracking() {
        val transportMetrics = TransportMetrics()
        val runtimeMonitor = RuntimePerformanceMonitor(transportMetrics)
        runtimeMonitor.setMonitoringEnabled(true)

        val snapBefore = runtimeMonitor.snapshot()
        assertEquals(0, snapBefore.activeSessions)

        runtimeMonitor.recordSessionStart()
        assertEquals(1, runtimeMonitor.snapshot().activeSessions)

        runtimeMonitor.recordSessionEnd()
        assertEquals(0, runtimeMonitor.snapshot().activeSessions)
    }
}
