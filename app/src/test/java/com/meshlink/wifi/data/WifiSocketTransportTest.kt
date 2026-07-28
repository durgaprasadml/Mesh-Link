package com.meshlink.wifi.data

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
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

    private lateinit var transport: WifiSocketTransport

    @Before
    fun setup() {
        transport = WifiSocketTransport(applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
    }

    @After
    fun teardown() {
        transport.stopServer()
        transport.disconnect()
    }

    private suspend fun eventually(timeoutMs: Long = 5000L, pollMs: Long = 50L, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) {
                delay(pollMs)
            }
        }
    }

    @Test
    fun `startServer and connectAsClient establishes successful socket connection`() = runBlocking {
        val serverTransport = WifiSocketTransport(applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
        val clientTransport = WifiSocketTransport(applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))

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
        
        val client = WifiSocketTransport(applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
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
        val serverTransport = WifiSocketTransport(applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
        val clientTransport = WifiSocketTransport(applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))

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
}
