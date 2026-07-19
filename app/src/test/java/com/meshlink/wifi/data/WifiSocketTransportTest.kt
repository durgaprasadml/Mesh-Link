package com.meshlink.wifi.data

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
        transport = WifiSocketTransport()
    }

    @After
    fun teardown() {
        transport.stopServer()
        transport.disconnect()
    }

    @Test
    fun `startServer and connectAsClient establishes successful socket connection`() = runBlocking {
        val serverTransport = WifiSocketTransport()
        val clientTransport = WifiSocketTransport()

        var receivedPacketByServer: MeshPacket? = null
        var receivedPacketByClient: MeshPacket? = null

        serverTransport.onPacketReceived = { receivedPacketByServer = it }
        clientTransport.onPacketReceived = { receivedPacketByClient = it }

        // Start Server on 127.0.0.1 (Loopback)
        serverTransport.startServer()
        delay(500) // Give server time to bind

        // Client connects
        clientTransport.connectAsClient("127.0.0.1")
        delay(500) // Give sockets time to establish streams

        assertTrue(clientTransport.isConnected())
        // serverTransport.isConnected() reflects the activeSocket on the server side which is set when client connects
        assertTrue(serverTransport.isConnected())

        // Client sends packet to server
        val packetFromClient = MeshPacket("client_to_server", "client", "server", "hello from client", PacketType.TEXT, encrypted = false)
        clientTransport.sendPacket(packetFromClient)
        delay(200)

        assertEquals("hello from client", receivedPacketByServer?.payload)
        assertEquals("client_to_server", receivedPacketByServer?.packetId)

        // Server sends packet to client
        val packetFromServer = MeshPacket("server_to_client", "server", "client", "hello from server", PacketType.TEXT, encrypted = false)
        serverTransport.sendPacket(packetFromServer)
        delay(200)

        assertEquals("hello from server", receivedPacketByClient?.payload)
        assertEquals("server_to_client", receivedPacketByClient?.packetId)

        clientTransport.disconnect()
        serverTransport.stopServer()
    }

    @Test
    fun `disconnect cleans up streams and socket state`() = runBlocking {
        transport.startServer()
        delay(200)
        
        val client = WifiSocketTransport()
        client.connectAsClient("127.0.0.1")
        delay(200)
        
        assertTrue(client.isConnected())
        client.disconnect()
        
        assertFalse(client.isConnected())
        
        transport.stopServer()
    }
    
    @Test
    fun `massive packet serialization handles large payloads over TCP`() = runBlocking {
        val serverTransport = WifiSocketTransport()
        val clientTransport = WifiSocketTransport()

        var receivedPacket: MeshPacket? = null
        serverTransport.onPacketReceived = { receivedPacket = it }

        serverTransport.startServer()
        delay(500)
        clientTransport.connectAsClient("127.0.0.1")
        delay(500)

        // Create a 1MB String payload
        val massivePayload = "A".repeat(1024 * 1024)
        val packet = MeshPacket("massive", "client", "server", massivePayload, PacketType.MEDIA_CHUNK, encrypted = false)
        
        clientTransport.sendPacket(packet)
        
        // Wait longer for large payload to transit and parse
        delay(1000)
        
        assertNotNull(receivedPacket)
        assertEquals(massivePayload.length, receivedPacket?.payload?.length)
        
        clientTransport.disconnect()
        serverTransport.stopServer()
    }
}
