package com.meshlink.security.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import io.mockk.mockkStatic
import io.mockk.every
import io.mockk.mockk
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.junit.Ignore

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Ignore("Robolectric does not fully support AndroidKeyStore AES/GCM")
class DatabaseSecurityManagerTest {

    private lateinit var context: Context
    private lateinit var databaseSecurityManager: DatabaseSecurityManager

    @Before
    fun setup() {
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        context = ApplicationProvider.getApplicationContext()
        databaseSecurityManager = DatabaseSecurityManager(context)
    }

    @Test
    fun `getDatabasePassphrase returns secure passphrase bytes`() {
        // Initial setup
        val passphraseBytes = databaseSecurityManager.getDatabasePassphrase()
        assertNotNull(passphraseBytes)
        assertTrue(passphraseBytes.isNotEmpty())
        
        // Calling it again should yield the same derived bytes (idempotency)
        val passphraseBytes2 = databaseSecurityManager.getDatabasePassphrase()
        assertTrue(passphraseBytes.contentEquals(passphraseBytes2))
    }

    @Test
    fun `migration from legacy UUID passphrase executes without crash`() {
        // Simulate a legacy UUID passphrase in SharedPreferences
        val legacyPrefs = context.getSharedPreferences(SecurityConstants.DB_PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
        legacyPrefs.edit().putString(SecurityConstants.KEY_LEGACY_PASSPHRASE, UUID.randomUUID().toString()).commit()

        // getDatabasePassphrase should detect the legacy key, attempt to migrate the DB (which won't exist in test, so it ignores or catches safely)
        // and clears the legacy pref.
        val passphraseBytes = databaseSecurityManager.getDatabasePassphrase()
        assertNotNull(passphraseBytes)
        assertTrue(passphraseBytes.isNotEmpty())
        
        // Verify migration completion flag was set and legacy passphrase removed
        val stillHasLegacy = legacyPrefs.contains(SecurityConstants.KEY_LEGACY_PASSPHRASE)
        assertTrue(!stillHasLegacy)
    }
}
