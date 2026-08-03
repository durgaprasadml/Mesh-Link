package com.meshlink.regression

import com.meshlink.metrics.TransferStressTest
import com.meshlink.routing.engine.TransportMetrics
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferEngineRegressionTest {

    @Test
    fun testTransferStressEngineResilience() {
        val metrics = TransportMetrics()
        val tester = TransferStressTest(metrics)

        val report = tester.executeTransferStressSuite(totalTransfers = 50)
        assertTrue(report.passed)
        assertTrue(report.completionRatePct >= 95.0f)
        assertTrue(report.successfulTransfersCount > 0)
    }
}
