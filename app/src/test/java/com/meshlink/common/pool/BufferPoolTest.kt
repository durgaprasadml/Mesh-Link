package com.meshlink.common.pool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BufferPoolTest {

    @Before
    fun setup() {
        BufferPool.trimMemory() // Reset for each test
    }

    @Test
    fun `borrowBuffer allocates new buffer when empty`() {
        val size = 512
        val buffer = BufferPool.borrowBuffer(size)
        assertEquals(size, buffer.size)
        assertEquals(1, BufferPool.missCount.get())
        assertEquals(0, BufferPool.hitCount.get())
    }

    @Test
    fun `borrowBuffer returns pooled buffer`() {
        val size = 512
        val buffer1 = BufferPool.borrowBuffer(size)
        BufferPool.returnBuffer(buffer1)
        
        val buffer2 = BufferPool.borrowBuffer(size)
        assertSame("Buffer should be the exact same object", buffer1, buffer2)
        assertEquals(1, BufferPool.hitCount.get())
    }

    @Test
    fun `returnSecureBuffer clears content before pooling`() {
        val size = 512
        val buffer1 = BufferPool.borrowBuffer(size)
        buffer1[0] = 42
        buffer1[1] = -1
        
        BufferPool.returnSecureBuffer(buffer1)
        
        val buffer2 = BufferPool.borrowBuffer(size)
        assertSame(buffer1, buffer2)
        assertEquals("Content must be zeroed", 0.toByte(), buffer2[0])
        assertEquals("Content must be zeroed", 0.toByte(), buffer2[1])
    }

    @Test
    fun `pool limits prevent unbounded memory growth`() {
        val size = 128
        val buffers = mutableListOf<ByteArray>()
        
        // Exceed the capacity of 100
        for (i in 0..150) {
            buffers.add(ByteArray(size))
        }
        
        buffers.forEach { BufferPool.returnBuffer(it) }
        
        assertTrue("Eviction count should reflect dropped buffers", BufferPool.evictionCount.get() >= 50)
    }

    @Test
    fun `borrowCleanBuffer ensures zeros`() {
        val size = 256
        val buffer1 = BufferPool.borrowBuffer(size)
        buffer1.fill(99)
        BufferPool.returnBuffer(buffer1) // returned dirty
        
        val buffer2 = BufferPool.borrowCleanBuffer(size)
        assertSame(buffer1, buffer2)
        assertEquals(0.toByte(), buffer2[0])
    }
}
