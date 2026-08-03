package com.meshlink.regression

import com.meshlink.metrics.MemoryStressValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupRegressionTest {

    @Test
    fun testMemoryCleanupAndZeroLeakedBuffers() {
        val validator = MemoryStressValidator()
        val report = validator.executeStressValidation(
            textMessageCount = 200,
            imageTransferCount = 50,
            audioTransferCount = 10,
            connectionCycleCount = 10
        )

        assertTrue(report.bufferPoolConsistent)
        assertEquals(0, report.activeBorrowedBuffersRemaining)
        assertTrue(report.passed)
    }
}
