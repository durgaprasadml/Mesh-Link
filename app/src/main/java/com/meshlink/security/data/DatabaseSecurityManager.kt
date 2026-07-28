package com.meshlink.security.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.zetetic.database.sqlcipher.SQLiteDatabase

@Singleton
class DatabaseSecurityManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager
) {
    companion object {
        private const val TAG = "DbSecurity"
    }

    private val migrationMutex = Mutex()

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

    @Throws(SecurityRecoveryException::class)
    fun getDatabasePassphrase(): SecureDatabaseKey = runBlocking {
        migrationMutex.withLock {
            com.meshlink.common.logger.MeshLogger.d(TAG, "Initializing database passphrase derivation")
            val legacyPrefs = context.getSharedPreferences(SecurityConstants.DB_PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
            
            val secureKey = getOrGenerateSecurePassphrase()
            
            val migrationState = encPrefs.getString(SecurityConstants.KEY_MIGRATION_STATE, SecurityConstants.STATE_NOT_STARTED)
            com.meshlink.common.logger.MeshLogger.d(TAG, "Migration state = $migrationState")
            
            val dbFile = context.getDatabasePath(SecurityConstants.DB_NAME)
            
            if (migrationState != SecurityConstants.STATE_VERIFIED) {
                val legacyPassphraseStr = legacyPrefs.getString(SecurityConstants.KEY_LEGACY_PASSPHRASE, null)
                com.meshlink.common.logger.MeshLogger.d(TAG, "Legacy key detected = ${legacyPassphraseStr != null}")
                
                if (legacyPassphraseStr != null) {
                    val legacyPassBytes = legacyPassphraseStr.toByteArray(Charsets.UTF_8)
                    var legacyKey: SecureDatabaseKey? = null
                    
                    try {
                        legacyKey = SecureDatabaseKey(legacyPassBytes)
                        com.meshlink.common.logger.MeshLogger.d(TAG, "Changing migration state to IN_PROGRESS")
                        encPrefs.edit().putString(SecurityConstants.KEY_MIGRATION_STATE, SecurityConstants.STATE_IN_PROGRESS).apply()
                        
                        val result = migrateDatabaseIfNeeded(legacyKey, secureKey)
                        if (result is RekeyResult.Success || result is RekeyResult.MigrationNotRequired) {
                            com.meshlink.common.logger.MeshLogger.d(TAG, "Migration complete. Deleting legacy key.")
                            legacyPrefs.edit().remove(SecurityConstants.KEY_LEGACY_PASSPHRASE).apply()
                            encPrefs.edit().putString(SecurityConstants.KEY_MIGRATION_STATE, SecurityConstants.STATE_VERIFIED).apply()
                        } else {
                            com.meshlink.common.logger.MeshLogger.w(TAG, "Migration failed, falling back to legacy passphrase. State = FAILED.")
                            encPrefs.edit().putString(SecurityConstants.KEY_MIGRATION_STATE, SecurityConstants.STATE_FAILED).apply()
                            
                            // To fallback without returning destroyed key, we duplicate the legacy key to return
                            val fallbackBytes = Arrays.copyOf(legacyKey.getBytes(), legacyKey.getBytes().size)
                            secureKey.close() // Close the new key since we fallback
                            return@withLock SecureDatabaseKey(fallbackBytes)
                        }
                    } finally {
                        legacyKey?.close()
                        // Ensure original string bytes are wiped
                        Arrays.fill(legacyPassBytes, 0.toByte())
                    }
                } else {
                    com.meshlink.common.logger.MeshLogger.d(TAG, "No legacy key found. Marking VERIFIED.")
                    encPrefs.edit().putString(SecurityConstants.KEY_MIGRATION_STATE, SecurityConstants.STATE_VERIFIED).apply()
                }
            }
            
            return@withLock secureKey
        }
    }

    @Throws(SecurityRecoveryException::class)
    private fun getOrGenerateSecurePassphrase(): SecureDatabaseKey {
        val encryptedSeedBase64 = encPrefs.getString(SecurityConstants.KEY_ENCRYPTED_SEED, null)
        val saltBase64 = encPrefs.getString(SecurityConstants.KEY_SALT, null)

        val seed: ByteArray
        val salt: ByteArray

        if (encryptedSeedBase64 == null || saltBase64 == null) {
            seed = ByteArray(SecurityConstants.SEED_LENGTH_BYTES).apply { SecureRandom().nextBytes(this) }
            salt = ByteArray(SecurityConstants.SALT_LENGTH_BYTES).apply { SecureRandom().nextBytes(this) }

            val encryptedSeed = keystoreManager.encrypt(seed)

            encPrefs.edit()
                .putString(SecurityConstants.KEY_ENCRYPTED_SEED, Base64.encodeToString(encryptedSeed, Base64.NO_WRAP))
                .putString(SecurityConstants.KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .apply()
        } else {
            salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val encryptedSeed = Base64.decode(encryptedSeedBase64, Base64.NO_WRAP)
            
            val decryptedSeed = keystoreManager.decrypt(encryptedSeed)
            if (decryptedSeed.isEmpty()) {
                throw SecurityRecoveryException("Database seed cannot be recovered. Decrypted seed is empty.")
            }
            seed = decryptedSeed
        }

        var seedChars: CharArray? = null
        var passphraseBytes: ByteArray? = null
        var finalPassphraseBytes: ByteArray? = null
        val spec = PBEKeySpec(Base64.encodeToString(seed, Base64.NO_WRAP).toCharArray(), salt, SecurityConstants.PBKDF2_ITERATIONS, SecurityConstants.PBKDF2_KEY_LENGTH_BITS)

        try {
            seedChars = Base64.encodeToString(seed, Base64.NO_WRAP).toCharArray()
            val factory = SecretKeyFactory.getInstance(SecurityConstants.PBKDF2_ALGORITHM)
            val secret = factory.generateSecret(spec)
            passphraseBytes = secret.encoded
            
            // To maintain compatibility with legacy databases, the new key must be exactly the UTF-8 bytes of the Base64 representation.
            val finalPassphraseStr = Base64.encodeToString(passphraseBytes, Base64.NO_WRAP)
            finalPassphraseBytes = finalPassphraseStr.toByteArray(Charsets.UTF_8)
            
            return SecureDatabaseKey(finalPassphraseBytes)
        } finally {
            Arrays.fill(seed, 0.toByte())
            seedChars?.let { Arrays.fill(it, '\u0000') }
            passphraseBytes?.let { Arrays.fill(it, 0.toByte()) }
            spec.clearPassword()
        }
    }

    private fun migrateDatabaseIfNeeded(legacyKey: SecureDatabaseKey, newKey: SecureDatabaseKey): RekeyResult {
        val dbFile = context.getDatabasePath(SecurityConstants.DB_NAME)
        if (!dbFile.exists()) return RekeyResult.MigrationNotRequired

        com.meshlink.common.logger.MeshLogger.d(TAG, "Attempting to open database with legacy key for migration")
        var legacyDb: SQLiteDatabase? = null
        var verifyDb: SQLiteDatabase? = null
        
        try {
            legacyDb = SQLiteDatabase.openDatabase(
                dbFile.path,
                legacyKey.getBytes(),
                null as SQLiteDatabase.CursorFactory?,
                SQLiteDatabase.OPEN_READWRITE,
                null
            )
            
            com.meshlink.common.logger.MeshLogger.d(TAG, "Checkpointing WAL")
            legacyDb.execSQL("PRAGMA wal_checkpoint(FULL);")
            com.meshlink.common.logger.MeshLogger.d(TAG, "Switching to DELETE journal mode")
            legacyDb.execSQL("PRAGMA journal_mode = DELETE;")
            
            com.meshlink.common.logger.MeshLogger.d(TAG, "Executing PRAGMA rekey")
            val formattedHexKey = SqlCipherKeyFormatter.formatHexKey(newKey)
            
            // PRAGMA rekey changes the encryption key of the database on the fly.
            legacyDb.execSQL("PRAGMA rekey = $formattedHexKey;")
            com.meshlink.common.logger.MeshLogger.d(TAG, "Rekey executed successfully")
            
        } catch (e: Exception) {
            val sanitizedMsg = e.javaClass.simpleName
            com.meshlink.common.logger.MeshLogger.e(TAG, "Rekey failure: $sanitizedMsg", e)
            return RekeyResult.RekeyFailed(sanitizedMsg)
        } finally {
            legacyDb?.close()
        }
        
        com.meshlink.common.logger.MeshLogger.d(TAG, "Re-opening with new key to verify")
        try {
            verifyDb = SQLiteDatabase.openDatabase(
                dbFile.path,
                newKey.getBytes(),
                null as SQLiteDatabase.CursorFactory?,
                SQLiteDatabase.OPEN_READWRITE,
                null
            )
            
            com.meshlink.common.logger.MeshLogger.d(TAG, "Executing PRAGMA integrity_check")
            verifyDb.rawQuery("PRAGMA integrity_check;", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    if (!result.equals("ok", ignoreCase = true)) {
                        return RekeyResult.VerificationFailed("integrity_check failed: $result")
                    }
                } else {
                    return RekeyResult.VerificationFailed("integrity_check returned empty")
                }
            }
            
            com.meshlink.common.logger.MeshLogger.d(TAG, "Reading representative tables")
            verifyDb.rawQuery("SELECT COUNT(*) FROM sqlite_schema;", null).use { cursor ->
                if (!cursor.moveToFirst()) {
                    return RekeyResult.VerificationFailed("sqlite_schema read failed")
                }
            }
            
            com.meshlink.common.logger.MeshLogger.d(TAG, "Restoring WAL mode")
            verifyDb.execSQL("PRAGMA journal_mode = WAL;")
            verifyDb.execSQL("PRAGMA synchronous = NORMAL;")
            
            return RekeyResult.Success
        } catch (e: Exception) {
            val sanitizedMsg = e.javaClass.simpleName
            com.meshlink.common.logger.MeshLogger.e(TAG, "Verification failure: $sanitizedMsg", e)
            return RekeyResult.VerificationFailed(sanitizedMsg)
        } finally {
            verifyDb?.close()
        }
    }
}
