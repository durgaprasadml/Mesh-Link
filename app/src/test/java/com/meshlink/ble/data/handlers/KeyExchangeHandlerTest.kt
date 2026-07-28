package com.meshlink.ble.data.handlers

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.ble.data.BleConnectionManager
import com.meshlink.ble.data.RoutingCoordinator
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.KeyExchangeReplayCache
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.MeshSecurityMonitor
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import io.mockk.every

class KeyExchangeHandlerTest {

    private lateinit var cryptoManager: MeshCryptoManager
    private lateinit var sessionManager: SessionManager
    private lateinit var trustManager: TrustManager
    private lateinit var keyExchangeReplayCache: KeyExchangeReplayCache
    private lateinit var securityMonitor: MeshSecurityMonitor
    private lateinit var routingCoordinator: RoutingCoordinator
    private lateinit var connectionManager: BleConnectionManager
    private lateinit var userRepository: UserRepository
    private lateinit var packetDispatcher: PacketDispatcher
    private lateinit var applicationScope: CoroutineScope
    private lateinit var keyExchangeHandler: KeyExchangeHandler

    @Before
    fun setUp() {
        cryptoManager = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        trustManager = mockk(relaxed = true)
        keyExchangeReplayCache = mockk(relaxed = true)
        securityMonitor = mockk(relaxed = true)
        routingCoordinator = mockk(relaxed = true)
        connectionManager = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        packetDispatcher = mockk(relaxed = true)
        applicationScope = mockk(relaxed = true)

        keyExchangeHandler = KeyExchangeHandler(
            cryptoManager,
            sessionManager,
            trustManager,
            keyExchangeReplayCache,
            securityMonitor,
            routingCoordinator,
            connectionManager,
            userRepository,
            packetDispatcher,
            applicationScope
        )
    }

    @Test
    fun `generateSignedKeyExchange returns valid MeshPacket with expected payload format`() {
        every { cryptoManager.getOrCreatePublicKey() } returns "base64PublicKeyString="
        every { cryptoManager.getOrCreateSigningKey() } returns "base64SigningKeyString="
        every { cryptoManager.sign(any()) } returns ByteArray(64)

        val packet = keyExchangeHandler.generateSignedKeyExchange("local-peer-id", isResponse = false)

        assertNotNull(packet)
        assertEquals("local-peer-id", packet.senderId)
        assertEquals(PacketType.KEY_EXCHANGE, packet.type)
        assertTrue(packet.payload.startsWith("v2|"))
    }
}
