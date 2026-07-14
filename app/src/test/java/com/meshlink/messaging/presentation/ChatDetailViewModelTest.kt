package com.meshlink.messaging.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.usecase.messaging.DeleteChatUseCase
import com.meshlink.domain.usecase.messaging.DeleteMessagesUseCase
import com.meshlink.domain.usecase.messaging.GetChatMessagesUseCase
import com.meshlink.domain.usecase.messaging.GetMessageUseCase
import com.meshlink.domain.usecase.messaging.MarkChatAsReadUseCase
import com.meshlink.domain.usecase.messaging.SendMessageUseCase
import com.meshlink.media.data.VoicePlayer
import com.meshlink.media.data.VoiceRecorder
import com.meshlink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var meshRepository: MeshRepository
    private lateinit var getChatMessagesUseCase: GetChatMessagesUseCase
    private lateinit var deleteMessagesUseCase: DeleteMessagesUseCase
    private lateinit var deleteChatUseCase: DeleteChatUseCase
    private lateinit var markChatAsReadUseCase: MarkChatAsReadUseCase
    private lateinit var getMessageUseCase: GetMessageUseCase
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var voicePlayer: VoicePlayer
    private lateinit var sendMessageUseCase: SendMessageUseCase
    
    private lateinit var viewModel: ChatDetailViewModel

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle(mapOf("address" to "peer_1", "name" to "John"))
        meshRepository = mockk(relaxed = true)
        getChatMessagesUseCase = mockk(relaxed = true)
        deleteMessagesUseCase = mockk(relaxed = true)
        deleteChatUseCase = mockk(relaxed = true)
        markChatAsReadUseCase = mockk(relaxed = true)
        getMessageUseCase = mockk(relaxed = true)
        voiceRecorder = mockk(relaxed = true)
        voicePlayer = mockk(relaxed = true)
        sendMessageUseCase = mockk(relaxed = true)

        every { meshRepository.resolveChatId(any()) } returns "peer_1"
        every { getChatMessagesUseCase("peer_1") } returns flowOf(emptyList())
        every { meshRepository.scannedDevices } returns MutableStateFlow(emptyMap())
        every { meshRepository.transferProgress } returns MutableStateFlow(emptyMap())
        every { voiceRecorder.isRecording } returns MutableStateFlow(false)
        every { voiceRecorder.elapsedMs } returns MutableStateFlow(0L)
        every { voicePlayer.currentlyPlaying } returns MutableStateFlow(null)
        every { voicePlayer.progress } returns MutableStateFlow(0f)
    }

    private fun createViewModel() {
        viewModel = ChatDetailViewModel(
            savedStateHandle, meshRepository, getChatMessagesUseCase, deleteMessagesUseCase,
            deleteChatUseCase, markChatAsReadUseCase, getMessageUseCase, voiceRecorder,
            voicePlayer, sendMessageUseCase
        )
    }

    @Test
    fun `sendMessage should call sendMessageUseCase`() = runTest {
        createViewModel()
        viewModel.sendMessage("Hello")
        coVerify(exactly = 1) { sendMessageUseCase("peer_1", "Hello", "John") }
    }
    
    @Test
    fun `sendMessage should not call usecase if text is blank`() = runTest {
        createViewModel()
        viewModel.sendMessage("   ")
        coVerify(exactly = 0) { sendMessageUseCase(any(), any(), any()) }
    }

    @Test
    fun `markChatAsRead should call markChatAsReadUseCase and sendReadReceipts`() = runTest {
        createViewModel()
        viewModel.markChatAsRead()
        coVerify(exactly = 2) { markChatAsReadUseCase("peer_1") }
        coVerify(exactly = 2) { meshRepository.sendReadReceipts("peer_1") }
    }

    @Test
    fun `deleteSelectedMessages should call use case and clear selection`() = runTest {
        createViewModel()
        viewModel.toggleMessageSelection("msg_1")
        viewModel.deleteSelectedMessages()
        
        coVerify(exactly = 1) { deleteMessagesUseCase(listOf("msg_1")) }
        assertEquals(emptySet<String>(), viewModel.selectedMessageIds.value)
    }
}
