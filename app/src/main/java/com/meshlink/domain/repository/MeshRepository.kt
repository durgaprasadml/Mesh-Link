package com.meshlink.domain.repository

import android.net.Uri
import com.meshlink.domain.model.BleDevice
import com.meshlink.routing.data.MeshRouter
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MeshRepository {
    val meshRouter: MeshRouter
    val scannedDevices: StateFlow<Map<String, BleDevice>>
    val incomingMeshPayloads: SharedFlow<Pair<String, com.meshlink.ble.data.MeshPacket>>
    val transferProgress: StateFlow<Map<String, Float>>

    fun resolveChatId(peerIdOrAddress: String): String
    fun isAnyPeerConnected(): Boolean
    
    fun startAdvertising(name: String, meshId: String)
    fun stopAdvertising()
    fun startScanning()
    fun stopScanning()
    fun startServer()
    fun stopServer()
    
    fun connectToDevice(address: String)
    fun connectToPeer(peerIdOrAddress: String): Boolean
    
    suspend fun autoStartMesh()
    fun stopMesh()
    
    suspend fun sendMessage(message: com.meshlink.domain.model.Message, chatName: String)
    suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String)
    suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String)
    suspend fun sendLocation(targetMeshId: String, chatName: String)
    suspend fun sendReadReceipts(chatId: String)
    suspend fun sendSos()
    suspend fun broadcastMessage(messageText: String)

    suspend fun setLocalMeshId(meshId: String)
    fun connectToAllScannedDevices()
    fun dispatchTextMessage(targetPeerId: String, payload: String, localPeerId: String, encrypted: Boolean, packetId: String?): Boolean
}
