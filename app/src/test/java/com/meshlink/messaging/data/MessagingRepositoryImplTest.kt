package com.meshlink.messaging.data

import app.cash.turbine.test
import com.meshlink.database.data.local.ChatEntity
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.source.ChatLocalDataSource
import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MessagingRepositoryImplTest {

    private lateinit var chatLocalDataSource: ChatLocalDataSource
    private lateinit var repository: MessagingRepositoryImpl

    @Before
    fun setup() {
        chatLocalDataSource = mockk(relaxed = true)
        repository = MessagingRepositoryImpl(chatLocalDataSource)
    }

    @Test
    fun `getMessagesForChat should map entities to domain models`() = runTest {
        val entity = MessageEntity(
            messageId = "msg_1",
            chatId = "chat_1",
            senderId = "sender_1",
            text = "Hello",
            timestamp = 1000L,
            isFromMe = true,
            status = com.meshlink.database.data.local.DeliveryStatus.SENT,
            messageType = com.meshlink.database.data.local.MessageType.TEXT,
            mediaPath = null,
            mediaDurationMs = null
        )
        every { chatLocalDataSource.getMessagesForChat("chat_1") } returns flowOf(listOf(entity))

        repository.getMessagesForChat("chat_1").test {
            val messages = awaitItem()
            assertEquals(1, messages.size)
            val domainMessage = messages[0]
            assertEquals("msg_1", domainMessage.messageId)
            assertEquals("Hello", domainMessage.text)
            assertEquals(DeliveryStatus.SENT, domainMessage.status)
            awaitComplete()
        }
    }

    @Test
    fun `saveMessage should call insertMessageAndUpdateChat if chatName is provided`() = runTest {
        val message = com.meshlink.util.TestDataFactory.createMessage(messageId = "msg_1", status = DeliveryStatus.PENDING)

        repository.saveMessage(message, "John Doe")

        coVerify(exactly = 1) { chatLocalDataSource.insertMessageAndUpdateChat(any(), "John Doe") }
        coVerify(exactly = 0) { chatLocalDataSource.insertMessage(any()) }
    }

    @Test
    fun `saveMessage should call insertMessage if chatName is null`() = runTest {
        val message = com.meshlink.util.TestDataFactory.createMessage(messageId = "msg_1", status = DeliveryStatus.PENDING)

        repository.saveMessage(message, null)

        coVerify(exactly = 0) { chatLocalDataSource.insertMessageAndUpdateChat(any(), any()) }
        coVerify(exactly = 1) { chatLocalDataSource.insertMessage(any()) }
    }

    @Test
    fun `updateMessageStatus should convert status and call source`() = runTest {
        repository.updateMessageStatus("msg_1", DeliveryStatus.DELIVERED)

        coVerify(exactly = 1) { 
            chatLocalDataSource.updateMessageStatus("msg_1", com.meshlink.database.data.local.DeliveryStatus.DELIVERED) 
        }
    }

    @Test
    fun `deleteMessages should call source`() = runTest {
        val ids = listOf("msg_1")
        repository.deleteMessages(ids)
        coVerify(exactly = 1) { chatLocalDataSource.deleteMessages(ids) }
    }

    @Test
    fun `deleteChat should call source`() = runTest {
        repository.deleteChat("chat_1")
        coVerify(exactly = 1) { chatLocalDataSource.deleteChat("chat_1") }
    }

    @Test
    fun `markChatAsRead should call source`() = runTest {
        repository.markChatAsRead("chat_1")
        coVerify(exactly = 1) { chatLocalDataSource.markChatAsRead("chat_1") }
    }
}
