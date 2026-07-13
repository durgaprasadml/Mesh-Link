package com.meshlink.security.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.ProviderException
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Handles derivation and protection of the SQLCipher database passphrase using Android Keystore.
 * Implements backward-compatible migration via PRAGMA rekey, PBKDF2 key derivation,
 * memory hygiene, and robust crash safety.
 */
@Singleton
class DatabaseSecurityManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DbSecurity"
    }

    private val encPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SecurityConstants.DB_PREFS_NAME_ENC,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Retrieves the highly secure, derived SQLCipher passphrase.
     * Automatically migrates from the legacy UUID passphrase if one exists.
     * 
     * Returns a ByteArray containing the UTF-8 bytes of a Base64 string,
     * which can be directly passed to SQLCipher SupportOpenHelperFactory.
     */
    @Throws(SecurityRecoveryException::class)
    fun getDatabasePassphrase(): ByteArray {
        val legacyPrefs = context.getSharedPreferences(SecurityConstants.DB_PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
        
        // 1. Ensure we have our secure, Keystore-backed passphrase derived via PBKDF2
        // We receive the raw ByteArray and format it only inside migration or return it
        val securePassphraseString = getOrGenerateSecurePassphraseString()
        
        // 2. Check for migration completeness
        val isMigrated = encPrefs.getBoolean(SecurityConstants.KEY_MIGRATION_COMPLETE, false)
        if (!isMigrated) {
            val legacyPassphrase = legacyPrefs.getString(SecurityConstants.KEY_LEGACY_PASSPHRASE, null)
            if (legacyPassphrase != null) {
                migrateDatabaseIfNeeded(legacyPassphrase, securePassphraseString)
                legacyPrefs.edit().remove(SecurityConstants.KEY_LEGACY_PASSPHRASE).apply()
                // Set flag to avoid future migration checks
                encPrefs.edit().putBoolean(SecurityConstants.KEY_MIGRATION_COMPLETE, true).apply()
            } else {
                // If there's no legacy passphrase, it's a fresh install or already migrated previously
                encPrefs.edit().putBoolean(SecurityConstants.KEY_MIGRATION_COMPLETE, true).apply()
            }
        }
        
        val passBytes = securePassphraseString.toByteArray(Charsets.UTF_8)
        
        // Wipe String? We can't in JVM, but we avoided holding raw arrays longer than needed.
        return passBytes
    }

    @Throws(SecurityRecoveryException::class)
    private fun getOrGenerateSecurePassphraseString(): String {
        val encryptedSeedBase64 = encPrefs.getString(SecurityConstants.KEY_ENCRYPTED_SEED, null)
        val saltBase64 = encPrefs.getString(SecurityConstants.KEY_SALT, null)

        val seed: ByteArray
        val salt: ByteArray

        if (encryptedSeedBase64 == null || saltBase64 == null) {
            // First time setup (or migration)
            seed = ByteArray(SecurityConstants.SEED_LENGTH_BYTES).apply { SecureRandom().nextBytes(this) }
            salt = ByteArray(SecurityConstants.SALT_LENGTH_BYTES).apply { SecureRandom().nextBytes(this) }

            val encryptedSeed = encryptWithKeystore(seed)

            encPrefs.edit()
                .putString(SecurityConstants.KEY_ENCRYPTED_SEED, Base64.encodeToString(encryptedSeed, Base64.NO_WRAP))
                .putString(SecurityConstants.KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .apply()
        } else {
            // Decrypt existing
            salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val encryptedSeed = Base64.decode(encryptedSeedBase64, Base64.NO_WRAP)
            
            val decryptedSeed = decryptWithKeystore(encryptedSeed)
            if (decryptedSeed.isEmpty()) {
                throw SecurityRecoveryException("Database seed cannot be recovered. Aborting to prevent data destruction.")
            }
            seed = decryptedSeed
        }

        // Derive via PBKDF2
        val factory = SecretKeyFactory.getInstance(SecurityConstants.PBKDF2_ALGORITHM)
        val seedChars = Base64.encodeToString(seed, Base64.NO_WRAP).toCharArray()
        
        val spec = PBEKeySpec(seedChars, salt, com.meshlink.config.SecurityConfig.PBKDF2_ITERATIONS, SecurityConstants.PBKDF2_KEY_LENGTH_BITS)
        val secret = factory.generateSecret(spec)
        val passphraseBytes = secret.encoded
        
        val finalPassphraseStr = Base64.encodeToString(passphraseBytes, Base64.NO_WRAP)

        // Memory hygiene
        Arrays.fill(seed, 0.toByte())
        Arrays.fill(seedChars, '\u0000')
        Arrays.fill(passphraseBytes, 0.toByte())
        spec.clearPassword()

        return finalPassphraseStr
    }

    /**
     * Executes PRAGMA rekey on the existing database using the legacy passphrase, securely wrapped in a transaction.
     */
    private fun migrateDatabaseIfNeeded(legacyPassphrase: String, newPassphraseStr: String) {
        val dbFile = context.getDatabasePath(SecurityConstants.DB_NAME)
        if (!dbFile.exists()) return

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                dbFile.path,
                legacyPassphrase,
                null as SQLiteDatabase.CursorFactory?,
                SQLiteDatabase.OPEN_READWRITE,
                null
            )
            
            db.execSQL("BEGIN IMMEDIATE;")
            try {
                // PRAGMA rekey changes the encryption key of the database on the fly
                db.execSQL("PRAGMA rekey = '$newPassphraseStr';")
                db.execSQL("COMMIT;")
            } catch (innerE: Exception) {
                db.execSQL("ROLLBACK;")
                throw innerE
            }
        } catch (e: Exception) {
            val sanitizedMsg = e.javaClass.simpleName
            FirebaseCrashlytics.getInstance().recordException(Exception("Database migration failed: $sanitizedMsg"))
        } finally {
            db?.close()
        }
    }

    // ────────── Keystore Wrapper ──────────

    private fun getKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE).apply { load(null) }
        
        if (!keyStore.containsAlias(SecurityConstants.DB_MASTER_KEY_ALIAS)) {
            generateKeystoreKeyWithStrongBoxFallback()
        }
        return keyStore.getKey(SecurityConstants.DB_MASTER_KEY_ALIAS, null) as SecretKey
    }

    private fun generateKeystoreKeyWithStrongBoxFallback() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, SecurityConstants.ANDROID_KEYSTORE)
        val specBuilder = KeyGenParameterSpec.Builder(SecurityConstants.DB_MASTER_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(SecurityConstants.AES_KEY_SIZE_BITS)
            
        // Attempt StrongBox first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                keyGenerator.init(specBuilder.setIsStrongBoxBacked(true).build())
                keyGenerator.generateKey()
                return
            } catch (e: StrongBoxUnavailableException) {
                // Ignore, fallback below
            } catch (e: ProviderException) {
                // Ignore, fallback below
            } catch (e: InvalidAlgorithmParameterException) {
                // Ignore, fallback below
            }
        }

        // Fallback to normal Keystore
        keyGenerator.init(specBuilder.setIsStrongBoxBacked(false).build())
        keyGenerator.generateKey()
    }

    private fun encryptWithKeystore(plaintext: ByteArray): ByteArray {
        var attempt = 0
        while (attempt < 3) {
            try {
                val key = getKeystoreKey()
                val cipher = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                
                val iv = cipher.iv
                val ciphertext = cipher.doFinal(plaintext)
                
                val result = ByteArray(iv.size + ciphertext.size)
                System.arraycopy(iv, 0, result, 0, iv.size)
                System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)
                return result
            } catch (e: Exception) {
                attempt++
                if (attempt >= 3) {
                    FirebaseCrashlytics.getInstance().recordException(Exception("Keystore DB encrypt failed after 3 retries", e))
                    throw e
                }
            }
        }
        return ByteArray(0)
    }

    private fun decryptWithKeystore(ciphertext: ByteArray): ByteArray {
        var attempt = 0
        while (attempt < 3) {
            try {
                val key = getKeystoreKey()
                val cipher = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
                
                val iv = ciphertext.copyOfRange(0, SecurityConstants.GCM_IV_LENGTH_BYTES)
                val encrypted = ciphertext.copyOfRange(SecurityConstants.GCM_IV_LENGTH_BYTES, ciphertext.size)
                
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv))
                
                return cipher.doFinal(encrypted)
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                FirebaseCrashlytics.getInstance().recordException(Exception("Keystore DB key permanently invalidated", e))
                // Regenerate the master key but do NOT delete the DB or return dummy byte array
                try {
                    val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE).apply { load(null) }
                    keyStore.deleteEntry(SecurityConstants.DB_MASTER_KEY_ALIAS)
                    generateKeystoreKeyWithStrongBoxFallback()
                } catch (ignore: Exception) {}
                // Bubble up failure to avoid destructive Room fallback
                throw SecurityRecoveryException("Keystore DB key permanently invalidated", e)
            } catch (e: Exception) {
                attempt++
                if (attempt >= 3) {
                    val msg = e.javaClass.simpleName
                    FirebaseCrashlytics.getInstance().recordException(Exception("Keystore DB decrypt failed after 3 retries: $msg"))
                    return ByteArray(0)
                }
            }
        }
        return ByteArray(0)
    }
}
