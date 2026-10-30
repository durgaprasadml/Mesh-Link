package com.meshlink.wifi.data

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SessionManager
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class WifiSocketTransportTest {

    private val mockContext = mockk<android.content.Context>(relaxed = true)
    private val mockCryptoManager = mockk<MeshCryptoManager>(relaxed = true)
    private val mockSessionManager = mockk<SessionManager>(relaxed = true)

    private fun createTransport() = WifiSocketTransport(
        context = mockContext,
        applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
        cryptoManager = mockCryptoManager,
        sessionManager = mockSessionManager
    )

    private lateinit var transport: WifiSocketTransport

    @Before
    fun setup() {
        transport = createTransport()
    }

    @After
    fun teardown() {
        transport.stopServer()
        transport.disconnect()
    }

    private suspend fun eventually(timeoutMs: Long = 15000L, pollMs: Long = 50L, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) {
                delay(pollMs)
            }
        }
    }

    @Test
    fun `startServer and connectAsClient establishes successful socket connection`() = runBlocking {
        val serverTransport = createTransport()
        val clientTransport = createTransport()

        var receivedPacketByServer: MeshPacket? = null
        var receivedPacketByClient: MeshPacket? = null

        serverTransport.onPacketReceived = { receivedPacketByServer = it }
        clientTransport.onPacketReceived = { receivedPacketByClient = it }

        // Start Server on 127.0.0.1 (Loopback)
        serverTransport.startServer()

        // Client connects
        clientTransport.connectAsClient("127.0.0.1")
        eventually { clientTransport.isConnected() && serverTransport.isConnected() }

        assertTrue(clientTransport.isConnected())
        assertTrue(serverTransport.isConnected())

        // Client sends packet to server
        val packetFromClient = MeshPacket("client_to_server", "client", "server", "hello from client", PacketType.TEXT, encrypted = false)
        clientTransport.sendPacket(packetFromClient)
        eventually { receivedPacketByServer != null }

        assertEquals("hello from client", receivedPacketByServer?.payload)
        assertEquals("client_to_server", receivedPacketByServer?.packetId)

        // Server sends packet to client
        val packetFromServer = MeshPacket("server_to_client", "server", "client", "hello from server", PacketType.TEXT, encrypted = false)
        serverTransport.sendPacket(packetFromServer)
        eventually { receivedPacketByClient != null }

        assertEquals("hello from server", receivedPacketByClient?.payload)
        assertEquals("server_to_client", receivedPacketByClient?.packetId)

        clientTransport.disconnect()
        serverTransport.stopServer()
    }

    @Test
    fun `disconnect cleans up streams and socket state`() = runBlocking {
        transport.startServer()
        
        val client = createTransport()
        client.connectAsClient("127.0.0.1")
        eventually { client.isConnected() }
        
        assertTrue(client.isConnected())
        client.disconnect()
        eventually { !client.isConnected() }
        
        assertFalse(client.isConnected())
        
        transport.stopServer()
    }
    
    @Test
    fun `massive packet serialization handles large payloads over TCP`() = runBlocking {
        val serverTransport = createTransport()
        val clientTransport = createTransport()

        var receivedPacket: MeshPacket? = null
        serverTransport.onPacketReceived = { receivedPacket = it }

        serverTransport.startServer()
        clientTransport.connectAsClient("127.0.0.1")
        eventually { clientTransport.isConnected() && serverTransport.isConnected() }

        // Create a 1MB String payload
        val massivePayload = "A".repeat(1024 * 1024)
        val packet = MeshPacket("massive", "client", "server", massivePayload, PacketType.MEDIA_CHUNK, encrypted = false)
        
        clientTransport.sendPacket(packet)
        eventually(timeoutMs = 5000L) { receivedPacket != null }
        
        assertNotNull(receivedPacket)
        assertEquals(massivePayload.length, receivedPacket?.payload?.length)
        
        clientTransport.disconnect()
        serverTransport.stopServer()
    }

    @Test
    fun `frame and chunk length validators reject negative, zero, and oversized lengths before allocation (ML-AUDIT-018)`() {
        assertFalse(WifiSocketTransport.isValidFrameLength(-1))
        assertFalse(WifiSocketTransport.isValidFrameLength(Int.MIN_VALUE))
        assertFalse(WifiSocketTransport.isValidFrameLength(0))
        assertTrue(WifiSocketTransport.isValidFrameLength(1))
        assertTrue(WifiSocketTransport.isValidFrameLength(WifiSocketTransport.MAX_FRAME_SIZE_BYTES))
        assertFalse(WifiSocketTransport.isValidFrameLength(WifiSocketTransport.MAX_FRAME_SIZE_BYTES + 1))
        assertFalse(WifiSocketTransport.isValidFrameLength(Int.MAX_VALUE))

        assertFalse(WifiSocketTransport.isValidEncryptedChunkLength(-100))
        assertFalse(WifiSocketTransport.isValidEncryptedChunkLength(0))
        assertTrue(WifiSocketTransport.isValidEncryptedChunkLength(1024))
        assertTrue(WifiSocketTransport.isValidEncryptedChunkLength(WifiSocketTransport.MAX_ENCRYPTED_CHUNK_BYTES))
        assertFalse(WifiSocketTransport.isValidEncryptedChunkLength(WifiSocketTransport.MAX_ENCRYPTED_CHUNK_BYTES + 1))
    }

    @Test
    fun `socket server safely closes connection when peer sends oversized or negative frame length without OOM (ML-AUDIT-018)`() = runBlocking {
        val serverTransport = createTransport()
        var receivedAny = false
        serverTransport.onPacketReceived = { receivedAny = true }
        serverTransport.startServer()

        // Connect raw socket and send an oversized length (Int.MAX_VALUE)
        java.net.Socket("127.0.0.1", com.meshlink.config.WifiConfig.DEFAULT_PORT).use { rawSocket ->
            eventually { serverTransport.isConnected() }
            val out = java.io.DataOutputStream(rawSocket.getOutputStream())
            out.writeInt(WifiSocketTransport.MAX_FRAME_SIZE_BYTES + 1024)
            out.flush()
            eventually { !serverTransport.isConnected() }
        }
        assertFalse(receivedAny)

        // Connect raw socket and send a negative unknown frame length
        java.net.Socket("127.0.0.1", com.meshlink.config.WifiConfig.DEFAULT_PORT).use { rawSocket ->
            eventually { serverTransport.isConnected() }
            val out = java.io.DataOutputStream(rawSocket.getOutputStream())
            out.writeInt(-42)
            out.flush()
            eventually { !serverTransport.isConnected() }
        }
        assertFalse(receivedAny)

        serverTransport.stopServer()
    }
}
