package com.meshlink.database.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UserTest {

    @Test
    fun `test user entity creation and fields`() {
        val user = UserEntity(
            meshId = "mesh_123",
            name = "Alice",
            phoneNumber = "1234567890",
            pinHash = "hash123"
        )
        
        assertEquals("mesh_123", user.meshId)
        assertEquals("Alice", user.name)
        assertEquals("1234567890", user.phoneNumber)
        assertEquals("hash123", user.pinHash)
    }

    @Test
    fun `test user entity equality and hashcode`() {
        val user1 = UserEntity("mesh_1", "Alice", "111", "hash")
        val user2 = UserEntity("mesh_1", "Alice", "111", "hash")
        val user3 = UserEntity("mesh_2", "Bob", "222", "hash2")

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
        assertNotEquals(user1, user3)
        assertNotEquals(user1.hashCode(), user3.hashCode())
    }

    @Test
    fun `test user entity copy`() {
        val user = UserEntity("mesh_1", "Alice", "111", "hash")
        val copied = user.copy(name = "Alice Updated")
        
        assertEquals("mesh_1", copied.meshId)
        assertEquals("Alice Updated", copied.name)
        assertNotEquals(user, copied)
    }
}
