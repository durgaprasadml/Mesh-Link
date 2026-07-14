package com.meshlink.database.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatEntityTest {

    @Test
    fun `test chat entity default values`() {
        val chat = ChatEntity(
            id = "chat_1",
            name = "General",
            lastMessage = null,
            lastMessageAt = 1000L
            // unreadCount should default to 0
        )
        
        assertEquals(0, chat.unreadCount)
        assertEquals(null, chat.lastMessage)
    }

    @Test
    fun `test chat entity equality`() {
        val chat1 = ChatEntity("chat_1", "General", "Hello", 1000L, 2)
        val chat2 = ChatEntity("chat_1", "General", "Hello", 1000L, 2)
        val chat3 = ChatEntity("chat_2", "General", "Hello", 1000L, 2)

        assertEquals(chat1, chat2)
        assertEquals(chat1.hashCode(), chat2.hashCode())
        assertNotEquals(chat1, chat3)
    }
}
