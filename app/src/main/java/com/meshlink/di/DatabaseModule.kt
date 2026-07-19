package com.meshlink.di

import android.app.KeyguardManager
import android.content.Context
import androidx.room.Room
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.MeshDatabase
import com.meshlink.database.data.local.RelayDao
import com.meshlink.database.data.local.UserDao
import com.meshlink.database.data.source.ChatLocalDataSource
import com.meshlink.database.data.source.ChatLocalDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SQLiteNotADatabaseException
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMeshDatabase(
        @ApplicationContext context: Context,
        databaseSecurityManager: com.meshlink.security.data.DatabaseSecurityManager
    ): MeshDatabase {
        // Double-check native library loading
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            com.meshlink.common.logger.MeshLogger.e("DbSecurity", "SQLCipher native library load failed", e)
        }

        com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Starting DatabaseModule.provideMeshDatabase()")
        val dbFile = context.getDatabasePath(com.meshlink.security.data.SecurityConstants.DB_NAME)
        
        // Header inspection
        if (dbFile.exists()) {
            try {
                val bytes = ByteArray(16)
                java.io.FileInputStream(dbFile).use { it.read(bytes) }
                val headerStr = String(bytes, Charsets.UTF_8)
                if (headerStr.startsWith("SQLite format 3")) {
                    com.meshlink.common.logger.MeshLogger.w("DbSecurity", "Database Header Inspection: PLAINTEXT SQLITE")
                } else {
                    com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Database Header Inspection: ENCRYPTED/UNKNOWN")
                }
            } catch (e: Exception) {
                com.meshlink.common.logger.MeshLogger.e("DbSecurity", "Database Header Inspection: FAILED TO READ", e)
            }
        }
        
        val passphraseBytes = try {
            databaseSecurityManager.getDatabasePassphrase()
        } catch (e: com.meshlink.security.data.SecurityRecoveryException) {
            ByteArray(0)
        }
        
        val hook = object : SQLiteDatabaseHook {
            override fun preKey(connection: SQLiteConnection?) {}
            override fun postKey(connection: SQLiteConnection?) {
                try {
                    com.meshlink.common.logger.MeshLogger.d("DbSecurity", "SQLCipher postKey hook started")
                    connection?.execute("PRAGMA cipher_version;", null, null)
                    connection?.execute("PRAGMA journal_mode;", null, null)
                    connection?.execute("SELECT COUNT(*) FROM sqlite_schema;", null, null)
                    com.meshlink.common.logger.MeshLogger.d("DbSecurity", "SQLCipher postKey diagnostics completed successfully")
                    
                    connection?.execute("PRAGMA journal_mode = WAL;", null, null)
                    connection?.execute("PRAGMA synchronous = NORMAL;", null, null)
                } catch (e: Exception) {
                    com.meshlink.common.logger.MeshLogger.e("DbSecurity", "SQLCipher postKey diagnostic failure: ${e.message}", e)
                }
            }
        }
        
        val factory = SupportOpenHelperFactory(passphraseBytes, hook, false)

        val builder = Room.databaseBuilder(
            context,
            MeshDatabase::class.java,
            com.meshlink.security.data.SecurityConstants.DB_NAME
        ).openHelperFactory(factory)
            
        if (passphraseBytes.isNotEmpty() && (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            builder.fallbackToDestructiveMigration()
        }
        
        return try {
            val db = builder.build()
            // Force open to test connection immediately
            val writableDb = db.openHelper.writableDatabase
            
            // Active Integrity Check
            writableDb.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    if (!result.equals("ok", ignoreCase = true)) {
                        com.meshlink.common.logger.MeshLogger.e("DbSecurity", "Database integrity check failed: $result")
                        throw SQLiteNotADatabaseException("Database integrity check failed: $result")
                    } else {
                        com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Database integrity check passed.")
                    }
                }
            }
            db
        } catch (e: Exception) {
            com.meshlink.common.logger.MeshLogger.e("DbSecurity", "Room build failed. Initiating Recovery Diagnostics.", e)

            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isLocked = keyguardManager.isDeviceLocked
            if (isLocked) {
                com.meshlink.common.logger.MeshLogger.e("DbSecurity", "CRITICAL: Database access attempted while device is LOCKED. This is likely the cause of SQLiteNotADatabaseException due to FBE.")
            }

            // Specific handling for SQLiteNotADatabaseException
            if (e is SQLiteNotADatabaseException || e.cause is SQLiteNotADatabaseException) {
                if (isLocked) {
                    com.meshlink.common.logger.MeshLogger.w("DbSecurity", "Database is not recognized but device is locked. Deferring recovery to avoid false corruption detection.")
                } else {
                    com.meshlink.common.logger.MeshLogger.e("DbSecurity", "Database file is not a database or corrupted while unlocked. ABORTING automatic deletion to preserve potential recovery forensics. Halting database initialization.")
                    throw RuntimeException("Database corruption detected. Manual recovery required. Database recreation aborted.", e)
                }
            }
            
            // Try new key manually
            try {
                com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Recovery: Trying new key")
                net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                    dbFile.path,
                    passphraseBytes,
                    null as net.zetetic.database.sqlcipher.SQLiteDatabase.CursorFactory?,
                    net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
                    null
                ).close()
                com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Recovery: New key succeeded")
            } catch (ex: Exception) {
                com.meshlink.common.logger.MeshLogger.e("DbSecurity", "Recovery: New key failed", ex)
            }
            
            // Try legacy key manually
            val legacyPrefs = context.getSharedPreferences(com.meshlink.security.data.SecurityConstants.DB_PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
            val legacyPassphrase = legacyPrefs.getString(com.meshlink.security.data.SecurityConstants.KEY_LEGACY_PASSPHRASE, null)
            if (legacyPassphrase != null) {
                try {
                    com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Recovery: Trying legacy key")
                    net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                        dbFile.path,
                        legacyPassphrase.toByteArray(Charsets.UTF_8),
                        null as net.zetetic.database.sqlcipher.SQLiteDatabase.CursorFactory?,
                        net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
                        null
                    ).close()
                    com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Recovery: Legacy key succeeded. Migration is proven incomplete.")
                } catch (ex: Exception) {
                    com.meshlink.common.logger.MeshLogger.e("DbSecurity", "Recovery: Legacy key failed. Database may be corrupted.", ex)
                }
            } else {
                com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Recovery: No legacy key available.")
            }
            
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideUserDao(db: MeshDatabase): UserDao = db.userDao

    @Provides
    @Singleton
    fun provideChatDao(db: MeshDatabase): ChatDao = db.chatDao

    @Provides
    @Singleton
    fun provideRelayDao(db: MeshDatabase): RelayDao = db.relayDao

    @Provides
    @Singleton
    fun provideTrustDao(db: MeshDatabase): com.meshlink.database.data.local.TrustDao = db.trustDao

    @Provides
    @Singleton
    fun provideAuditLogDao(db: MeshDatabase): com.meshlink.database.data.local.AuditLogDao = db.auditLogDao



}
