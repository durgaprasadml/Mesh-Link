package com.meshlink.regression

import com.meshlink.metrics.CoroutineDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineLifecycleRegressionTest {

    @Test
    fun testCoroutineLifecycleTrackingAndZeroLeaks() {
        val diagnostics = CoroutineDiagnostics()
        diagnostics.setDiagnosticsEnabled(true)

        val jobId = "job_test_101"
        diagnostics.recordJobStart(jobId)
        val snapDuring = diagnostics.snapshot()
        assertEquals(1L, snapDuring.activeJobsCount)

        diagnostics.recordJobCompleted(jobId, 150L)
        val snapAfter = diagnostics.snapshot()
        assertEquals(0L, snapAfter.activeJobsCount)
        assertEquals(1L, snapAfter.completedJobsCount)
        assertTrue(snapAfter.isStructuredConcurrencyCompliant)
    }
}
