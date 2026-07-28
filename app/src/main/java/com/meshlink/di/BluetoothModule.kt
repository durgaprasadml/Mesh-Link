package com.meshlink.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.meshlink.ble.data.source.BleMeshDataSource
import com.meshlink.ble.data.source.BleMeshDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothBindingModule {
    @Binds
    @Singleton
    abstract fun bindBleMeshDataSource(
        impl: BleMeshDataSourceImpl
    ): BleMeshDataSource

    @Binds
    @Singleton
    abstract fun bindGattConnectionManager(
        impl: com.meshlink.ble.data.gatt.GattConnectionManagerImpl
    ): com.meshlink.ble.data.gatt.GattConnectionManager

    @Binds
    @Singleton
    abstract fun bindMtuNegotiationManager(
        impl: com.meshlink.ble.data.gatt.MtuNegotiationManagerImpl
    ): com.meshlink.ble.data.gatt.MtuNegotiationManager

    @Binds
    @Singleton
    abstract fun bindGattWriteQueue(
        impl: com.meshlink.ble.data.gatt.GattWriteQueueImpl
    ): com.meshlink.ble.data.gatt.GattWriteQueue

    @Binds
    @Singleton
    abstract fun bindApplicationMessageQueue(
        impl: com.meshlink.ble.data.gatt.ApplicationMessageQueueImpl
    ): com.meshlink.ble.data.gatt.ApplicationMessageQueue

    @Binds
    @Singleton
    abstract fun bindServiceDiscoveryManager(
        impl: com.meshlink.ble.data.gatt.ServiceDiscoveryManagerImpl
    ): com.meshlink.ble.data.gatt.ServiceDiscoveryManager

    @Binds
    @Singleton
    abstract fun bindPacketFragmenter(
        impl: com.meshlink.ble.data.gatt.PacketFragmenterImpl
    ): com.meshlink.ble.data.gatt.PacketFragmenter

    @Binds
    @Singleton
    abstract fun bindPacketReassembler(
        impl: com.meshlink.ble.data.gatt.PacketReassemblerImpl
    ): com.meshlink.ble.data.gatt.PacketReassembler

    @Binds
    @Singleton
    abstract fun bindPacketDispatcher(
        impl: com.meshlink.ble.data.CorePacketDispatcher
    ): com.meshlink.ble.api.PacketDispatcher

    @Binds
    @Singleton
    internal abstract fun bindBleTransport(
        impl: com.meshlink.ble.data.BleTransportImpl
    ): com.meshlink.ble.api.BleTransport
}

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    @Provides
    @Singleton
    fun provideGattNotificationManager(
        gattManager: dagger.Lazy<com.meshlink.ble.data.BleGattManager>,
        @com.meshlink.di.ApplicationScope applicationScope: kotlinx.coroutines.CoroutineScope
    ): com.meshlink.ble.data.gatt.GattNotificationManager {
        // GattNotificationManagerImpl needs a way to get the GATT Server instance
        // which resides in BleGattManager. Using Lazy prevents circular dependency during init.
        return com.meshlink.ble.data.gatt.GattNotificationManagerImpl(
            { gattManager.get().getGattServer() },
            applicationScope
        )
    }
}
