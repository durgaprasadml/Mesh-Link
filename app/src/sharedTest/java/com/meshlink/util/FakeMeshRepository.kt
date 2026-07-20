package com.meshlink.util

import android.net.Uri
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.routing.data.MeshRouter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import io.mockk.mockk

class FakeMeshRepository : MeshRepository {
    // meshRouter removed from interface
    private val _scannedDevices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    override val scannedDevices: StateFlow<Map<String, BleDevice>> = _scannedDevices
    
    private val _incomingMeshPayloads = MutableSharedFlow<Pair<String, com.meshlink.domain.model.MeshPacket>>()
    override val incomingMeshPayloads: SharedFlow<Pair<String, com.meshlink.domain.model.MeshPacket>> = _incomingMeshPayloads
    
    private val _transferProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    override val transferProgress: StateFlow<Map<String, Float>> = _transferProgress
    
    var isConnected = false
    
    override fun resolveChatId(peerIdOrAddress: String): String = "chat_$peerIdOrAddress"
    override fun isAnyPeerConnected(): Boolean = isConnected
    
    override fun startAdvertising(name: String, meshId: String) {}
    override fun stopAdvertising() {}
    override fun startScanning() {}
    override fun stopScanning() {}
    override fun startServer() {}
    override fun stopServer() {}
    
    override fun connectToDevice(address: String) {}
    override fun connectToPeer(peerIdOrAddress: String): Boolean = true
    
    override suspend fun autoStartMesh() {}
    override fun stopMesh() {}
    
    override suspend fun sendMessage(message: Message, chatName: String) {}
    override suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {}
    override suspend fun sendDocument(targetMeshId: String, documentUri: Uri, chatName: String) {}
    override suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {}
    override suspend fun sendLocation(targetMeshId: String, chatName: String) {}
    override suspend fun sendReadReceipts(chatId: String) {}
    override suspend fun sendSos() {}
    override suspend fun broadcastMessage(messageText: String) {}
    
    override suspend fun setLocalMeshId(meshId: String) {}
    override fun connectToAllScannedDevices() {}
    override fun dispatchTextMessage(targetPeerId: String, payload: String, localPeerId: String, encrypted: Boolean, packetId: String?): Boolean = true
    
    fun emitScannedDevice(device: BleDevice) {
        val current = _scannedDevices.value.toMutableMap()
        current[device.address] = device
        _scannedDevices.value = current
    }

    override fun getMeshStatus(): com.meshlink.domain.model.MeshStatus {
        return com.meshlink.domain.model.MeshStatus(
            isBleAdvertising = false,
            isBleScanning = false,
            connectedPeersCount = if (isConnected) 1 else 0,
            isServerRunning = false
        )
    }

    override fun getRouteTable(): Map<String, String> = emptyMap()
    override fun getLocalMeshId(): String = "fake_local_mesh_id"
}
