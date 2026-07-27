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
}
