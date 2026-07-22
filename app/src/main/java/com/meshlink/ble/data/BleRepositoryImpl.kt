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
import com.meshlink.ble.data.PeerConnectionState
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
import com.meshlink.routing.data.MeshRouter
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.util.NotificationHelper
import com.meshlink.voice.transport.VoiceTransport
import com.meshlink.video.transport.VideoTransport
import com.meshlink.wifi.data.WifiDirectManager
import com.meshlink.wifi.data.WifiSocketTransport
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
    private val meshRouter: MeshRouter,
    private val chatDao: ChatDao,
    private val userRepository: UserRepository,
    private val transferManager: com.meshlink.transfer.TransferManager,
    private val mediaTransferManager: com.meshlink.media.data.MediaTransferManager,
    private val locationProvider: LocationProvider,
    private val cryptoManager: MeshCryptoManager,
    private val wifiDirectManager: WifiDirectManager,
    private val wifiSocketTransport: WifiSocketTransport,
    private val sessionManager: com.meshlink.security.data.SessionManager,
    private val rekeyManager: com.meshlink.security.data.RekeyManager,
    private val trustManager: com.meshlink.security.data.TrustManager,
    private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,
    private val discoveryManager: DiscoveryManager,
    private val connectionManager: BleConnectionManager,
    private val routingCoordinator: RoutingCoordinator,
    private val meshMessagingManager: MeshMessagingManager,
    private val voiceTransport: VoiceTransport,
    private val videoTransport: VideoTransport,
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
            scope.launch {
                val user = userRepository.getLocalUser()
                val senderId = user?.let { networkId(it.meshId) } ?: ""
                meshMessagingManager.dispatchSinglePacket(peerId, packet.copy(senderId = senderId))
            }
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

        // Wire VideoTransport for outbound real-time video packets
        videoTransport.onSendPacket = { packet ->
            meshRouter.sendMediaPacket(packet) // High priority routing
        }

        // Wire WifiSocketTransport so it routes incoming packets just like MeshRouter
        wifiSocketTransport.onPacketReceived = { packet ->
            scope.launch {
                meshMessagingManager.handleIncomingPacket(packet)
            }
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
        
        // Phase 6: Broadcast Wi-Fi Direct capability
        scope.launch {
            wifiDirectManager.localDeviceMac.collect { mac ->
                if (mac != null) {
                    val user = userRepository.getLocalUser()
                    if (user != null) {
                        val localPeerId = networkId(user.meshId)
                        val wifiPayload = JSONObject().apply {
                            put("wifiMac", mac)
                        }.toString()
                        val packet = MeshPacket(
                            senderId = localPeerId,
                            targetId = "BROADCAST",
                            payload = wifiPayload,
                            type = PacketType.WIFI_NEGOTIATION,
                            encrypted = false,
                            ttl = 15
                        )
                        meshRouter.sendMediaPacket(packet)
                    }
                }
            }
        }
        
        // Phase 6: Observe Wi-Fi Direct Connection Lifecycle
        scope.launch {
            wifiDirectManager.connectionInfo.collect { info ->
                if (info != null && info.groupFormed) {
                    if (info.isGroupOwner) {
                        wifiSocketTransport.startServer()
                    } else if (info.groupOwnerAddress != null) {
                        wifiSocketTransport.connectAsClient(info.groupOwnerAddress.hostAddress ?: "")
                    }
                } else {
                    wifiSocketTransport.disconnect()
                    wifiSocketTransport.stopServer()
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
                        connectToDevice(record.macAddress)
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
    override fun dispatchTextMessage(
        targetPeerId: String,
        payload: String,
        localPeerId: String,
        encrypted: Boolean,
        packetId: String?
    ): Boolean {
        return meshMessagingManager.dispatchTextMessage(targetPeerId, payload, localPeerId, encrypted, packetId)
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

    override fun connectToDevice(address: String) {
        connectionManager.connectToDevice(address)
    }

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

    /**
     * Auto-start BLE advertising + scanning + GATT server.
     * FIX ISSUE 2: Also auto-connects to all scanned devices for mesh relay.
     */
    override suspend fun autoStartMesh() {
        meshMessagingManager.autoStartMesh()
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

    override suspend fun sendMessage(targetMeshId: String, message: com.meshlink.domain.model.Message, chatName: String) {
        meshMessagingManager.sendMessage(targetMeshId, message, chatName)
    }

    override suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        meshMessagingManager.sendImage(targetMeshId, imageUri, chatName)
    }


    override suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
        meshMessagingManager.sendVoiceNote(targetMeshId, filePath, durationMs, chatName)
    }

    override suspend fun sendLocation(targetMeshId: String, chatName: String) {
        meshMessagingManager.sendLocation(targetMeshId, chatName)
    }

    override suspend fun sendReadReceipts(chatId: String) {
        meshMessagingManager.sendReadReceipts(chatId)
    }

    override suspend fun sendSos() {
        meshMessagingManager.sendSos()
    }

    override suspend fun broadcastMessage(messageText: String) {
        meshMessagingManager.broadcastMessage(messageText)
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
