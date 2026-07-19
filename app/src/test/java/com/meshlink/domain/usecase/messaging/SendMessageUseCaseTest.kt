package com.meshlink.domain.usecase.messaging

import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.ChatRepository
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SendMessageUseCaseTest {

    private lateinit var chatRepository: ChatRepository
    private lateinit var meshRepository: MeshRepository
    private lateinit var userRepository: UserRepository
    private lateinit var sendMessageUseCase: SendMessageUseCase

    @Before
    fun setup() {
        chatRepository = mockk(relaxed = true)
        meshRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        sendMessageUseCase = SendMessageUseCase(chatRepository, meshRepository, userRepository)
    }

    @Test
    fun `invoke should return early if local user is null`() = runTest {
        // Arrange
        coEvery { userRepository.getLocalUser() } returns null

        // Act
        sendMessageUseCase(
            targetMeshId = "target_123",
            messageText = "Hello",
            chatName = "John Doe"
        )

        // Assert
        coVerify(exactly = 0) { chatRepository.saveMessage(any(), any()) }
        coVerify(exactly = 0) { meshRepository.sendMessage(any(), any()) }
    }

    @Test
    fun `invoke should create message and call repositories if user exists`() = runTest {
        // Arrange
        val myUser = User(
            meshId = "my_id",
            name = "Me",
            phoneNumber = "123"
        )
        coEvery { userRepository.getLocalUser() } returns myUser

        val capturedMessages = mutableListOf<Message>()
        coEvery { chatRepository.saveMessage(capture(capturedMessages), any()) } returns Unit

        // Act
        sendMessageUseCase(
            targetMeshId = "target_123",
            messageText = "Hello",
            chatName = "John Doe"
        )

        // Assert
        coVerify(exactly = 1) { chatRepository.saveMessage(any(), eq("John Doe")) }
        coVerify(exactly = 1) { meshRepository.sendMessage(any(), eq("John Doe")) }

        val captured = capturedMessages.first()
        assertEquals("target_123", captured.chatId)
        assertEquals("Hello", captured.text)
        assertEquals("my_id", captured.senderId)
        assertEquals(true, captured.isFromMe)
        assertEquals(DeliveryStatus.PENDING, captured.status)
        assertEquals(MessageType.TEXT, captured.messageType)
    }
}
