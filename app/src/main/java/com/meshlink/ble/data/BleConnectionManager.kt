package com.meshlink.ble.data

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.meshlink.ble.data.source.BleMeshDataSource
import java.util.concurrent.ConcurrentHashMap
import com.meshlink.common.logger.MeshLogger
import com.meshlink.ble.discovery.DiscoveryEngine

@Singleton
class BleConnectionManager @Inject constructor(
    private val bleDataSource: BleMeshDataSource,
    private val discoveryEngine: DiscoveryEngine
) {
    private val TAG = "BleConnectionManager"

    // Moving peerStates from BleRepositoryImpl here
    val peerStates: MutableMap<String, PeerConnectionState> = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, PeerConnectionState>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PeerConnectionState>?): Boolean {
                return size > 1000
            }
        }
    )
    
    val activeClients: Set<String>
        get() = bleDataSource.activeClients
        
    val connectedServers: Set<String>
        get() = bleDataSource.connectedServers

    fun startServer() {
        bleDataSource.startServer()
    }

    fun stopServer() {
        bleDataSource.stopServer()
    }

    fun connectToDevice(address: String) {
        bleDataSource.connectToDevice(address)
    }

    fun disconnectFromDevice(address: String) {
        bleDataSource.disconnectFromDevice(address)
    }

    fun isAnyPeerConnected(): Boolean {
        return bleDataSource.connectedServers.isNotEmpty() || bleDataSource.activeClients.isNotEmpty()
    }

    fun updatePeerState(address: String, newState: PeerConnectionState) {
        val current = peerStates[address] ?: PeerConnectionState.DISCONNECTED
        peerStates[address] = newState
        MeshLogger.d(TAG, "Peer $address state: $current -> $newState")
        
        if (newState == PeerConnectionState.CONNECTED) {
            discoveryEngine.notifyConnectionSuccess(address)
            updateAnalyticsConnectionCount()
        } else if (newState == PeerConnectionState.DISCONNECTED) {
            discoveryEngine.notifyConnectionFailure(address)
            updateAnalyticsConnectionCount()
        }
    }
    
    fun updateAnalyticsConnectionCount() {
        val count = synchronized(peerStates) {
            peerStates.values.count { 
                it == PeerConnectionState.CONNECTED || 
                it == PeerConnectionState.SESSION_READY || 
                it == PeerConnectionState.SESSION_ESTABLISHED 
            }
        }
        discoveryEngine.analytics.updateActiveConnections(count)
    }
    
    fun getPeerState(address: String): PeerConnectionState {
        return peerStates[address] ?: PeerConnectionState.DISCONNECTED
    }
}
