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
     * The passphrase is derived from a stable device-specific key
     * to ensure local message cache is encrypted at rest.
     */
    @Provides
    @Singleton
    fun provideMeshDatabase(@ApplicationContext context: Context): MeshDatabase {
        // SQLCipher native libraries are loaded once in MeshLinkApp.onCreate().
        // Generate a stable DB passphrase from the app's unique installation.
        val prefs = context.getSharedPreferences("mesh_db_config", Context.MODE_PRIVATE)
        var passphrase = prefs.getString("db_passphrase", null)
        if (passphrase == null) {
            passphrase = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("db_passphrase", passphrase).apply()
        }

        val factory = SupportOpenHelperFactory(passphrase.toByteArray())

        return Room.databaseBuilder(
            context,
            MeshDatabase::class.java,
            "mesh_db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
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
