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
            maintenanceScheduler,
            applicationScope = kotlinx.coroutines.test.TestScope(testDispatcher)
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
            maintenanceScheduler,
            applicationScope = kotlinx.coroutines.test.TestScope(testDispatcher)
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
        
        // Pre-authentication validation succeeds and returns the candidate keyVersion,
        // but MUST NOT mutate session state before AES-GCM authentication succeeds (ML-AUDIT-002)
        assertNotNull(unwrapped)
        assertEquals(2, unwrapped!!.third) // Returns the key version
        assertEquals(1, mockSession.keyVersion)
        assertEquals(0, mockSession.previousKeyVersion)
        assertEquals(0L, mockSession.receiveCounter.get())

        // Once decryption/authentication succeeds, commitAuthenticatedPacket advances the keyVersion and sequence window
        sessionManager.commitAuthenticatedPacket(peerId, unwrapped.first)
        assertEquals(2, mockSession.keyVersion)
        assertEquals(1, mockSession.previousKeyVersion)
        assertEquals(123L, mockSession.receiveCounter.get())
    }

    @Test
    fun `test forged sid is rejected and does not overwrite active sessionId (ML-AUDIT-002)`() = runTest(testDispatcher) {
        assertEquals(sessionId, mockSession.sessionId)

        val forgedAadJson = org.json.JSONObject().apply {
            put("sid", "forged_attacker_sid")
            put("seq", 1L)
            put("ts", System.currentTimeMillis())
            put("kv", 1)
        }
        val aadBase64 = Base64.encodeToString(forgedAadJson.toString().toByteArray(), Base64.NO_WRAP)
        val forgedPayload = "v2|$aadBase64|forged_ciphertext"

        val unwrapped = sessionManager.validateAndUnwrap(peerId, forgedPayload)
        assertNull(unwrapped)

        // Active session ID must remain intact
        assertEquals(sessionId, sessionManager.getSession(peerId)?.sessionId)
        assertEquals(0L, mockSession.receiveCounter.get())
    }

    @Test
    fun `test forged kv=current+1 with invalid ciphertext does not advance keyVersion or replay window (ML-AUDIT-002)`() = runTest(testDispatcher) {
        every { cryptoManager.hasPeerKey(peerId) } returns true
        assertEquals(1, mockSession.keyVersion)
        assertEquals(0, mockSession.previousKeyVersion)

        val forgedAadJson = org.json.JSONObject().apply {
            put("sid", sessionId)
            put("seq", 10L)
            put("ts", System.currentTimeMillis())
            put("kv", 2)
        }
        val aadBase64 = Base64.encodeToString(forgedAadJson.toString().toByteArray(), Base64.NO_WRAP)
        val invalidCiphertextPayload = "v2|$aadBase64|tampered_ciphertext"

        // Step 1: validateAndUnwrap parses metadata prior to decryption
        val unwrapped = sessionManager.validateAndUnwrap(peerId, invalidCiphertextPayload)
        assertNotNull(unwrapped)

        // Step 2: Decryption fails (returns null), so commitAuthenticatedPacket is NEVER called.
        // Verify keyVersion, previousKeyVersion, and replay window were NOT mutated!
        assertEquals(1, mockSession.keyVersion)
        assertEquals(0, mockSession.previousKeyVersion)
        assertEquals(0L, mockSession.receiveCounter.get())
        assertFalse(mockSession.isReplay(10L))
        assertEquals(0L, mockSession.totalDecryptedPackets.get())
    }

    @Test
    fun `test out-of-order packets within valid 64-packet window succeed while replays are rejected (ML-AUDIT-002)`() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        fun buildPayload(seq: Long): String {
            val aadJson = org.json.JSONObject().apply {
                put("sid", sessionId)
                put("seq", seq)
                put("ts", now)
                put("kv", 1)
            }
            val aadBase64 = Base64.encodeToString(aadJson.toString().toByteArray(), Base64.NO_WRAP)
            return "v2|$aadBase64|valid_ciphertext_$seq"
        }

        // Receive packet seq = 10 first
        val pkt10 = sessionManager.validateAndUnwrap(peerId, buildPayload(10L))
        assertNotNull(pkt10)
        sessionManager.commitAuthenticatedPacket(peerId, pkt10!!.first)
        assertEquals(10L, mockSession.receiveCounter.get())

        // Receive out-of-order packet seq = 5 (within 64-packet window) -> must succeed
        val pkt5 = sessionManager.validateAndUnwrap(peerId, buildPayload(5L))
        assertNotNull(pkt5)
        sessionManager.commitAuthenticatedPacket(peerId, pkt5!!.first)
        assertEquals(10L, mockSession.receiveCounter.get())

        // Replay of seq = 5 -> must be rejected
        assertNull(sessionManager.validateAndUnwrap(peerId, buildPayload(5L)))

        // Replay of seq = 10 -> must be rejected
        assertNull(sessionManager.validateAndUnwrap(peerId, buildPayload(10L)))

        // Out-of-order packet seq = 7 -> must still succeed
        val pkt7 = sessionManager.validateAndUnwrap(peerId, buildPayload(7L))
        assertNotNull(pkt7)
        sessionManager.commitAuthenticatedPacket(peerId, pkt7!!.first)
        assertEquals(3L, mockSession.totalDecryptedPackets.get())
    }

    @Test
    fun `test strict encryption rejects unencrypted rekey packets via MeshRouter logic`() {
        // SESSION_REKEY is OPTIONAL in non-strict mode to permit key rollover bootstrapping,
        // but strict mode explicitly rejects unencrypted SESSION_REKEY packets.
        val requirement = com.meshlink.security.policy.PacketEncryptionPolicy.getRequirement(PacketType.SESSION_REKEY)
        assertEquals(com.meshlink.security.policy.EncryptionRequirement.OPTIONAL, requirement)

        val unencryptedRekeyPacket = com.meshlink.domain.model.MeshPacket(
            packetId = "rekey_1",
            senderId = "peer",
            targetId = "local",
            payload = "rekey|s1|1|2|pub|ts|nonce|sig",
            type = PacketType.SESSION_REKEY,
            encrypted = false
        )
        val validInStrictMode = com.meshlink.security.policy.PacketEncryptionPolicy.validatePacketEncryption(
            unencryptedRekeyPacket,
            strictMode = true,
            hasSecureSession = false
        )
        assertFalse(validInStrictMode)
    }

    private fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }
}
