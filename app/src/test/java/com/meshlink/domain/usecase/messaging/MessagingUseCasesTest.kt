package com.meshlink.domain.usecase.messaging

import app.cash.turbine.test
import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MessagingUseCasesTest {

    private lateinit var chatRepository: ChatRepository

    @Before
    fun setup() {
        chatRepository = mockk(relaxed = true)
    }

    @Test
    fun `GetChatMessagesUseCase should emit messages from repository`() = runTest {
        val useCase = GetChatMessagesUseCase(chatRepository)
        val mockMessages = listOf(mockk<Message>(), mockk<Message>())
        every { chatRepository.getMessagesForChat("chat_1") } returns flowOf(mockMessages)

        useCase("chat_1").test {
            assertEquals(mockMessages, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `GetBroadcastMessagesUseCase should emit broadcast messages`() = runTest {
        val useCase = GetBroadcastMessagesUseCase(chatRepository)
        val mockMessages = listOf(mockk<Message>())
        every { chatRepository.getBroadcastMessages() } returns flowOf(mockMessages)

        useCase().test {
            assertEquals(mockMessages, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `GetAllChatsUseCase should emit all chats`() = runTest {
        val useCase = GetAllChatsUseCase(chatRepository)
        val mockChats = listOf(mockk<Chat>(), mockk<Chat>())
        every { chatRepository.getAllChats() } returns flowOf(mockChats)

        useCase().test {
            assertEquals(mockChats, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `DeleteMessagesUseCase should call repository deleteMessages`() = runTest {
        val useCase = DeleteMessagesUseCase(chatRepository)
        val ids = listOf("msg_1", "msg_2")
        
        useCase(ids)
        
        coVerify(exactly = 1) { chatRepository.deleteMessages(ids) }
    }

    @Test
    fun `DeleteChatUseCase should call repository deleteChat`() = runTest {
        val useCase = DeleteChatUseCase(chatRepository)
        
        useCase("chat_1")
        
        coVerify(exactly = 1) { chatRepository.deleteChat("chat_1") }
    }

    @Test
    fun `MarkChatAsReadUseCase should call repository markChatAsRead`() = runTest {
        val useCase = MarkChatAsReadUseCase(chatRepository)
        
        useCase("chat_1")
        
        coVerify(exactly = 1) { chatRepository.markChatAsRead("chat_1") }
    }

    @Test
    fun `GetMessageUseCase should return message from repository`() = runTest {
        val useCase = GetMessageUseCase(chatRepository)
        val mockMessage = mockk<Message>()
        coEvery { chatRepository.getMessageByUuid("msg_1") } returns mockMessage

        val result = useCase("msg_1")
        
        assertEquals(mockMessage, result)
        coVerify(exactly = 1) { chatRepository.getMessageByUuid("msg_1") }
    }
}
