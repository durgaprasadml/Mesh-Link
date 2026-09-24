package com.meshlink.ui.broadcast

import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.usecase.messaging.GetBroadcastMessagesUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val getBroadcastMessagesUseCase = mockk<GetBroadcastMessagesUseCase>(relaxed = true)
    private val scannedDevicesFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())

    private val usersFlow = MutableStateFlow<List<User>>(emptyList())

    private lateinit var viewModel: BroadcastViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val messages = listOf(
            Message("b1", "BROADCAST", "Emergency Alert", "s1", System.currentTimeMillis(), true, DeliveryStatus.SENT, MessageType.TEXT),
            Message("b2", "BROADCAST", "Follow up", "s1", System.currentTimeMillis(), true, DeliveryStatus.SENT, MessageType.TEXT)
        )
        every { getBroadcastMessagesUseCase() } returns flowOf(messages)
        every { meshRepository.scannedDevices } returns scannedDevicesFlow
        every { userRepository.observeAllUsers() } returns usersFlow
        coEvery { userRepository.getUserDisplayName("s1") } returns "Durga Prasad"
        coEvery { userRepository.getUserProfile("s1") } returns null

        viewModel = BroadcastViewModel(meshRepository, userRepository, getBroadcastMessagesUseCase)
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

        coVerify(exactly = 1) { meshRepository.broadcastMessage("Public Emergency Warning") }
    }

    @Test
    fun `sendBroadcast ignores blank or whitespace-only messages`() = runTest(testDispatcher) {
        viewModel.sendBroadcast("   ")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { meshRepository.broadcastMessage(any()) }
    }

    @Test
    fun `uiState maps broadcast messages flow with resolved sender name and batches profile lookups`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("b1", state.messages[0].message.messageId)
        assertEquals("Emergency Alert", state.messages[0].message.text)
        assertEquals("Durga Prasad", state.messages[0].senderName)

        assertEquals("b2", state.messages[1].message.messageId)
        assertEquals("Follow up", state.messages[1].message.text)
        assertEquals("Durga Prasad", state.messages[1].senderName)

        // Verifies batching: even though there are 2 messages with sender "s1", lookup is only performed once
        coVerify(exactly = 1) { userRepository.getUserDisplayName("s1") }
        coVerify(exactly = 1) { userRepository.getUserProfile("s1") }
    }

    @Test
    fun `uiState reflects nearby devices count from meshRepository`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.nearbyDevicesCount)

        val device = mockk<BleDevice>(relaxed = true)
        scannedDevicesFlow.value = mapOf("node_1" to device, "node_2" to device)
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.nearbyDevicesCount)
    }

    @Test
    fun `uiState reactively updates sender name when peer profile arrives after message`() = runTest(testDispatcher) {
        val unknownMessages = listOf(
            Message("b1", "BROADCAST", "Hello from peer", "peer_raju", System.currentTimeMillis(), false, DeliveryStatus.DELIVERED, MessageType.TEXT)
        )
        every { getBroadcastMessagesUseCase() } returns flowOf(unknownMessages)
        coEvery { userRepository.getUserDisplayName("peer_raju") } returns "Mesh Peer"
        coEvery { userRepository.getUserProfile("peer_raju") } returns null

        val vm = BroadcastViewModel(meshRepository, userRepository, getBroadcastMessagesUseCase)

        backgroundScope.launch { vm.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // Initially unresolved, shows neutral placeholder
        assertEquals("Mesh Peer", vm.uiState.value.messages[0].senderName)

        // Peer profile arrives asynchronously (from beacon, handshake, or broadcast senderName)
        coEvery { userRepository.getUserDisplayName("peer_raju") } returns "Raju"
        usersFlow.value = listOf(User("peer_raju", "Raju"))
        testScheduler.advanceUntilIdle()

        // UI state reactively updates to "Raju" without requiring screen reload or user action
        assertEquals("Raju", vm.uiState.value.messages[0].senderName)
    }
}
