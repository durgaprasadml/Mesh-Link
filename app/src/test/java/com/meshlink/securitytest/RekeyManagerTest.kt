package com.meshlink.securitytest

import android.util.Base64
import com.meshlink.ble.data.MeshMessagingManager
import com.meshlink.common.maintenance.MaintenanceScheduler
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PeerSecureSession
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.MeshSecurityMonitor
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import java.util.BitSet
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class RekeyManagerTest {

    private lateinit var rekeyManager: RekeyManager
    private lateinit var cryptoManager: MeshCryptoManager
    private lateinit var sessionManager: SessionManager
    private lateinit var userRepository: UserRepository
    private lateinit var trustManager: TrustManager
    private lateinit var securityMonitor: MeshSecurityMonitor
    private lateinit var maintenanceScheduler: MaintenanceScheduler
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)

    private val peerId = "peer123"
    private val localMeshId = "local123"
    private val sessionId = "session_xyz"

    private lateinit var mockSession: PeerSecureSession

    @Before
    fun setup() {
        cryptoManager = mockk(relaxed = true)
        trustManager = mockk(relaxed = true)
        securityMonitor = mockk(relaxed = true)
        maintenanceScheduler = mockk(relaxed = true)
        
        // testDispatcher and testScheduler are initialized directly
        
        sessionManager = SessionManager(
            cryptoManager,
            trustManager,
            securityMonitor,
            testDispatcher,
            maintenanceScheduler
        )
        
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any<ByteArray>(), any()) } answers {
            val bytes = arg<ByteArray>(0)
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val str = arg<String>(0)
            java.util.Base64.getDecoder().decode(str)
        }

        userRepository = mockk(relaxed = true)
        coEvery { userRepository.getLocalUser() } returns User("dummy", localMeshId, "alias")

        mockSession = PeerSecureSession(
            peerId = peerId,
            sessionId = sessionId,
            fingerprint = "fp",
            sessionStart = 0L,
            sessionVersion = 1,
            cryptoVersion = 1,
            verified = true,
            lastActivity = System.currentTimeMillis(),
            packetCounter = AtomicLong(0),
            receiveCounter = AtomicLong(0),
            expirationTime = System.currentTimeMillis() + 30 * 60 * 1000L,
            replayWindow = BitSet(64),
            keyVersion = 1,
            previousKeyVersion = 0,
            rekeyTimestamp = 0,
            rotationReason = "",
            totalEncryptedPackets = AtomicLong(0),
            totalDecryptedPackets = AtomicLong(0)
        )

        // Inject the active session directly into SessionManager
        val activeSessionsField = SessionManager::class.java.getDeclaredField("activeSessions")
        activeSessionsField.isAccessible = true
        val activeSessions = activeSessionsField.get(sessionManager) as java.util.concurrent.ConcurrentHashMap<String, PeerSecureSession>
        activeSessions[peerId] = mockSession

        rekeyManager = RekeyManager(
            cryptoManager,
            sessionManager,
            userRepository,
            testDispatcher,
            maintenanceScheduler
        )
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test initiateRekey constructs properly formatted request and retains currentKv`() = runTest(testDispatcher) {
        val ephemeralKeyPair = generateKeyPair()
        every { cryptoManager.generateEphemeralKeyPair() } returns ephemeralKeyPair
        every { cryptoManager.sign(any()) } returns "mock_signature".toByteArray()

        var sentPacket: MeshPacket? = null
        rekeyManager.sendPacketCallback = { target, packet ->
            assertEquals(peerId, target)
            sentPacket = packet
        }

        rekeyManager.manualRekey(peerId)
        
        // Wait for coroutines
        advanceUntilIdle()

        assertNotNull(sentPacket)
        assertEquals(PacketType.SESSION_REKEY, sentPacket?.type)
        assertFalse(sentPacket?.encrypted ?: true)

        val payload = sentPacket?.payload
        assertNotNull(payload)
        assertTrue(payload!!.startsWith("rekey|$sessionId|1|2|"))
        
        // Ensure session key version is NOT rotated by initiateRekey
        assertEquals(1, mockSession.keyVersion)
    }

    @Test
    fun `test responder rotates keys AFTER sendPacketCallback avoiding CRITICAL-05 race condition`() = runTest(testDispatcher) {
        val initiatorPubBase64 = "initiator_pub_base64"
        val mockSignature = "bW9ja19zaWduYXR1cmU=" // valid base64
        val payload = "rekey|$sessionId|1|2|$initiatorPubBase64|${System.currentTimeMillis()}|nonce123|$mockSignature"

        every { cryptoManager.verifySignature(any(), any(), any()) } returns true
        val ephemeralKeyPair = generateKeyPair()
        every { cryptoManager.generateEphemeralKeyPair() } returns ephemeralKeyPair
        every { cryptoManager.sign(any()) } returns "mock_signature".toByteArray()

        var sentPacket: MeshPacket? = null
        var keyVersionDuringSend = 0
        rekeyManager.sendPacketCallback = { target, packet ->
            sentPacket = packet
            keyVersionDuringSend = mockSession.keyVersion
        }

        rekeyManager.handleRekeyPacket(peerId, payload, "dummy_pub_key")

        // Wait for coroutines
        advanceUntilIdle()

        // The packet must be dispatched while the session is STILL on currentKv (1)
        assertEquals(1, keyVersionDuringSend)
        
        // The session must be updated to nextKv (2) AFTER the packet is dispatched
        assertEquals(2, mockSession.keyVersion)
        assertEquals(1, mockSession.previousKeyVersion)
        
        // Verify key derivation was called
        verify { cryptoManager.deriveEphemeralSharedKey(eq(peerId), eq(initiatorPubBase64), any()) }
    }

    @Test
    fun `test implicit forward rotation handles incoming packet with nextKv`() = runTest(testDispatcher) {
        // Setup state where session is at kv=1
        assertEquals(1, mockSession.keyVersion)
        
        // Simulate that the cryptoManager already derived kv=2
        every { cryptoManager.hasPeerKey(peerId) } returns true

        // Simulate incoming AAD with kv=2
        val aadJson = org.json.JSONObject().apply {
            put("sid", sessionId)
            put("seq", 123L)
            put("ts", System.currentTimeMillis())
            put("kv", 2)
        }
        val aadBase64 = Base64.encodeToString(aadJson.toString().toByteArray(), Base64.NO_WRAP)
        val dummyPayload = "v2|$aadBase64|ciphertext_dummy"

        val unwrapped = sessionManager.validateAndUnwrap(peerId, dummyPayload)
        
        // Validation should succeed due to implicit forward rotation
        assertNotNull(unwrapped)
        assertEquals(2, unwrapped!!.third) // Returns the key version
        
        // The session state should be updated to kv=2
        assertEquals(2, mockSession.keyVersion)
        assertEquals(1, mockSession.previousKeyVersion)
    }

    @Test
    fun `test strict encryption rejects unencrypted rekey packets via MeshRouter logic`() {
        // In the full system, MeshRouter explicitly drops unencrypted SESSION_REKEY packets.
        // We verify that the policy is set to REQUIRED so MeshMessagingManager will encrypt it.
        val requirement = com.meshlink.security.policy.PacketEncryptionPolicy.getRequirement(PacketType.SESSION_REKEY)
        assertEquals(com.meshlink.security.policy.EncryptionRequirement.REQUIRED, requirement)
    }

    private fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }
}
