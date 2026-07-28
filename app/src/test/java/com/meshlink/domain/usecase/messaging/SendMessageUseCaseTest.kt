package com.meshlink.domain.usecase.messaging

import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.ChatRepository
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SendMessageUseCaseTest {

    private val chatRepository = mockk<ChatRepository>(relaxed = true)
    private val meshRepository = mockk<MeshRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)

    private lateinit var sendMessageUseCase: SendMessageUseCase

    @Before
    fun setUp() {
        sendMessageUseCase = SendMessageUseCase(
            chatRepository = chatRepository,
            meshRepository = meshRepository,
            userRepository = userRepository
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `invoke returns early if local user is null`() = runTest {
        coEvery { userRepository.getLocalUser() } returns null

        sendMessageUseCase("target_123", "Hello", "Chat Name")

        coVerify(exactly = 0) { meshRepository.resolveChatId(any()) }
        coVerify(exactly = 0) { chatRepository.saveMessage(any(), any()) }
        coVerify(exactly = 0) { meshRepository.sendMessage(any(), any()) }
    }

    @Test
    fun `invoke creates message, saves to chatRepository, and sends via meshRepository`() = runTest {
        val localUser = User(meshId = "local_mesh_id", name = "Alice")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { meshRepository.resolveChatId("target_mesh_id") } returns "chat_id_normalized"

        sendMessageUseCase(
            targetMeshId = "target_mesh_id",
            messageText = "Test Message",
            chatName = "Bob"
        )

        coVerify {
            chatRepository.saveMessage(
                message = match {
                    it.chatId == "chat_id_normalized" &&
                    it.text == "Test Message" &&
                    it.senderId == "local_mesh_id" &&
                    it.isFromMe &&
                    it.status == DeliveryStatus.QUEUED &&
                    it.messageType == MessageType.TEXT
                },
                chatName = "Bob"
            )
        }

        coVerify {
            meshRepository.sendMessage(
                targetMeshId = "target_mesh_id",
                message = match {
                    it.chatId == "chat_id_normalized" &&
                    it.text == "Test Message"
                }
            )
        }
    }
}
