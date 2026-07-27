package com.meshlink.ble.data.gatt

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PacketReassemblerTest {

    private lateinit var classUnderTest: PacketReassemblerImpl
    private val address = "00:11:22:33:44:55"

    @Before
    fun setup() {
        classUnderTest = PacketReassemblerImpl()
    }

    @Test
    fun `handleFragment TYPE_FULL returns string immediately`() {
        val payload = "Hello".toByteArray()
        val fragment = ByteArray(payload.size + 1)
        fragment[0] = PacketFragmenterImpl.TYPE_FULL
        System.arraycopy(payload, 0, fragment, 1, payload.size)

        val result = classUnderTest.handleFragment(address, fragment)
        assertEquals("Hello", result)
    }

    @Test
    fun `handleFragment START CONT END assembles correctly`() {
        val part1 = "Hel".toByteArray()
        val part2 = "lo ".toByteArray()
        val part3 = "World".toByteArray()

        val frag1 = ByteArray(part1.size + 1).apply { this[0] = PacketFragmenterImpl.TYPE_START; System.arraycopy(part1, 0, this, 1, part1.size) }
        val frag2 = ByteArray(part2.size + 1).apply { this[0] = PacketFragmenterImpl.TYPE_CONT; System.arraycopy(part2, 0, this, 1, part2.size) }
        val frag3 = ByteArray(part3.size + 1).apply { this[0] = PacketFragmenterImpl.TYPE_END; System.arraycopy(part3, 0, this, 1, part3.size) }

        assertNull(classUnderTest.handleFragment(address, frag1))
        assertNull(classUnderTest.handleFragment(address, frag2))
        
        val result = classUnderTest.handleFragment(address, frag3)
        assertEquals("Hello World", result)
    }

    @Test
    fun `clear removes pending fragments`() {
        val part1 = "Hel".toByteArray()
        val frag1 = ByteArray(part1.size + 1).apply { this[0] = PacketFragmenterImpl.TYPE_START; System.arraycopy(part1, 0, this, 1, part1.size) }

        classUnderTest.handleFragment(address, frag1)
        classUnderTest.clear(address)

        val part3 = "World".toByteArray()
        val frag3 = ByteArray(part3.size + 1).apply { this[0] = PacketFragmenterImpl.TYPE_END; System.arraycopy(part3, 0, this, 1, part3.size) }
        
        assertNull(classUnderTest.handleFragment(address, frag3))
    }
}
