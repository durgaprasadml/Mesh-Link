package com.meshlink.domain.usecase.messaging

import com.meshlink.common.recovery.StateRestorationManager
import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.repository.ChatRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingUseCasesTest {

    private val chatRepository = mockk<ChatRepository>(relaxed = true)
    private val stateRestorationManager = mockk<StateRestorationManager>(relaxed = true)

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `GetChatMessagesUseCase invokes repository getMessagesForChat`() = runTest {
        val messages = listOf(
            Message("m1", "c1", "Hello", "s1", System.currentTimeMillis(), true, DeliveryStatus.DELIVERED, MessageType.TEXT)
        )
        every { chatRepository.getMessagesForChat("c1") } returns flowOf(messages)

        val useCase = GetChatMessagesUseCase(chatRepository)
        val result = useCase("c1").first()

        assertEquals(1, result.size)
        assertEquals("m1", result.first().messageId)
    }

    @Test
    fun `GetBroadcastMessagesUseCase invokes repository getBroadcastMessages`() = runTest {
        val messages = listOf(
            Message("m2", "BROADCAST", "Alert", "s1", System.currentTimeMillis(), false, DeliveryStatus.SEEN, MessageType.TEXT)
        )
        every { chatRepository.getBroadcastMessages() } returns flowOf(messages)

        val useCase = GetBroadcastMessagesUseCase(chatRepository)
        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("m2", result.first().messageId)
    }

    @Test
    fun `GetAllChatsUseCase returns chats flow from repository`() = runTest {
        val chats = listOf(Chat(id = "c1", name = "Alice", lastMessage = "Hello", lastMessageAt = System.currentTimeMillis(), unreadCount = 0))
        every { chatRepository.getAllChats() } returns flowOf(chats)

        val useCase = GetAllChatsUseCase(chatRepository)
        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("Alice", result.first().name)
    }

    @Test
    fun `DeleteMessagesUseCase delegates to chatRepository deleteMessages`() = runTest {
        val useCase = DeleteMessagesUseCase(chatRepository)
        useCase(listOf("m1", "m2"))

        coVerify { chatRepository.deleteMessages(listOf("m1", "m2")) }
    }

    @Test
    fun `DeleteChatUseCase deletes chat and clears active chat draft if active`() = runTest {
        val useCase = DeleteChatUseCase(chatRepository, stateRestorationManager)
        useCase("c1")

        coVerify { chatRepository.deleteChat("c1") }
        coVerify { stateRestorationManager.updateState(any()) }
    }

    @Test
    fun `MarkChatAsReadUseCase delegates to chatRepository markChatAsRead`() = runTest {
        val useCase = MarkChatAsReadUseCase(chatRepository)
        useCase("c1")

        coVerify { chatRepository.markChatAsRead("c1") }
    }

    @Test
    fun `GetMessageUseCase fetches message by uuid from repository`() = runTest {
        val message = Message("m1", "c1", "Hello", "s1", System.currentTimeMillis(), true, DeliveryStatus.SENT, MessageType.TEXT)
        coEvery { chatRepository.getMessageByUuid("m1") } returns message

        val useCase = GetMessageUseCase(chatRepository)
        val result = useCase("m1")

        assertNotNull(result)
        assertEquals("m1", result?.messageId)
    }
}
