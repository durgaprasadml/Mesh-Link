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
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            com.meshlink.common.logger.MeshLogger.e("DbSecurity", "SQLCipher native library load failed", e)
        }

        com.meshlink.common.logger.MeshLogger.d("DbSecurity", "Starting DatabaseModule.provideMeshDatabase() - Lazy init")
        
        val hook = object : SQLiteDatabaseHook {
            override fun preKey(connection: SQLiteConnection?) {}
            override fun postKey(connection: SQLiteConnection?) {
                try {
                    connection?.execute("PRAGMA cipher_version;", null, null)
                    connection?.execute("PRAGMA journal_mode = WAL;", null, null)
                    connection?.execute("PRAGMA synchronous = NORMAL;", null, null)
                } catch (e: Exception) {
                    com.meshlink.common.logger.MeshLogger.e("DbSecurity", "SQLCipher postKey diagnostic failure: ${e.message}", e)
                }
            }
        }

        val lazyFactory = androidx.sqlite.db.SupportSQLiteOpenHelper.Factory { configuration ->
            object : androidx.sqlite.db.SupportSQLiteOpenHelper {
                private var _delegate: androidx.sqlite.db.SupportSQLiteOpenHelper? = null
                private val delegate: androidx.sqlite.db.SupportSQLiteOpenHelper
                    get() {
                        if (_delegate == null) {
                            synchronized(this) {
                                if (_delegate == null) {
                                    val passphraseBytes = try {
                                        databaseSecurityManager.getDatabasePassphrase()
                                    } catch (e: com.meshlink.security.data.SecurityRecoveryException) {
                                        com.meshlink.common.logger.MeshLogger.e("DbSecurity", "Security recovery exception during lazy open", e)
                                        throw e
                                    }
                                    if (passphraseBytes.isEmpty()) {
                                        throw com.meshlink.security.data.SecurityRecoveryException("Empty passphrase returned")
                                    }
                                    val factory = SupportOpenHelperFactory(passphraseBytes, hook, false)
                                    _delegate = factory.create(configuration)
                                }
                            }
                        }
                        return _delegate!!
                    }

                override val databaseName: String? get() = configuration.name
                override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
                    delegate.setWriteAheadLoggingEnabled(enabled)
                }
                override val writableDatabase: androidx.sqlite.db.SupportSQLiteDatabase get() = delegate.writableDatabase
                override val readableDatabase: androidx.sqlite.db.SupportSQLiteDatabase get() = delegate.readableDatabase
                override fun close() {
                    _delegate?.close()
                }
            }
        }

        return Room.databaseBuilder(
            context,
            MeshDatabase::class.java,
            com.meshlink.security.data.SecurityConstants.DB_NAME
        ).addMigrations(
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_1_2,
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_2_3,
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_3_4,
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_4_5,
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_5_6,
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_6_7,
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_7_8,
            com.meshlink.database.data.local.MeshDatabaseMigrations.MIGRATION_8_9
        ).openHelperFactory(lazyFactory).build()
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
