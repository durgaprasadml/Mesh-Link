package com.meshlink.data.crypto

import android.util.Base64
import com.meshlink.data.ble.PeerSecureSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.json.JSONObject
import com.meshlink.data.security.TrustManager
import com.meshlink.data.security.TrustLevel
import com.meshlink.data.security.MeshSecurityMonitor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private lateinit var cryptoManager: MeshCryptoManager
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            val bytes = it.invocation.args[0] as ByteArray
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
        every { Base64.decode(any<String>(), any()) } answers {
            val str = it.invocation.args[0] as String
            java.util.Base64.getDecoder().decode(str)
        }

        cryptoManager = mockk(relaxed = true)
        val trustManager = mockk<TrustManager>(relaxed = true)
        every { trustManager.getTrustLevel(any()) } returns TrustLevel.TRUSTED
        val securityMonitor = mockk<MeshSecurityMonitor>(relaxed = true)
        
        sessionManager = SessionManager(cryptoManager, trustManager, securityMonitor)
    }

    @Test
    fun testReplayProtection_acceptsInWindow() {
        val session = sessionManager.createSession("peer1", "fprint", 2, true)
        
        // Sequence 1 is fresh
        assertFalse(session.isReplay(1))
        session.markReceived(1)

        // Sequence 2 is fresh
        assertFalse(session.isReplay(2))
        session.markReceived(2)

        // Sequence 1 is now a replay
        assertTrue(session.isReplay(1))
    }

    @Test
    fun testReplayProtection_rejectsOldPackets() {
        val session = sessionManager.createSession("peer1", "fprint", 2, true)
        
        session.markReceived(100) // Advances window
        
        // Window is 64 packets. Packet 100 - 64 = 36. 
        // Anything <= 36 is rejected.
        assertTrue(session.isReplay(36))
        assertTrue(session.isReplay(35))
        
        // 37 is within window but not received yet
        assertFalse(session.isReplay(37))
    }

    @Test
    fun testGenerateAndValidateAad() {
        val peerId = "peerX"
        sessionManager.createSession(peerId, "finger", 2, true)

        val generated = sessionManager.generateAad(peerId)
        assertNotNull(generated)
        val (aadBytes, prefix) = generated!!

        assertTrue(prefix.startsWith("v2|"))

        // Simulate wrapping a payload
        val wrappedPayload = "${prefix}dummyCiphertext"

        // Validate
        val unwrapped = sessionManager.validateAndUnwrap(peerId, wrappedPayload)
        assertNotNull(unwrapped)
        val (unwrappedAadBytes, unwrappedCiphertext) = unwrapped!!
        
        assertArrayEquals(aadBytes, unwrappedAadBytes)
        assertEquals("dummyCiphertext", unwrappedCiphertext)
    }
}
