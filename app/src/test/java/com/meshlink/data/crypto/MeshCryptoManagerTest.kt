package com.meshlink.security.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MeshCryptoManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var cryptoManager: MeshCryptoManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPrefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        
        // Mock Android Base64 with standard java.util.Base64 for unit tests
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            val bytes = arg<ByteArray>(0)
            Base64.getEncoder().encodeToString(bytes)
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val str = arg<String>(0)
            Base64.getDecoder().decode(str)
        }
        
        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.i(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0

        // Mock FirebaseCrashlytics
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics

        // Mock EncryptedSharedPreferences to return our mocked sharedPrefs
        mockkStatic(androidx.security.crypto.EncryptedSharedPreferences::class)
        every { androidx.security.crypto.EncryptedSharedPreferences.create(any<Context>(), any<String>(), any<androidx.security.crypto.MasterKey>(), any(), any()) } returns sharedPrefs
        
        // Since MasterKey requires real Context, we mock it too
        mockkConstructor(androidx.security.crypto.MasterKey.Builder::class)
        every { anyConstructed<androidx.security.crypto.MasterKey.Builder>().setKeyScheme(any()) } answers { callOriginal() }
        every { anyConstructed<androidx.security.crypto.MasterKey.Builder>().build() } returns mockk(relaxed = true)

        cryptoManager = MeshCryptoManager(context)
        
        // Generate software keys for testing
        // Mock shared preferences to return null so it generates new ones
        every { sharedPrefs.getString("__self_private_key__", null) } returns null
        every { sharedPrefs.getString("__self_public_key__", null) } returns null
        every { sharedPrefs.getString("__self_signing_private_key__", null) } returns null
        every { sharedPrefs.getString("__self_signing_public_key__", null) } returns null
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test encryptOrPassthrough respects requireEncryption flag`() {
        // When requireEncryption = false, it should return the original payload
        val originalPayload = "plain text"
        val targetPeerId = "peer1"
        
        val result = cryptoManager.encryptOrPassthrough(
            plaintext = originalPayload,
            peerId = targetPeerId,
            requireEncryption = false
        )
        assertNotNull(result)
        val (encryptedText, wasEncrypted) = result!!
        
        assertEquals(originalPayload, encryptedText)
        assertFalse(wasEncrypted)
    }

    @Test
    fun `test sign and verifySignature`() {
        // Mock that the signing key is stored and returned
        val privBase64Captor = slot<String>()
        every { editor.putString("__self_signing_private_key__", capture(privBase64Captor)) } returns editor
        every { sharedPrefs.getString("__self_signing_private_key__", null) } answers { 
            if (privBase64Captor.isCaptured) privBase64Captor.captured else null 
        }

        val pubKeyBase64 = cryptoManager.getOrCreateSigningKey()

        val dataToSign = "Test payload 123".toByteArray(Charsets.UTF_8)
        val signature = cryptoManager.sign(dataToSign)
        
        val isValid = cryptoManager.verifySignature(pubKeyBase64, dataToSign, signature)
        assertTrue("Signature should be valid", isValid)
    }

    @Test
    fun `test invalid signature rejection`() {
        // Mock that the signing key is stored and returned
        val privBase64Captor = slot<String>()
        every { editor.putString("__self_signing_private_key__", capture(privBase64Captor)) } returns editor
        every { sharedPrefs.getString("__self_signing_private_key__", null) } answers { 
            if (privBase64Captor.isCaptured) privBase64Captor.captured else null 
        }

        val pubKeyBase64 = cryptoManager.getOrCreateSigningKey()

        val dataToSign = "Test payload 123".toByteArray(Charsets.UTF_8)
        val signature = cryptoManager.sign(dataToSign)
        
        // Modify data
        val tamperedData = "Test payload 124".toByteArray(Charsets.UTF_8)
        val isValid = cryptoManager.verifySignature(pubKeyBase64, tamperedData, signature)
        assertFalse("Tampered data signature should be invalid", isValid)
        
        // Modify signature
        signature[0] = signature[0].inc()
        val isSigValid = cryptoManager.verifySignature(pubKeyBase64, dataToSign, signature)
        assertFalse("Tampered signature should be invalid", isSigValid)
    }

    @Test
    fun `test fingerprint generation`() {
        val pubKeyBase64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE+A" // valid base64
        val fingerprint = cryptoManager.getDeviceFingerprint(pubKeyBase64)
        
        assertNotNull(fingerprint)
        assertTrue("Fingerprint should contain colons", fingerprint.contains(":"))
    }
}
