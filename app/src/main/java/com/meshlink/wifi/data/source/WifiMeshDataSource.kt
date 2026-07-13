package com.meshlink.wifi.data.source

import kotlinx.coroutines.flow.SharedFlow

interface WifiMeshDataSource {
    fun startDiscovery()
    fun stopDiscovery()
    fun createGroup()
    fun removeGroup()
    
    fun sendPayload(address: String, payload: String): Boolean
    
    val incomingPayloads: SharedFlow<Pair<String, String>>
}
