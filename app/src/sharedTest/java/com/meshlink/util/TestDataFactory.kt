package com.meshlink.util

import com.meshlink.domain.model.Message
import com.meshlink.domain.model.DeliveryStatus

object TestDataFactory {
    fun createMessage(
        messageId: String = "msg_1",
        chatId: String = "chat_1",
        senderId: String = "sender_1",
        text: String = "Test Message",
        timestamp: Long = 1000L,
        status: DeliveryStatus = DeliveryStatus.SENT
    ): Message {
        return Message(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            text = text,
            timestamp = timestamp,
            status = status,
            isFromMe = true,
            messageType = com.meshlink.domain.model.MessageType.TEXT,
            mediaPath = null,
            mediaDurationMs = null
        )
    }
}
