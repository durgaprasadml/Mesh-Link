package com.meshlink.di

import com.meshlink.transport.HybridTransport
import com.meshlink.transport.HybridTransportManager
import com.meshlink.wifi.api.WifiTransport
import com.meshlink.wifi.data.WifiTransportImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiModule {

    @Binds
    @Singleton
    internal abstract fun bindWifiTransport(
        impl: WifiTransportImpl
    ): WifiTransport

    @Binds
    @Singleton
    internal abstract fun bindHybridTransport(
        impl: HybridTransportManager
    ): HybridTransport
}
