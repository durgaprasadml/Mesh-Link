package com.meshlink.messaging.data

import com.meshlink.routing.api.PacketFailed
import com.meshlink.routing.api.PacketQueued
import com.meshlink.routing.api.PacketStatusEvent
import com.meshlink.routing.api.PacketTransmitted
import com.meshlink.routing.api.Router
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val router = mockk<Router>(relaxed = true)
    private val stateMachine = mockk<MessageStateMachine>(relaxed = true)
    private val packetEventsFlow = MutableSharedFlow<PacketStatusEvent>()

    private lateinit var deliveryTracker: DeliveryTracker

    @Before
    fun setUp() {
        every { router.packetEvents } returns packetEventsFlow

        deliveryTracker = DeliveryTracker(
            router = router,
            stateMachine = stateMachine,
            applicationScope = testScope.backgroundScope
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `PacketQueued event transitions message state to QUEUED`() = testScope.runTest {
        packetEventsFlow.emit(PacketQueued("pkt_1"))
        testScheduler.advanceUntilIdle()

        coVerify { stateMachine.transitionToQueued("pkt_1") }
    }

    @Test
    fun `PacketTransmitted event transitions message state to SENT and starts timeout`() = testScope.runTest {
        packetEventsFlow.emit(PacketTransmitted("pkt_1"))
        testScheduler.advanceUntilIdle()

        coVerify { stateMachine.transitionToSent("pkt_1") }

        // Fast forward time by 30 seconds (DELIVERY_TIMEOUT_MS)
        testScheduler.advanceTimeBy(DeliveryTracker.DELIVERY_TIMEOUT_MS + 100)

        coVerify { stateMachine.transitionToFailed("pkt_1") }
    }

    @Test
    fun `PacketFailed event cancels timeout and transitions message state to FAILED`() = testScope.runTest {
        packetEventsFlow.emit(PacketTransmitted("pkt_1"))
        testScheduler.advanceUntilIdle()

        packetEventsFlow.emit(PacketFailed("pkt_1", Exception("Network error")))
        testScheduler.advanceUntilIdle()

        coVerify { stateMachine.transitionToFailed("pkt_1") }

        // Fast forward time past timeout to verify timeout job was cancelled
        testScheduler.advanceTimeBy(DeliveryTracker.DELIVERY_TIMEOUT_MS + 100)

        // transitionToFailed should only be called once from PacketFailed, not from timeout
        coVerify(exactly = 1) { stateMachine.transitionToFailed("pkt_1") }
    }

    @Test
    fun `onAckReceived cancels timeout and transitions message state to DELIVERED`() = testScope.runTest {
        packetEventsFlow.emit(PacketTransmitted("pkt_1"))
        testScheduler.advanceUntilIdle()

        deliveryTracker.onAckReceived("pkt_1")
        testScheduler.advanceUntilIdle()

        coVerify { stateMachine.transitionToDelivered("pkt_1") }

        // Fast forward time past timeout to verify timeout job was cancelled
        testScheduler.advanceTimeBy(DeliveryTracker.DELIVERY_TIMEOUT_MS + 100)

        coVerify(exactly = 0) { stateMachine.transitionToFailed("pkt_1") }
    }

    @Test
    fun `onReadReceiptReceived cancels timeout and transitions message state to SEEN`() = testScope.runTest {
        packetEventsFlow.emit(PacketTransmitted("pkt_1"))
        testScheduler.advanceUntilIdle()

        deliveryTracker.onReadReceiptReceived("pkt_1")
        testScheduler.advanceUntilIdle()

        coVerify { stateMachine.transitionToSeen("pkt_1") }

        // Fast forward time past timeout
        testScheduler.advanceTimeBy(DeliveryTracker.DELIVERY_TIMEOUT_MS + 100)

        coVerify(exactly = 0) { stateMachine.transitionToFailed("pkt_1") }
    }
}
