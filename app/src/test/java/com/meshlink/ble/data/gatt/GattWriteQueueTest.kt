package com.meshlink.ble.data.gatt

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GattWriteQueueTest {

    private lateinit var classUnderTest: GattWriteQueueImpl
    private val address = "00:11:22:33:44:55"

    @Before
    fun setup() {
        classUnderTest = GattWriteQueueImpl()
    }

    @Test
    fun `enqueue and dequeue ready`() {
        val write = PendingClientWrite(address, byteArrayOf(1, 2, 3))
        classUnderTest.enqueue(write)

        val ready = classUnderTest.dequeueReady(System.currentTimeMillis()) { true }
        assertEquals(write, ready)
        assertFalse(classUnderTest.hasPendingForDevice(address))
    }

    @Test
    fun `dequeueReady respects device readiness`() {
        val write = PendingClientWrite(address, byteArrayOf(1))
        classUnderTest.enqueue(write)

        val notReady = classUnderTest.dequeueReady(System.currentTimeMillis()) { false }
        assertNull(notReady)

        val ready = classUnderTest.dequeueReady(System.currentTimeMillis()) { true }
        assertEquals(write, ready)
    }

    @Test
    fun `requeueWithBackoff increases nextAttemptTime`() {
        val write = PendingClientWrite(address, byteArrayOf(1))
        val backoff = classUnderTest.requeueWithBackoff(write, 10, 50L)
        
        assertTrue(backoff > 0)
        assertEquals(1, write.retryCount)
        assertTrue(write.nextAttemptTime > System.currentTimeMillis())

        // Ensure it's not ready immediately
        val notReady = classUnderTest.dequeueReady(System.currentTimeMillis()) { true }
        assertNull(notReady)
    }

    @Test
    fun `stuck active write auto-prunes after timeout unblocking subsequent writes`() {
        var currentTime = 1000L
        classUnderTest.clock = { currentTime }

        val write1 = PendingClientWrite(address, byteArrayOf(1))
        val write2 = PendingClientWrite(address, byteArrayOf(2))

        classUnderTest.enqueue(write1)
        classUnderTest.enqueue(write2)

        val dequeued1 = classUnderTest.dequeueReady(currentTime) { true }
        assertEquals(write1, dequeued1)

        // Set active write without callback arriving (dropped callback scenario)
        classUnderTest.setActiveWrite(dequeued1)
        assertTrue(classUnderTest.hasPendingForDevice(address))

        // Advance slightly (before timeout) — write2 cannot dequeue because write1 is active
        currentTime += 500L
        val blocked = classUnderTest.dequeueReady(currentTime) { true }
        assertNull(blocked)

        // Advance past timeout (3000ms) — pruneExpiredActiveWrites clears write1 and allows write2 to dequeue
        currentTime += GattWriteQueueImpl.ACTIVE_WRITE_TIMEOUT_MS + 100L
        val unblocked = classUnderTest.dequeueReady(currentTime) { true }
        assertEquals(write2, unblocked)
    }

    @Test
    fun `clearIfActiveAndExpired clears expired write but preserves non-expired write`() {
        var currentTime = 1000L
        classUnderTest.clock = { currentTime }

        val write = PendingClientWrite(address, byteArrayOf(42))
        classUnderTest.setActiveWrite(write)

        // Before timeout, clearIfActiveAndExpired returns null
        val notExpired = classUnderTest.clearIfActiveAndExpired(address, write, 3000L)
        assertNull(notExpired)
        assertTrue(classUnderTest.hasPendingForDevice(address))

        // Advance past timeout
        currentTime += 3500L
        val expired = classUnderTest.clearIfActiveAndExpired(address, write, 3000L)
        assertEquals(write, expired)
        assertFalse(classUnderTest.hasPendingForDevice(address))
    }
}
