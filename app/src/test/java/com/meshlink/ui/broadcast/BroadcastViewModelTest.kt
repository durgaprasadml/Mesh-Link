package com.meshlink.ui.broadcast

import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.usecase.messaging.GetBroadcastMessagesUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BroadcastViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val meshRepository = mockk<MeshRepository>(relaxed = true)
    private val getBroadcastMessagesUseCase = mockk<GetBroadcastMessagesUseCase>(relaxed = true)

    private lateinit var viewModel: BroadcastViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val messages = listOf(
            Message("b1", "BROADCAST", "Emergency Alert", "s1", System.currentTimeMillis(), true, DeliveryStatus.SENT, MessageType.TEXT)
        )
        every { getBroadcastMessagesUseCase() } returns flowOf(messages)

        viewModel = BroadcastViewModel(meshRepository, getBroadcastMessagesUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `sendBroadcast delegates message to meshRepository broadcastMessage`() = runTest(testDispatcher) {
        viewModel.sendBroadcast("Public Emergency Warning")
        testScheduler.advanceUntilIdle()

        coVerify { meshRepository.broadcastMessage("Public Emergency Warning") }
    }

    @Test
    fun `uiState maps broadcast messages flow`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("b1", state.messages.first().messageId)
        assertEquals("Emergency Alert", state.messages.first().text)
    }
}
