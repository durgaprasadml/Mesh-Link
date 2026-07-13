package com.meshlink.data.ble

import android.util.Log
import com.meshlink.data.analytics.MeshAnalytics
import com.meshlink.data.local.RelayDao
import com.meshlink.data.security.TrustManager
import com.meshlink.data.security.TrustLevel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshRouterTest {

    private lateinit var gattManager: BleGattManager
    private lateinit var analytics: MeshAnalytics
    private lateinit var relayDao: RelayDao
    private lateinit var meshRouter: MeshRouter

    private val incomingMessagesFlow = MutableSharedFlow<Pair<String, String>>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } answers { println("Log.d: ${arg<String>(0)}: ${arg<String>(1)}"); 0 }
        every { Log.e(any(), any<String>()) } answers { println("Log.e: ${arg<String>(0)}: ${arg<String>(1)}"); 0 }
        every { Log.e(any(), any<String>(), any()) } answers { println("Log.e: ${arg<String>(0)}: ${arg<String>(1)} ${arg<Throwable>(2)}"); 0 }
        every { Log.i(any(), any<String>()) } answers { println("Log.i: ${arg<String>(0)}: ${arg<String>(1)}"); 0 }
        every { Log.w(any(), any<String>()) } answers { println("Log.w: ${arg<String>(0)}: ${arg<String>(1)}"); 0 }
        every { Log.w(any(), any<String>(), any()) } answers { println("Log.w: ${arg<String>(0)}: ${arg<String>(1)} ${arg<Throwable>(2)}"); 0 }
        every { Log.v(any(), any<String>()) } answers { println("Log.v: ${arg<String>(0)}: ${arg<String>(1)}"); 0 }

        gattManager = mockk(relaxed = true)
        every { gattManager.incomingMessages } returns incomingMessagesFlow

        analytics = mockk(relaxed = true)
        relayDao = mockk(relaxed = true)
        val trustManager = mockk<TrustManager>(relaxed = true)
        every { trustManager.getTrustLevel(any()) } returns TrustLevel.TRUSTED

        val map = java.util.concurrent.ConcurrentHashMap<String, android.bluetooth.BluetoothDevice>()
        map["peer2"] = mockk(relaxed = true)
        every { gattManager.connectedServers } returns map
        every { gattManager.activeClients } returns java.util.concurrent.ConcurrentHashMap()

        meshRouter = MeshRouter(gattManager, analytics, relayDao, trustManager)
        meshRouter.localMeshId = "local_node"
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `test incoming packet increases hop count and forwards if not target`() = runTest(testDispatcher) {
        val originalPacket = MeshPacket(
            senderId = "sender1",
            targetId = "target1",
            payload = "hello",
            ttl = 5,
            hopCount = 1,
            visitedPath = mutableListOf("sender1")
        )

        val json = MeshPacketParser.toJson(originalPacket)
        
        // Emit incoming message
        incomingMessagesFlow.emit(Pair("sender1", json))
        Thread.sleep(100) // Real thread sleep to wait for Dispatchers.IO
        advanceUntilIdle()
        
        // Verify broadcast packet is called
        verify(timeout = 2000) {
            gattManager.broadcastPacket(withArg { forwardedJson ->
                val forwarded = MeshPacketParser.fromJson(forwardedJson)
                assertEquals(2, forwarded?.hopCount)
                assertEquals(4, forwarded?.ttl)
                assertEquals(listOf("sender1", "local_node"), forwarded?.visitedPath)
            }, any(), any())
        }
    }

    @Test
    fun `test packet delivered to local node`() = runTest(testDispatcher) {
        val originalPacket = MeshPacket(
            senderId = "sender1",
            targetId = "local_node",
            payload = "hello to me",
            ttl = 5,
            hopCount = 1
        )

        val json = MeshPacketParser.toJson(originalPacket)

        val received = mutableListOf<Pair<String, MeshPacket>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            meshRouter.incomingPayloads.collect {
                received.add(it)
            }
        }

        incomingMessagesFlow.emit(Pair("sender1", json))
        Thread.sleep(100) // Real thread sleep to wait for Dispatchers.IO
        advanceUntilIdle() // Run testDispatcher tasks

        assertEquals(1, received.size)
        assertEquals("hello to me", received[0].second.payload)
        
        job.cancel()
    }

    @Test
    fun `test packet dropped if ttl is 0`() = runTest(testDispatcher) {
        val originalPacket = MeshPacket(
            senderId = "sender1",
            targetId = "target1",
            payload = "hello",
            ttl = 0,
            hopCount = 5
        )

        val json = MeshPacketParser.toJson(originalPacket)
        incomingMessagesFlow.emit(Pair("sender1", json))
        Thread.sleep(100)
        advanceUntilIdle()

        // Verify broadcast is not called
        verify(exactly = 0, timeout = 2000) { gattManager.broadcastPacket(any()) }
    }
}
