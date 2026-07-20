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
        if (newState == PeerConnectionState.SERVICES_DISCOVERED || newState == PeerConnectionState.MTU_READY) {
            checkAndTriggerHandshake(address)
        }
    }

    private fun checkAndTriggerHandshake(address: String) {
        val state = connectionManager.peerStates[address] ?: return
        if (state == PeerConnectionState.SERVICES_DISCOVERED || state == PeerConnectionState.MTU_READY) {
            val peerId = scannedDevices.value.values.firstOrNull { it.address == address }?.meshId
                ?: meshRouter.routeTable.entries.firstOrNull { it.value.nextHop == address }?.key
                
            if (peerId != null) {
                scope.launch {
                    val reqEnc = userRepository.isEncryptionEnabled.first()
                    if (reqEnc) {
                        if (cryptoManager.hasPeerKey(peerId)) {
                            updatePeerState(address, PeerConnectionState.SESSION_READY)
                            retryPendingMessages()
                        } else {
                            val currentState = connectionManager.peerStates[address]
                            if (currentState != PeerConnectionState.KEY_EXCHANGE_STARTED) {
                                connectionManager.peerStates[address] = PeerConnectionState.KEY_EXCHANGE_STARTED
                                val user = userRepository.getLocalUser()
                                if (user != null) {
                                    val localPeerId = networkId(user.meshId)
                                    val packetBase = generateSignedKeyExchange(localPeerId)
                                    val packet = packetBase.copy(targetId = peerId)
                                    meshMessagingManager.dispatchSinglePacket(peerId, packet)
                                }
                            }
                        }
                    } else {
                        updatePeerState(address, PeerConnectionState.SESSION_READY)
                        retryPendingMessages()
                    }
                }
            }
        }
    }

    override val scannedDevices: StateFlow<Map<String, BleDevice>> = discoveryManager.scannedDevices
    override val incomingMeshPayloads: SharedFlow<Pair<String, MeshPacket>> = meshRouter.incomingPayloads
    override val transferProgress = mediaTransferManager.transferProgress

    private fun networkId(peerId: String): String = routingCoordinator.networkId(peerId)
    private fun normalizePeerId(peerIdOrAddress: String): String = routingCoordinator.normalizePeerId(peerIdOrAddress)
    override fun resolveChatId(peerIdOrAddress: String): String = routingCoordinator.resolveChatId(peerIdOrAddress)
    private fun outgoingChatId(targetMeshId: String): String = routingCoordinator.outgoingChatId(targetMeshId)
    private fun incomingChatId(senderMeshId: String): String = routingCoordinator.incomingChatId(senderMeshId)
    private fun resolvePeerAddress(peerIdOrAddress: String): String? = routingCoordinator.resolvePeerAddress(peerIdOrAddress)

    
    override suspend fun setLocalMeshId(meshId: String) {
        meshRouter.localMeshId = meshId
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
                        val packetBase = generateSignedKeyExchange(localPeerId)
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
                retryPendingMessages()
            }
        }

        scope.launch {
            scannedDevices.collect { devices ->
                if (devices.isNotEmpty()) {
                    retryPendingMessages()
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

    
    private suspend fun retryPendingMessages() {
        val pending = chatDao.getMessagesByStatus(DeliveryStatus.PENDING)
        if (pending.isEmpty()) return

        // FIX ISSUE 2: Auto-connect to all scanned devices before retrying
        connectToAllScannedDevices()
        if (!isAnyPeerConnected()) return

        MeshLogger.d(TAG, "Retrying ${pending.size} pending messages...")
        pending.forEach { msg ->
            if (!hasDeliveryPath(msg.chatId)) {
                return@forEach
            }
            val reqEncCheck = userRepository.isEncryptionEnabled.first()
            if (reqEncCheck && !cryptoManager.hasPeerKey(msg.chatId)) {
                MeshLogger.w(TAG, "Missing key for ${msg.chatId}, requesting key exchange and postponing retry")
                val localUser = userRepository.getLocalUser()
                if (localUser != null) {
                    val localPeerId = networkId(localUser.meshId)
                    val publicKey = cryptoManager.getOrCreatePublicKey()
                    meshRouter.broadcastKeyExchange(localPeerId, publicKey)
                }
                return@forEach
            }
            when (msg.messageType) {
                MessageType.TEXT -> {
                    val user = userRepository.getLocalUser() ?: return@forEach
                    val localPeerId = networkId(user.meshId)
                    val wrappedPayload = JSONObject().apply {
                        put("text", msg.text)
                        put("senderName", user.name)
                    }.toString()
                    val reqEnc = userRepository.isEncryptionEnabled.first()
                    val result = encryptAndWrapPayload(wrappedPayload, msg.chatId, reqEnc, msg.messageId)
                    if (result == null) return@forEach
                    val (payload, isEncrypted) = result
                    // FIX ISSUE 1: Pass original messageId as packetId so retries
                    // produce the same packet and receiver deduplicates them
                    if (dispatchTextMessage(msg.chatId, payload, localPeerId, isEncrypted, msg.messageId)) {
                        chatDao.updateMessageStatus(msg.messageId, DeliveryStatus.SENT)
                    }
                }
                MessageType.IMAGE, MessageType.VOICE -> {
                    val file = msg.mediaPath?.let { File(it) }
                    if (file != null && file.exists()) {
                        val bytes = file.readBytes()
                        val packets = mediaTransferManager.createChunkedPackets(
                            data = bytes,
                            senderId = networkId(msg.senderId),
                            targetId = msg.chatId,
                            mimeType = if (msg.messageType == MessageType.IMAGE) "image/jpeg" else "audio/m4a",
                            transferId = msg.messageId
                        )
                        if (meshMessagingManager.dispatchMediaPackets(msg.chatId, packets)) {
                            chatDao.updateMessageStatus(msg.messageId, DeliveryStatus.SENT)
                        }
                    }
                }
                MessageType.LOCATION -> {
                    val payloadJson = JSONObject().apply {
                        put("lat", msg.latitude)
                        put("lng", msg.longitude)
                        put("battery", msg.batteryPercent)
                        put("timestamp", msg.timestamp)
                        put("senderName", "Me")
                    }.toString()
                    val reqEnc = userRepository.isEncryptionEnabled.first()
                    val result = encryptAndWrapPayload(payloadJson, msg.chatId, reqEnc, msg.messageId)
                    if (result == null) return@forEach
                    val (encPayload, isEnc) = result
                    val packet = MeshPacket(
                        packetId = msg.messageId, // Use original messageId
                        senderId = networkId(msg.senderId),
                        targetId = msg.chatId,
                        payload = encPayload,
                        type = PacketType.LOCATION,
                        encrypted = isEnc
                    )
                    if (meshMessagingManager.dispatchSinglePacket(msg.chatId, packet)) {
                        chatDao.updateMessageStatus(msg.messageId, DeliveryStatus.SENT)
                    }
                }
                else -> {}
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
        scannedDevices.value.values.forEach { device ->
            try {
                if (!connectionManager.activeClients.contains(device.address)) {
                    connectionManager.connectToDevice(device.address)
                }
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Auto-connect failed for ${device.name}: ${e.message}")
            }
        }
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
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        if (!hasDeliveryPath(targetPeerId)) {
            MeshLogger.d(TAG, "No delivery path for text to $targetPeerId, keeping PENDING")
            return false
        }
        meshRouter.sendPayload(targetPeerId, payload, localPeerId, encrypted, packetId)
        return true
    }

    
    
    private fun encryptAndWrapPayload(
        plaintext: String,
        targetPeerId: String,
        requireEncryption: Boolean,
        messageId: String
    ): Pair<String, Boolean>? {
        var finalPlaintext = plaintext
        var aadBytes: ByteArray? = null
        var aadPrefix = ""

        if (requireEncryption) {
            val aadResult = sessionManager.generateAad(targetPeerId)
            if (aadResult != null) {
                aadBytes = aadResult.first
                aadPrefix = aadResult.second
            }
        }

        val result = cryptoManager.encryptOrPassthrough(finalPlaintext, targetPeerId, requireEncryption, messageId, 0, aadBytes)
            ?: return null

        val (ciphertext, isEncrypted) = result
        if (isEncrypted && aadPrefix.isNotEmpty()) {
            return Pair("$aadPrefix$ciphertext", true)
        }
        return result
    }

    /**
     * Handle incoming KEY_EXCHANGE: store the peer's ECDH public key.
     */
    private fun handleKeyExchange(packet: MeshPacket) {
        try {
            val parts = packet.payload.split("|")
            if (parts.size >= 5 && parts[0] == "v2") {
                // v2 format: v2|ecdhPub|timestamp|nonce|version|signatureBase64|signingPub
                val ecdhPublicKey = parts[1]
                val timestamp = parts[2].toLong()
                val nonce = parts[3]
                val version = parts[4].toInt()
                val signatureBase64 = parts[5]
                val signingPublicKey = parts[6]

                val now = System.currentTimeMillis()
                if (Math.abs(now - timestamp) > 120_000) {
                    MeshLogger.e(TAG, "KEY_EXCHANGE timestamp expired or invalid")
                    return
                }

                val dataToVerify = "${packet.packetId}|$ecdhPublicKey|$timestamp|$nonce|$version".toByteArray(Charsets.UTF_8)
                val sigBytes = android.util.Base64.decode(signatureBase64, android.util.Base64.NO_WRAP)
                if (!cryptoManager.verifySignature(signingPublicKey, dataToVerify, sigBytes)) {
                    MeshLogger.e(TAG, "KEY_EXCHANGE signature verification failed")
                    securityMonitor.reportEvent(packet.senderId, com.meshlink.security.data.SecurityEvent.InvalidSignature("KEY_EXCHANGE"))
                    return
                }

                val fingerprint = cryptoManager.getDeviceFingerprint(signingPublicKey)
                trustManager.updatePeerIdentity(packet.senderId, fingerprint, null)
                val trustLevel = trustManager.getTrustLevel(packet.senderId)
                if (trustLevel == com.meshlink.security.data.TrustLevel.BLOCKED || trustLevel == com.meshlink.security.data.TrustLevel.REVOKED) {
                    MeshLogger.w(TAG, "Rejecting KEY_EXCHANGE from rogue node ${packet.senderId}")
                    val address = resolvePeerAddress(packet.senderId)
                    if (address != null) disconnectDevice(address)
                    return
                }

                cryptoManager.storePeerPublicKey(packet.senderId, ecdhPublicKey)
                cryptoManager.storePeerSigningKey(packet.senderId, signingPublicKey)
                
                sessionManager.createSession(
                    peerId = packet.senderId,
                    fingerprint = fingerprint,
                    sessionVersion = version,
                    verified = true
                )

                MeshLogger.d(TAG, "🔐 SECURE Key exchanged with: ${packet.senderId.takeLast(8)}")

                val address = resolvePeerAddress(packet.senderId)
                if (address != null) {
                    updatePeerState(address, PeerConnectionState.SESSION_ESTABLISHED)
                    scope.launch { retryPendingMessages() }
                }
            } else {
                // Legacy unauthenticated key exchange
                cryptoManager.storePeerPublicKey(packet.senderId, packet.payload)
                MeshLogger.d(TAG, "🔐 LEGACY Key exchanged with: ${packet.senderId.takeLast(8)}")
                val address = resolvePeerAddress(packet.senderId)
                if (address != null) {
                    updatePeerState(address, PeerConnectionState.SESSION_READY)
                    scope.launch { retryPendingMessages() }
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to handle KEY_EXCHANGE: ${e.message}")
        }
    }

    private fun disconnectDevice(address: String) {
        connectionManager.disconnectFromDevice(address)
        updatePeerState(address, PeerConnectionState.DISCONNECTED)
    }

    private fun generateSignedKeyExchange(localPeerId: String): MeshPacket {
        val ecdhPublicKey = cryptoManager.getOrCreatePublicKey()
        val signingPublicKey = cryptoManager.getOrCreateSigningKey()
        val timestamp = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString()
        val version = 2
        val uuid = UUID.randomUUID().toString()
        
        val dataToSign = "$uuid|$ecdhPublicKey|$timestamp|$nonce|$version".toByteArray(Charsets.UTF_8)
        val signature = cryptoManager.sign(dataToSign)
        val signatureBase64 = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)

        val payload = "v2|$ecdhPublicKey|$timestamp|$nonce|$version|$signatureBase64|$signingPublicKey"
        return MeshPacket(
            packetId = uuid,
            senderId = localPeerId,
            targetId = "",
            payload = payload,
            type = PacketType.KEY_EXCHANGE,
            encrypted = false
        )
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
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        meshRouter.localMeshId = localPeerId
        
        // Broadcast routing capabilities
        discoveryManager.startAdvertising(user.name, user.meshId, 0x01)
        startServer()
        startScanning()

        // Wait briefly for scan results before connecting
        delay(2000)
        connectToAllScannedDevices()

        // Broadcast our public key so all peers can set up E2E
        val publicKey = cryptoManager.getOrCreatePublicKey()
        meshRouter.broadcastKeyExchange(localPeerId, publicKey)
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

    override suspend fun sendMessage(message: com.meshlink.domain.model.Message, chatName: String) {
        meshMessagingManager.sendMessage(message, chatName)
    }

    override suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        meshMessagingManager.sendImage(targetMeshId, imageUri, chatName)
    }

    override suspend fun sendDocument(targetMeshId: String, documentUri: Uri, chatName: String) {
        meshMessagingManager.sendDocument(targetMeshId, documentUri, chatName)
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
