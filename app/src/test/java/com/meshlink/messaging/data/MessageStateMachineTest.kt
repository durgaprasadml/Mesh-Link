package com.meshlink.messaging.data

import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageStateMachineTest {

    private val chatDao = mockk<ChatDao>(relaxed = true)
    private lateinit var messageStateMachine: MessageStateMachine

    @Before
    fun setUp() {
        messageStateMachine = MessageStateMachine(chatDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `transitionToQueued updates status to QUEUED`() = runTest {
        messageStateMachine.transitionToQueued("msg_1")
        coVerify { chatDao.updateMessageStatusConditional("msg_1", DeliveryStatus.QUEUED, any()) }
    }

    @Test
    fun `transitionToSent updates status if current state is not DELIVERED or SEEN`() = runTest {
        val message = mockk<MessageEntity>()
        every { message.status } returns DeliveryStatus.QUEUED
        coEvery { chatDao.getMessageByUuid("msg_1") } returns message

        messageStateMachine.transitionToSent("msg_1")

        coVerify { chatDao.updateMessageStatusConditional("msg_1", DeliveryStatus.SENT, any()) }
    }

    @Test
    fun `transitionToSent does not update status if already DELIVERED`() = runTest {
        val message = mockk<MessageEntity>()
        every { message.status } returns DeliveryStatus.DELIVERED
        coEvery { chatDao.getMessageByUuid("msg_1") } returns message

        messageStateMachine.transitionToSent("msg_1")

        coVerify(exactly = 0) { chatDao.updateMessageStatusConditional("msg_1", any(), any()) }
    }

    @Test
    fun `transitionToDelivered updates status to DELIVERED`() = runTest {
        messageStateMachine.transitionToDelivered("msg_1")
        coVerify { chatDao.updateMessageStatusConditional("msg_1", DeliveryStatus.DELIVERED, any()) }
    }

    @Test
    fun `transitionToSeen updates status to SEEN`() = runTest {
        messageStateMachine.transitionToSeen("msg_1")
        coVerify { chatDao.updateMessageStatusConditional("msg_1", DeliveryStatus.SEEN, any()) }
    }

    @Test
    fun `transitionToFailed does not update status if already SEEN`() = runTest {
        val message = mockk<MessageEntity>()
        every { message.status } returns DeliveryStatus.SEEN
        coEvery { chatDao.getMessageByUuid(any()) } returns message

        messageStateMachine.transitionToFailed("msg_1")

        coVerify(exactly = 0) { chatDao.updateMessageStatusConditional("msg_1", any(), any()) }
    }
}
