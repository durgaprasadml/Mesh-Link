package com.meshlink.ble.data.gatt

import com.meshlink.common.pool.BufferPool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PacketFragmenterTest {

    private lateinit classUnderTest: PacketFragmenterImpl

    @Before
    fun setup() {
        classUnderTest = PacketFragmenterImpl()
    }

    @Test
    fun `fragment small payload creates TYPE_FULL`() = runTest {
        val payload = "Hello World".toByteArray()
        val fragments = mutableListOf<ByteArray>()

        classUnderTest.fragment(payload, 23) { fragment ->
            fragments.add(fragment.copyOf())
            BufferPool.returnBuffer(fragment)
        }

        assertEquals(1, fragments.size)
        assertEquals(PacketFragmenterImpl.TYPE_FULL, fragments[0][0])
        val reconstructed = String(fragments[0].copyOfRange(1, fragments[0].size))
        assertEquals("Hello World", reconstructed)
    }

    @Test
    fun `fragment large payload splits into START CONT END`() = runTest {
        // MTU 23 means max payload is 23 - 3 (header) - 1 (type) = 19 bytes per fragment
        val payload = ByteArray(50) { it.toByte() }
        val fragments = mutableListOf<ByteArray>()

        classUnderTest.fragment(payload, 23) { fragment ->
            fragments.add(fragment.copyOf())
            BufferPool.returnBuffer(fragment)
        }

        // 50 bytes / 19 bytes per fragment = 3 fragments (19, 19, 12)
        assertEquals(3, fragments.size)
        
        assertEquals(PacketFragmenterImpl.TYPE_START, fragments[0][0])
        assertEquals(PacketFragmenterImpl.TYPE_CONT, fragments[1][0])
        assertEquals(PacketFragmenterImpl.TYPE_END, fragments[2][0])

        val reassembled = fragments.map { it.copyOfRange(1, it.size) }.reduce { acc, bytes -> acc + bytes }
        assertArrayEquals(payload, reassembled)
    }
}
