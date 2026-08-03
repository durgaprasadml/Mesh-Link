package com.meshlink.regression

import com.meshlink.metrics.PerformanceBenchmark
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptionRegressionTest {

    @Test
    fun testEncryptionBenchmarkPerformance() {
        val benchmark = PerformanceBenchmark()
        val encLatency = benchmark.measureEncryptionLatency(payloadSize = 1024, iterations = 50)
        val decLatency = benchmark.measureDecryptionLatency(payloadSize = 1024, iterations = 50)

        assertTrue(encLatency >= 0.0)
        assertTrue(decLatency >= 0.0)
        // Encryption processing should be under 5ms per 1KB block
        assertTrue(encLatency < 5.0)
        assertTrue(decLatency < 5.0)
    }
}
