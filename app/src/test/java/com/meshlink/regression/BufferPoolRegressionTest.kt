package com.meshlink.regression

import com.meshlink.common.pool.BufferPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BufferPoolRegressionTest {

    @Test
    fun testBufferPoolBorrowReturnAndZeroDoubleReturns() {
        BufferPool.trimMemory()
        val size = 1024
        val buf1 = BufferPool.borrowBuffer(size)
        assertEquals(size, buf1.size)
        assertEquals(1, BufferPool.getActiveBorrowedCount())

        BufferPool.returnBuffer(buf1)
        assertEquals(0, BufferPool.getActiveBorrowedCount())
        assertTrue(BufferPool.checkPoolConsistency())

        // Double return check
        val initialDoubleReturns = BufferPool.doubleReturnCount.get()
        BufferPool.returnBuffer(buf1)
        assertTrue(BufferPool.doubleReturnCount.get() >= initialDoubleReturns)
    }
}
