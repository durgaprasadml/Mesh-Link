package com.meshlink.ble.data

import android.app.Application
import android.content.Context
import android.net.Uri
import com.meshlink.common.logger.MeshLogger
import com.meshlink.ble.data.BleAdvertiserManager
import com.meshlink.ble.data.BleConstants
import com.meshlink.ble.data.BleGattManager
import com.meshlink.ble.data.BleGattManager.GattEvent
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.BroadcastType
import com.meshlink.domain.model.PeerConnectionState
import com.meshlink.ble.data.source.BleMeshDataSource
import com.meshlink.data.location.LocationProvider
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.media.data.ImageCompressor
import com.meshlink.transfer.TransferManager
import com.meshlink.routing.api.Router
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.util.NotificationHelper
import com.meshlink.voice.transport.VoiceTransport

import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

@Singleton

class BleRepositoryImpl @Inject constructor(
    private val application: Application,
    private val bleDataSource: BleMeshDataSource,
    private val meshRouter: Router,
    private val chatDao: ChatDao,
    private val userRepository: UserRepository,
    private val transferManager: com.meshlink.transfer.TransferManager,
    private val mediaTransferManager: com.meshlink.media.data.MediaTransferManager,
    private val locationProvider: LocationProvider,
    private val cryptoManager: MeshCryptoManager,

    private val sessionManager: com.meshlink.security.data.SessionManager,
    private val rekeyManager: com.meshlink.security.data.RekeyManager,
    private val trustManager: com.meshlink.security.data.TrustManager,
    private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,
    private val discoveryManager: DiscoveryManager,
    private val connectionManager: BleConnectionManager,
    private val routingCoordinator: RoutingCoordinator,
    private val meshMessagingManager: MeshMessagingManager,
    private val voiceTransport: VoiceTransport,
    @ApplicationContext private val context: Context
) : MeshRepository {
    companion object {
        private const val TAG = "MeshRepository"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val discoveryEngine get() = discoveryManager.discoveryEngine

    private fun updatePeerState(address: String, newState: PeerConnectionState) {
        connectionManager.updatePeerState(address, newState)
        if (newState == PeerConnectionState.SERVICES_DISCOVERED || newState == PeerConnectionState.MTU_READY || newState == PeerConnectionState.CONNECTED) {
            meshMessagingManager.checkAndTriggerHandshake(address)
        }
    }

    private fun checkAndTriggerHandshake(address: String) {
        meshMessagingManager.checkAndTriggerHandshake(address)
    }

    override val scannedDevices: StateFlow<Map<String, BleDevice>> = discoveryManager.scannedDevices
    override val incomingMeshPayloads: SharedFlow<Pair<String, MeshPacket>> = meshRouter.incomingPayloads
    override val transferProgress = transferManager.transferProgress

    private fun networkId(peerId: String): String = routingCoordinator.networkId(peerId)
    private fun normalizePeerId(peerIdOrAddress: String): String = routingCoordinator.normalizePeerId(peerIdOrAddress)
    override fun resolveChatId(peerIdOrAddress: String): String = routingCoordinator.resolveChatId(peerIdOrAddress)
    private fun outgoingChatId(targetMeshId: String): String = routingCoordinator.outgoingChatId(targetMeshId)
    private fun incomingChatId(senderMeshId: String): String = routingCoordinator.incomingChatId(senderMeshId)
    private fun resolvePeerAddress(peerIdOrAddress: String): String? = routingCoordinator.resolvePeerAddress(peerIdOrAddress)

    
    override suspend fun setLocalMeshId(meshId: String) {
        meshRouter.localMeshId = networkId(meshId)
    }

    init {
        // Wire RekeyManager
        rekeyManager.sendPacketCallback = { peerId, packet ->
            val user = userRepository.getLocalUser()
            val senderId = user?.let { networkId(it.meshId) } ?: ""
            meshMessagingManager.dispatchSinglePacket(peerId, packet.copy(senderId = senderId))
        }
        rekeyManager.forceKeyExchangeCallback = { peerId ->
            scope.launch {
                val address = resolvePeerAddress(peerId)
                if (address != null) {
                    connectionManager.peerStates[address] = PeerConnectionState.KEY_EXCHANGE_STARTED
                    val user = userRepository.getLocalUser()
                    if (user != null) {
                        val localPeerId = networkId(user.meshId)
                        val packetBase = meshMessagingManager.generateSignedKeyExchange(localPeerId)
                        val packet = packetBase.copy(targetId = peerId)
                        meshMessagingManager.dispatchSinglePacket(peerId, packet)
                    }
                }
            }
        }

        // Wire TransferManager so it can dispatch ACK/NACK/retried chunks via MeshRouter
        transferManager.onSendPacket = { packet ->
            meshRouter.sendMediaPacket(packet)
        }
        
        transferManager.onTransferCompleted = { session ->
            scope.launch {
                meshMessagingManager.receiveMediaMessage(session.transferId, session.filePath!!, session.mimeType, session.senderId)
            }
        }

        transferManager.onOutgoingTransferCompleted = { session ->
            scope.launch {
                chatDao.updateMessageStatus(session.transferId, DeliveryStatus.SENT)
            }
        }

        // Wire VoiceTransport for outbound real-time voice packets
        voiceTransport.onSendPacket = { packet ->
            meshRouter.sendMediaPacket(packet) // Reuse high-priority routing
        }


        scope.launch {
            incomingMeshPayloads.collect { (_, packet) ->
                meshMessagingManager.handleIncomingPacket(packet)
            }
        }

        // Note: Transfer progress is now handled by TransferManager's scheduler.
        // We will collect state flow updates from TransferScheduler if UI needs it.

        // Periodically retry sending PENDING messages if we are connected to anyone
        scope.launch {
            while (scope.isActive) {
                delay(15000)
                meshMessagingManager.retryPendingMessages()
            }
        }

        scope.launch {
            scannedDevices.collect { devices ->
                if (devices.isNotEmpty()) {
                    meshMessagingManager.retryPendingMessages()
                }
            }
        }
        

        // Phase E2: Observe DiscoveryEngine events for Smart Connect
        scope.launch {
            discoveryEngine.engineEvents.collect { record ->
                val state = connectionManager.peerStates[record.macAddress] ?: PeerConnectionState.DISCONNECTED
                val isConnected = state == PeerConnectionState.CONNECTED || 
                                  state == PeerConnectionState.SESSION_READY || 
                                  state == PeerConnectionState.SESSION_ESTABLISHED
                
                if (discoveryEngine.connectionPolicy.canConnect(record, isConnected)) {
                    if (state == PeerConnectionState.DISCONNECTED || state == PeerConnectionState.DISCOVERED) {
                        discoveryEngine.notifyConnectionAttempt(record.macAddress)
                        connectionManager.connectToDevice(record.macAddress, isManual = false)
                    }
                }
            }
        }
    }

    


    override fun isAnyPeerConnected(): Boolean {
        return connectionManager.connectedServers.isNotEmpty() || connectionManager.activeClients.isNotEmpty()
    }

    /**
     * FIX ISSUE 2: Connect to ALL scanned devices to establish GATT links.
     * This ensures mesh relay works — e.g. if A sees B but not C,
     * A must have a GATT connection to B so packets relay through B to C.
     */
    override fun connectToAllScannedDevices() {
        meshMessagingManager.connectToAllScannedDevices()
    }

    private fun hasDeliveryPath(targetPeerIdOrAddress: String): Boolean = routingCoordinator.hasDeliveryPath(targetPeerIdOrAddress)

    /**
     * FIX ISSUE 1 & 2: Dispatch text via mesh.
     * - Auto-connects to all scanned peers for relay
     * - Accepts packetId so retries use the same ID
     * - Only sends when delivery path exists
     */
    @Deprecated("Use routeTextMessage instead", ReplaceWith("routeTextMessage(targetPeerId, payload, localPeerId, encrypted, packetId)"))
    override fun dispatchTextMessage(
        targetPeerId: String,
        payload: String,
        localPeerId: String,
        encrypted: Boolean,
        packetId: String?
    ): Boolean {
        // Build packet and route it
        val packet = com.meshlink.domain.model.MeshPacket(
            packetId = packetId ?: java.util.UUID.randomUUID().toString(),
            senderId = localPeerId,
            targetId = targetPeerId,
            payload = payload,
            type = com.meshlink.domain.model.PacketType.TEXT,
            encrypted = encrypted
        )
        kotlinx.coroutines.runBlocking {
            meshMessagingManager.dispatchSinglePacket(targetPeerId, packet)
        }
        return true
    }

    override suspend fun routeTextMessage(
        targetPeerId: String,
        payload: String,
        localPeerId: String,
        encrypted: Boolean,
        packetId: String?
    ): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            val packet = com.meshlink.domain.model.MeshPacket(
                packetId = packetId ?: java.util.UUID.randomUUID().toString(),
                senderId = localPeerId,
                targetId = targetPeerId,
                payload = payload,
                type = com.meshlink.domain.model.PacketType.TEXT,
                encrypted = encrypted
            )
            val success = meshMessagingManager.dispatchSinglePacket(targetPeerId, packet)
            if (success) {
                com.meshlink.domain.model.MeshResult.Success(Unit)
            } else {
                com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.RoutingError("No path to target", targetPeerId))
            }
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.RoutingError("Failed to route text message", targetPeerId, e))
        }
    }

    
    


    // ────────── BLE Lifecycle ──────────

    override fun startAdvertising(name: String, meshId: String) {
        discoveryManager.startAdvertising(name, meshId, 0x01) // 0x01 = Routing Support
    }

    override fun stopAdvertising() {
        discoveryManager.stopAdvertising()
    }

    override fun startScanning() {
        discoveryManager.startScanning()
        // Start the intelligent engine loop
        // (Assuming BleScannerManager delegates this internally, but we can also trigger engine here)
    }

    override fun stopScanning() {
        discoveryManager.stopScanning()
    }

    override fun startServer() {
        connectionManager.startServer()
    }

    override fun stopServer() {
        connectionManager.stopServer()
    }

    @Deprecated("Use connectDevice instead", ReplaceWith("connectDevice(address)"))
    override fun connectToDevice(address: String) {
        // Exposed via MeshRepository, considered a manual reconnect intent
        connectionManager.connectToDevice(address, isManual = true)
    }

    override suspend fun connectDevice(address: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            connectionManager.connectToDevice(address, isManual = true)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.TransportError("Failed to connect", deviceAddress = address, cause = e))
        }
    }

    @Deprecated("Use connectPeer instead", ReplaceWith("connectPeer(peerIdOrAddress)"))
    override fun connectToPeer(peerIdOrAddress: String): Boolean {
        val address = resolvePeerAddress(peerIdOrAddress) ?: return false
        return try {
            connectToDevice(address)
            true
        } catch (e: Exception) {
            MeshLogger.w(TAG, "connectToPeer failed for $peerIdOrAddress: ${e.message}")
            false
        }
    }

    override suspend fun connectPeer(peerIdOrAddress: String): com.meshlink.domain.model.MeshResult<Unit> {
        val address = resolvePeerAddress(peerIdOrAddress) 
            ?: return com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.TransportError("Could not resolve address", peerIdOrAddress))
        return connectDevice(address)
    }

    /**
     * Auto-start BLE advertising + scanning + GATT server.
     * FIX ISSUE 2: Also auto-connects to all scanned devices for mesh relay.
     */
    override suspend fun autoStartMesh() {
        meshMessagingManager.autoStartMesh()
    }

    override suspend fun refreshMesh() {
        meshMessagingManager.refreshMesh()
    }

    /**
     * Stop all BLE operations.
     */
    override fun stopMesh() {
        stopAdvertising()
        stopScanning()
        stopServer()
    }

    @VisibleForTesting
    fun cancelScope() {
        scope.cancel()
    }

    // ────────── Text Messages (ENCRYPTED) ──────────

    @Deprecated("Use dispatchMessage instead", ReplaceWith("dispatchMessage(targetMeshId, message)"))
    override suspend fun sendMessage(targetMeshId: String, message: com.meshlink.domain.model.Message) {
        meshMessagingManager.sendMessage(targetMeshId, message)
    }

    override suspend fun dispatchMessage(targetMeshId: String, message: com.meshlink.domain.model.Message): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            meshMessagingManager.sendMessage(targetMeshId, message)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.RoutingError("Failed to send message", targetMeshId, e))
        }
    }

    @Deprecated("Use dispatchImage instead", ReplaceWith("dispatchImage(targetMeshId, imageUri, chatName)"))
    override suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        meshMessagingManager.sendImage(targetMeshId, imageUri, chatName)
    }

    override suspend fun dispatchImage(targetMeshId: String, imageUri: Uri, chatName: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            meshMessagingManager.sendImage(targetMeshId, imageUri, chatName)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.MediaError("Failed to send image", targetMeshId, e))
        }
    }

    @Deprecated("Use dispatchVoiceNote instead", ReplaceWith("dispatchVoiceNote(targetMeshId, filePath, durationMs, chatName)"))
    override suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
        meshMessagingManager.sendVoiceNote(targetMeshId, filePath, durationMs, chatName)
    }

    override suspend fun dispatchVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            meshMessagingManager.sendVoiceNote(targetMeshId, filePath, durationMs, chatName)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.MediaError("Failed to send voice note", targetMeshId, e))
        }
    }

    @Deprecated("Use dispatchLocation instead", ReplaceWith("dispatchLocation(targetMeshId, chatName)"))
    override suspend fun sendLocation(targetMeshId: String, chatName: String) {
        meshMessagingManager.sendLocation(targetMeshId, chatName)
    }

    override suspend fun dispatchLocation(targetMeshId: String, chatName: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            meshMessagingManager.sendLocation(targetMeshId, chatName)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.RoutingError("Failed to send location", targetMeshId, e))
        }
    }

    @Deprecated("Use dispatchReadReceipts instead", ReplaceWith("dispatchReadReceipts(chatId)"))
    override suspend fun sendReadReceipts(chatId: String) {
        meshMessagingManager.sendReadReceipts(chatId)
    }

    override suspend fun dispatchReadReceipts(chatId: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            meshMessagingManager.sendReadReceipts(chatId)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.RoutingError("Failed to send read receipts", chatId, e))
        }
    }

    @Deprecated("Use dispatchSos instead", ReplaceWith("dispatchSos()"))
    override suspend fun sendSos() {
        meshMessagingManager.sendSos()
    }

    override suspend fun dispatchSos(): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            meshMessagingManager.sendSos()
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.RoutingError("Failed to send SOS", null, e))
        }
    }

    @Deprecated("Use dispatchBroadcastMessage instead", ReplaceWith("dispatchBroadcastMessage(messageText)"))
    override suspend fun broadcastMessage(messageText: String) {
        meshMessagingManager.broadcastMessage(messageText)
    }

    override suspend fun dispatchBroadcastMessage(messageText: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            meshMessagingManager.broadcastMessage(messageText)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.RoutingError("Failed to broadcast message", null, e))
        }
    }

    override fun getMeshStatus(): com.meshlink.domain.model.MeshStatus {
        return com.meshlink.domain.model.MeshStatus(
            isBleAdvertising = discoveryManager.isAdvertising(),
            isBleScanning = discoveryManager.isScanning(),
            connectedPeersCount = connectionManager.connectedServers.size + connectionManager.activeClients.size,
            isServerRunning = true
        )
    }

    override fun getRouteTable(): Map<String, String> {
        return meshRouter.routeTable.mapValues { it.value.nextHop }
    }

    override fun getLocalMeshId(): String {
        return meshRouter.localMeshId
    }
}
