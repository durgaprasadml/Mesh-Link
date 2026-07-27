package com.meshlink.domain.repository

import android.net.Uri
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.MeshResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages high-level mesh network operations and coordination.
 *
 * Responsibility: Facade for BLE operations, message dispatching, and mesh lifecycle.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe.
 */
interface MeshRepository {
    val scannedDevices: StateFlow<Map<String, BleDevice>>
    val incomingMeshPayloads: SharedFlow<Pair<String, com.meshlink.domain.model.MeshPacket>>
    val transferProgress: StateFlow<Map<String, Float>>

    fun resolveChatId(peerIdOrAddress: String): String
    fun isAnyPeerConnected(): Boolean
    
    fun startAdvertising(name: String, meshId: String)
    fun stopAdvertising()
    fun startScanning()
    fun stopScanning()
    fun startServer()
    fun stopServer()
    
    @Deprecated("Use connectDevice instead", ReplaceWith("connectDevice(address)"))
    fun connectToDevice(address: String)
    suspend fun connectDevice(address: String): MeshResult<Unit>

    @Deprecated("Use connectPeer instead", ReplaceWith("connectPeer(peerIdOrAddress)"))
    fun connectToPeer(peerIdOrAddress: String): Boolean
    suspend fun connectPeer(peerIdOrAddress: String): MeshResult<Unit>
    
    suspend fun autoStartMesh()
    suspend fun refreshMesh()
    fun stopMesh()
    fun getMeshStatus(): com.meshlink.domain.model.MeshStatus
    
    @Deprecated("Use dispatchMessage instead", ReplaceWith("dispatchMessage(targetMeshId, message)"))
    suspend fun sendMessage(targetMeshId: String, message: com.meshlink.domain.model.Message)
    suspend fun dispatchMessage(targetMeshId: String, message: com.meshlink.domain.model.Message): MeshResult<Unit>

    @Deprecated("Use dispatchImage instead", ReplaceWith("dispatchImage(targetMeshId, imageUri, chatName)"))
    suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String)
    suspend fun dispatchImage(targetMeshId: String, imageUri: Uri, chatName: String): MeshResult<Unit>

    @Deprecated("Use dispatchVoiceNote instead", ReplaceWith("dispatchVoiceNote(targetMeshId, filePath, durationMs, chatName)"))
    suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String)
    suspend fun dispatchVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String): MeshResult<Unit>

    @Deprecated("Use dispatchLocation instead", ReplaceWith("dispatchLocation(targetMeshId, chatName)"))
    suspend fun sendLocation(targetMeshId: String, chatName: String)
    suspend fun dispatchLocation(targetMeshId: String, chatName: String): MeshResult<Unit>

    @Deprecated("Use dispatchReadReceipts instead", ReplaceWith("dispatchReadReceipts(chatId)"))
    suspend fun sendReadReceipts(chatId: String)
    suspend fun dispatchReadReceipts(chatId: String): MeshResult<Unit>

    @Deprecated("Use dispatchSos instead", ReplaceWith("dispatchSos()"))
    suspend fun sendSos()
    suspend fun dispatchSos(): MeshResult<Unit>

    @Deprecated("Use dispatchBroadcastMessage instead", ReplaceWith("dispatchBroadcastMessage(messageText)"))
    suspend fun broadcastMessage(messageText: String)
    suspend fun dispatchBroadcastMessage(messageText: String): MeshResult<Unit>

    suspend fun setLocalMeshId(meshId: String)
    fun connectToAllScannedDevices()

    @Deprecated("Use routeTextMessage instead", ReplaceWith("routeTextMessage(targetPeerId, payload, localPeerId, encrypted, packetId)"))
    fun dispatchTextMessage(targetPeerId: String, payload: String, localPeerId: String, encrypted: Boolean, packetId: String?): Boolean
    suspend fun routeTextMessage(targetPeerId: String, payload: String, localPeerId: String, encrypted: Boolean, packetId: String?): MeshResult<Unit>

    // Debug / Analytics accessors
    fun getRouteTable(): Map<String, String>
    fun getLocalMeshId(): String
}
