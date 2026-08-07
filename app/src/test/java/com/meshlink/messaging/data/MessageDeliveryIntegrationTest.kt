package com.meshlink.messaging.data

import com.meshlink.common.recovery.RetryCoordinator
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.IntelligentRetryEngine
import com.meshlink.ui.components.chat.DeliveryUiState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
class MessageDeliveryIntegrationTest {

    private val chatDao = mockk<ChatDao>(relaxed = true)
    private val meshRepository = mockk<MeshRepository>(relaxed = true)
    private val meshRepositoryProvider = Provider { meshRepository }
    private val meshRouter = mockk<Router>(relaxed = true)
    private val intelligentRetryEngine = mockk<IntelligentRetryEngine>(relaxed = true)
    
    private lateinit var stateMachine: MessageStateMachine

    @Before
    fun setUp() {
        stateMachine = MessageStateMachine(chatDao)
        coEvery { intelligentRetryEngine.shouldRetryNow() } returns true
        coEvery { intelligentRetryEngine.calculateRetryDelay(any()) } returns 1000L
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `MessageStateMachine prevents downgrading DELIVERED to RETRYING`() = runTest {
        val existing = MessageEntity(
            messageId = "msg_1",
            chatId = "peer_1",
            senderId = "my_id",
            text = "Hello",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
        )
        coEvery { chatDao.getMessageByUuid("msg_1") } returns existing

        val result = stateMachine.transitionToRetrying("msg_1")

        assertFalse(result)
        coVerify(exactly = 0) { chatDao.updateMessageStatus("msg_1", any()) }
        coVerify(exactly = 0) { chatDao.updateMessageStatusConditional("msg_1", any(), any()) }
    }

    @Test
    fun `MessageStateMachine prevents downgrading DELIVERED to WAITING_FOR_ROUTE`() = runTest {
        val existing = MessageEntity(
            messageId = "msg_1",
            chatId = "peer_1",
            senderId = "my_id",
            text = "Hello",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
        )
        coEvery { chatDao.getMessageByUuid("msg_1") } returns existing

        val result = stateMachine.transitionToWaitingForRoute("msg_1")

        assertFalse(result)
        coVerify(exactly = 0) { chatDao.updateMessageStatus("msg_1", any()) }
    }

    @Test
    fun `MessageStateMachine prevents downgrading SEEN to DELIVERED`() = runTest {
        val existing = MessageEntity(
            messageId = "msg_1",
            chatId = "peer_1",
            senderId = "my_id",
            text = "Hello",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.SEEN,
            messageType = MessageType.TEXT
        )
        coEvery { chatDao.getMessageByUuid("msg_1") } returns existing

        val result = stateMachine.transitionToDelivered("msg_1")

        assertFalse(result)
        coVerify(exactly = 0) { chatDao.updateMessageStatus("msg_1", any()) }
    }

    @Test
    fun `MessageStateMachine allows valid transition from SENT to DELIVERED`() = runTest {
        val existing = MessageEntity(
            messageId = "msg_1",
            chatId = "peer_1",
            senderId = "my_id",
            text = "Hello",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.SENT,
            messageType = MessageType.TEXT
        )
        coEvery { chatDao.getMessageByUuid("msg_1") } returns existing
        coEvery { chatDao.updateMessageStatusConditional("msg_1", DeliveryStatus.DELIVERED, any()) } returns 1

        val result = stateMachine.transitionToDelivered("msg_1")

        assertTrue(result)
        coVerify { chatDao.updateMessageStatusConditional("msg_1", DeliveryStatus.DELIVERED, any()) }
    }

    @Test
    fun `DeliveryUiState correctly maps domain DeliveryStatus`() {
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.PENDING))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.QUEUED))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.SENDING))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.WAITING_FOR_ROUTE))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.WAITING_FOR_ACK))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.RETRYING))
        assertEquals(DeliveryUiState.Sent, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.SENT))
        assertEquals(DeliveryUiState.Delivered, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.DELIVERED))
        assertEquals(DeliveryUiState.Delivered, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.RELAYED))
        assertEquals(DeliveryUiState.Seen, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.SEEN))
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.FAILED))
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.PERMANENT_FAILURE))
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.CANCELLED))
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.EXPIRED))
    }
}
