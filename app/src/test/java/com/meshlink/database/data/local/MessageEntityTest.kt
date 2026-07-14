package com.meshlink.database.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MessageEntityTest {

    @Test
    fun `test message entity default values`() {
        val message = MessageEntity(
            messageId = "msg_1",
            chatId = "chat_1",
            senderId = "user_1",
            text = "Hello",
            timestamp = 1000L,
            isFromMe = true,
            status = DeliveryStatus.SENT
        )
        
        // Defaults
        assertEquals(0L, message.localId)
        assertEquals(MessageType.TEXT, message.messageType)
        assertEquals(null, message.mediaPath)
        assertEquals(null, message.mediaDurationMs)
        assertEquals(null, message.latitude)
        assertEquals(null, message.longitude)
        assertEquals(null, message.batteryPercent)
    }

    @Test
    fun `test message entity equality ignores localId if configured appropriately, but data classes include it`() {
        val message1 = MessageEntity(1L, "msg_1", "chat_1", "user_1", "Hello", 1000L, true, DeliveryStatus.SENT)
        val message2 = MessageEntity(1L, "msg_1", "chat_1", "user_1", "Hello", 1000L, true, DeliveryStatus.SENT)
        val message3 = MessageEntity(2L, "msg_1", "chat_1", "user_1", "Hello", 1000L, true, DeliveryStatus.SENT)

        assertEquals(message1, message2)
        assertNotEquals(message1, message3)
    }
}
