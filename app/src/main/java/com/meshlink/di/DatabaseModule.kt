package com.meshlink.di

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
        val passphraseBytes = try {
            databaseSecurityManager.getDatabasePassphrase()
        } catch (e: com.meshlink.security.data.SecurityRecoveryException) {
            ByteArray(0)
        }
        
        val hook = object : SQLiteDatabaseHook {
            override fun preKey(connection: SQLiteConnection?) {}
            override fun postKey(connection: SQLiteConnection?) {
                try {
                    connection?.execute("PRAGMA journal_mode = WAL;", null, null)
                    connection?.execute("PRAGMA synchronous = NORMAL;", null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
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
        
        return builder.build()
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
