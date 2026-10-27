package com.meshlink.ble.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the advertiser lifecycle bookkeeping that decides whether the local device
 * is *really* advertising and whether recovery is allowed to run.
 */
class BleAdvertisingTrackerTest {

    private var clockMs = 1_000_000L
    private val tracker = BleAdvertisingTracker { clockMs }
    private val request = AdvertisingRequest("Alice", "ALICE123", 0x01)

    @Test
    fun `isActive only after confirmed onStartSuccess`() {
        val token = tracker.request(request)
        assertFalse("requested is not advertising", tracker.isActive)
        tracker.onStarting(token)
        assertEquals(AdvertisingState.STARTING, tracker.state)
        assertFalse("starting is not advertising", tracker.isActive)

        assertTrue(tracker.onStartSuccess(token))
        assertTrue(tracker.isActive)
    }

    @Test
    fun `start failure is not reported as advertising and is flagged for recovery`() {
        // Before the fix isAdvertising was `callback != null`, i.e. true here, so the watchdog and
        // refreshMesh never restarted a failed advertiser.
        val token = tracker.request(request)
        tracker.onStarting(token)
        assertTrue(tracker.onStartFailure(token))

        assertFalse(tracker.isActive)
        assertEquals(AdvertisingState.FAILED, tracker.state)
        assertTrue(tracker.needsRecovery())
        assertEquals(request, tracker.desired)
    }

    @Test
    fun `callbacks from a superseded session are ignored`() {
        val first = tracker.request(request)
        tracker.onStarting(first)
        val second = tracker.request(request)
        tracker.onStarting(second)

        assertFalse("late success of superseded session", tracker.onStartSuccess(first))
        assertFalse("late failure of superseded session", tracker.onStartFailure(first))
        assertEquals(AdvertisingState.STARTING, tracker.state)
        assertTrue(tracker.onStartSuccess(second))
        assertTrue(tracker.isActive)
    }

    @Test
    fun `explicit stop invalidates a queued start so it never reaches the radio`() {
        val token = tracker.request(request)
        tracker.onStopped(byCaller = true)

        assertFalse(tracker.isCurrent(token))
        assertFalse(tracker.onStarting(token))
        assertNull(tracker.desired)
        assertFalse(tracker.needsRecovery())
    }

    @Test
    fun `internal stop before restart keeps the desired request`() {
        val token = tracker.request(request)
        tracker.onStarting(token)
        tracker.onStartSuccess(token)

        tracker.onStopped(byCaller = false)

        assertFalse(tracker.isActive)
        assertEquals(request, tracker.desired)
        assertTrue(tracker.needsRecovery())
        assertTrue("same token still current after internal stop", tracker.isCurrent(token))
    }

    @Test
    fun `a stopped or cancelled advertiser can be started again`() {
        val first = tracker.request(request)
        tracker.onStarting(first)
        tracker.onStartSuccess(first)
        tracker.onStopped(byCaller = true)
        assertFalse(tracker.isActive)

        val second = tracker.request(request)
        assertTrue(tracker.isCurrent(second))
        assertTrue(tracker.onStarting(second))
        assertTrue(tracker.onStartSuccess(second))
        assertTrue(tracker.isActive)
    }

    @Test
    fun `link re-arm is rate limited so connection churn cannot cause a restart storm`() {
        val token = tracker.request(request)
        tracker.onStarting(token)
        tracker.onStartSuccess(token)

        assertTrue("first re-arm allowed", tracker.tryAcquireRearm())
        clockMs += 1_000
        assertFalse("second re-arm inside the window denied", tracker.tryAcquireRearm())
        assertEquals(BleAdvertisingTracker.MIN_REARM_INTERVAL_MS - 1_000, tracker.rearmWaitMs())

        clockMs += BleAdvertisingTracker.MIN_REARM_INTERVAL_MS
        assertEquals(0L, tracker.rearmWaitMs())
        assertTrue("allowed again after the window", tracker.tryAcquireRearm())
    }

    @Test
    fun `link re-arm is denied when advertising is not wanted or a start is in flight`() {
        assertFalse("nothing wanted", tracker.tryAcquireRearm())

        val token = tracker.request(request)
        tracker.onStarting(token)
        assertFalse("start in flight", tracker.tryAcquireRearm())

        tracker.onStartSuccess(token)
        tracker.onStopped(byCaller = true)
        assertFalse("stopped by caller", tracker.tryAcquireRearm())
    }
}
