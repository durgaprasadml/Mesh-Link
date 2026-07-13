package com.meshlink.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.meshlink.data.ble.BleAdvertiserManager
import com.meshlink.data.ble.BleGattManager
import com.meshlink.data.ble.BleScannerManager
import com.meshlink.data.crypto.MeshCryptoManager
import com.meshlink.data.local.MeshDatabase
import com.meshlink.data.local.UserDao
import com.meshlink.data.local.ChatDao
import com.meshlink.data.local.RelayDao
import com.meshlink.data.media.MediaTransferManager
import com.meshlink.data.repository.UserRepositoryImpl
import com.meshlink.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Encrypted Room database using SQLCipher.
     * The passphrase is derived from a stable Keystore-backed seed
     * to ensure local message cache is encrypted securely at rest.
     */
    @Provides
    @Singleton
    fun provideMeshDatabase(
        @ApplicationContext context: Context,
        databaseSecurityManager: com.meshlink.data.crypto.DatabaseSecurityManager
    ): MeshDatabase {
        // SQLCipher native libraries are loaded once in MeshLinkApp.onCreate().
        val passphraseBytes = try {
            databaseSecurityManager.getDatabasePassphrase()
        } catch (e: com.meshlink.data.crypto.SecurityRecoveryException) {
            // Recovery is impossible. Return a controlled error state by providing
            // an empty byte array. We also explicitly disable destructive migration 
            // so SQLCipher throws an exception on query rather than silently deleting user data.
            ByteArray(0)
        }
        
        val factory = SupportOpenHelperFactory(passphraseBytes)

        val builder = Room.databaseBuilder(
            context,
            MeshDatabase::class.java,
            com.meshlink.data.crypto.SecurityConstants.DB_NAME
        ).openHelperFactory(factory)
            
        // Only allow destructive migration in debug builds IF we have a valid passphrase.
        // If the passphrase is empty (Keystore failed), we MUST NOT allow destructive migration.
        if (passphraseBytes.isNotEmpty() && (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            builder.fallbackToDestructiveMigration()
        }
        
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideUserDao(db: MeshDatabase): UserDao {
        return db.userDao
    }

    @Provides
    @Singleton
    fun provideChatDao(db: MeshDatabase): ChatDao {
        return db.chatDao
    }

    @Provides
    @Singleton
    fun provideRelayDao(db: MeshDatabase): RelayDao {
        return db.relayDao
    }

    @Provides
    @Singleton
    fun provideTrustDao(db: MeshDatabase): com.meshlink.data.local.TrustDao {
        return db.trustDao
    }

    @Provides
    @Singleton
    fun provideAuditLogDao(db: MeshDatabase): com.meshlink.data.local.AuditLogDao {
        return db.auditLogDao
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("mesh_preferences") }
        )
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        dataStore: DataStore<Preferences>
    ): UserRepository {
        return UserRepositoryImpl(userDao, dataStore)
    }

    @Provides
    @Singleton
    fun provideBleScannerManager(@ApplicationContext context: Context): BleScannerManager {
        return BleScannerManager(context)
    }

    @Provides
    @Singleton
    fun provideBleAdvertiserManager(@ApplicationContext context: Context): BleAdvertiserManager {
        return BleAdvertiserManager(context)
    }

    @Provides
    @Singleton
    fun provideBleGattManager(@ApplicationContext context: Context): BleGattManager {
        return BleGattManager(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): com.google.firebase.analytics.FirebaseAnalytics {
        return com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideWifiDirectManager(
        @ApplicationContext context: Context,
        analytics: com.google.firebase.analytics.FirebaseAnalytics
    ): com.meshlink.data.wifi.WifiDirectManager {
        return com.meshlink.data.wifi.WifiDirectManager(context, analytics)
    }

    @Provides
    @Singleton
    fun provideMediaTransferManager(@ApplicationContext context: Context): MediaTransferManager {
        return MediaTransferManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceRecorder(@ApplicationContext context: Context): com.meshlink.data.media.VoiceRecorder {
        return com.meshlink.data.media.VoiceRecorder(context)
    }

    @Provides
    @Singleton
    fun provideVoicePlayer(): com.meshlink.data.media.VoicePlayer {
        return com.meshlink.data.media.VoicePlayer()
    }

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): com.meshlink.data.location.LocationProvider {
        return com.meshlink.data.location.LocationProvider(context)
    }

    @Provides
    @Singleton
    fun provideMeshCryptoManager(@ApplicationContext context: Context): MeshCryptoManager {
        return MeshCryptoManager(context)
    }

    @Provides
    @Singleton
    fun provideMeshAnalytics(): com.meshlink.data.analytics.MeshAnalytics {
        return com.meshlink.data.analytics.MeshAnalytics()
    }
}
