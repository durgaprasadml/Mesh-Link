package com.meshlink.common.pool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BufferPoolLifecycleTest {

    @Before
    fun setup() {
        BufferPool.trimMemory()
        BufferPool.hitCount.set(0)
        BufferPool.missCount.set(0)
        BufferPool.evictionCount.set(0)
    }

    @Test
    fun `test active borrowed count tracking`() {
        val b1 = BufferPool.borrowBuffer(1024)
        val b2 = BufferPool.borrowBuffer(2048)

        assertEquals(2, BufferPool.getActiveBorrowedCount())

        BufferPool.returnBuffer(b1)
        assertEquals(1, BufferPool.getActiveBorrowedCount())

        BufferPool.returnBuffer(b2)
        assertEquals(0, BufferPool.getActiveBorrowedCount())
    }

    @Test
    fun `test double return detection`() {
        val b1 = BufferPool.borrowBuffer(512)
        BufferPool.returnBuffer(b1)

        val doubleReturnsBefore = BufferPool.doubleReturnCount.get()
        BufferPool.returnBuffer(b1) // Second return of same buffer
        val doubleReturnsAfter = BufferPool.doubleReturnCount.get()

        assertTrue(doubleReturnsAfter > doubleReturnsBefore)
    }

    @Test
    fun `test checkPoolConsistency returns true for consistent pool`() {
        val b1 = BufferPool.borrowBuffer(256)
        BufferPool.returnBuffer(b1)

        assertTrue(BufferPool.checkPoolConsistency())
    }

    @Test
    fun `test useBuffer returns buffer on exception`() {
        var borrowed: ByteArray? = null
        try {
            BufferPool.useBuffer(1024) { buf ->
                borrowed = buf
                throw RuntimeException("Simulated exception during buffer processing")
            }
        } catch (e: Exception) {
            // Expected exception
        }

        assertEquals(0, BufferPool.getActiveBorrowedCount())
    }
}
