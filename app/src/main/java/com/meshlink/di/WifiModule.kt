package com.meshlink.di

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import com.meshlink.wifi.data.WifiSocketTransport
import com.meshlink.wifi.data.WifiTransportImpl
import com.meshlink.wifi.manager.WifiP2pManagerFacade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WifiModule {

    @Provides
    @Singleton
    fun provideWifiP2pManager(
        @ApplicationContext context: Context
    ): WifiP2pManager? {
        return context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    @Provides
    @Singleton
    fun provideWifiP2pChannel(
        @ApplicationContext context: Context,
        wifiP2pManager: WifiP2pManager?
    ): WifiP2pManager.Channel? {
        return wifiP2pManager?.initialize(context, context.mainLooper, null)
    }
}
