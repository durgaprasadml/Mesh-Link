package com.meshlink.data.crypto

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
}
