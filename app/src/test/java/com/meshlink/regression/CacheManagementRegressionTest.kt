package com.meshlink.regression

import com.meshlink.metrics.ProductionDiagnosticsReport
import com.meshlink.metrics.RuntimePerformanceMonitor
import com.meshlink.metrics.CoroutineDiagnostics
import com.meshlink.routing.engine.TransportHealthMonitor
import com.meshlink.routing.engine.TransportMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheManagementRegressionTest {

    @Test
    fun testSanitizedCacheDiagnostics() {
        val transportMetrics = TransportMetrics()
        val runtimeMonitor = RuntimePerformanceMonitor(transportMetrics)
        val healthMonitor = TransportHealthMonitor()
        val coroutineDiagnostics = CoroutineDiagnostics()

        val reporter = ProductionDiagnosticsReport(
            runtimeMonitor = runtimeMonitor,
            transportMetrics = transportMetrics,
            transportHealthMonitor = healthMonitor,
            coroutineDiagnostics = coroutineDiagnostics
        )

        val report = reporter.generateSanitizedReport()
        assertTrue(report.isSanitized)
        assertEquals("HEALTHY", report.cacheSummary["status"])
    }
}
