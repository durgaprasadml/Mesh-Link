package com.meshlink.ble.data.handlers

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.KeyExchangeReplayCache
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class KeyExchangeHandlerTest {

    private lateinit var userRepository: UserRepository
    private lateinit var cryptoManager: MeshCryptoManager
    private lateinit var sessionManager: SessionManager
    private lateinit var keyExchangeReplayCache: KeyExchangeReplayCache
    private lateinit var packetDispatcher: PacketDispatcher
    private lateinit var keyExchangeHandler: KeyExchangeHandler

    @Before
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        cryptoManager = mock(MeshCryptoManager::class.java)
        sessionManager = mock(SessionManager::class.java)
        keyExchangeReplayCache = mock(KeyExchangeReplayCache::class.java)
        packetDispatcher = mock(PacketDispatcher::class.java)

        keyExchangeHandler = KeyExchangeHandler(
            userRepository,
            cryptoManager,
            sessionManager,
            keyExchangeReplayCache,
            packetDispatcher
        )
    }

    @Test
    fun `generateSignedKeyExchange returns valid MeshPacket with expected payload format`() {
        `when`(cryptoManager.generatePublicKeyPair()).thenReturn(Pair(ByteArray(32), ByteArray(32)))
        `when`(cryptoManager.signData(any())).thenReturn(ByteArray(64))

        val packet = keyExchangeHandler.generateSignedKeyExchange("local-peer-id", isResponse = false)

        assertNotNull(packet)
        assertEquals("local-peer-id", packet.senderId)
        assertEquals(PacketType.KEY_EXCHANGE, packet.type)
        assertTrue(packet.payload.startsWith("v2|"))
    }
}
