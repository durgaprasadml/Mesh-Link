package com.meshlink.ble.data

import android.net.Uri
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.ble.data.handlers.AckManager
import com.meshlink.ble.data.handlers.BroadcastHandler
import com.meshlink.ble.data.handlers.KeyExchangeHandler
import com.meshlink.ble.data.handlers.LocationMessageHandler
import com.meshlink.ble.data.handlers.MediaMessageHandler
import com.meshlink.ble.data.handlers.TextMessageHandler
import com.meshlink.ble.data.handlers.VoiceMessageHandler
import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PeerConnectionState
import com.meshlink.domain.repository.UserRepository
import com.meshlink.messaging.api.MessageProcessor
import com.meshlink.routing.api.Router
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.transfer.TransferManager
import com.meshlink.util.MeshIdNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshMessagingManager @Inject constructor(
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val meshRouter: Router,
    private val cryptoManager: MeshCryptoManager,
    private val transferManager: TransferManager,
    private val connectionManager: BleConnectionManager,
    private val discoveryManager: DiscoveryManager,
    private val routingCoordinator: RoutingCoordinator,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope,
    
    // Core Dispatchers
    private val corePacketDispatcher: CorePacketDispatcher,
    private val incomingPacketDispatcher: IncomingPacketDispatcher,
    
    // Extracted Handlers
    private val keyExchangeHandler: KeyExchangeHandler,
    private val textMessageHandler: TextMessageHandler,
    private val mediaMessageHandler: MediaMessageHandler,
    private val voiceMessageHandler: VoiceMessageHandler,
    private val locationMessageHandler: LocationMessageHandler,
    private val broadcastHandler: BroadcastHandler,
    private val ackManager: AckManager,
    private val beaconHandler: com.meshlink.ble.data.handlers.BeaconHandler,
    private val retryCoordinator: com.meshlink.common.recovery.RetryCoordinator,
    private val stateMachine: com.meshlink.messaging.data.MessageStateMachine
) : MessageProcessor, PacketDispatcher by corePacketDispatcher {

    enum class MeshStartupState { STOPPED, STARTING, RUNNING }
    private val startupState = AtomicReference(MeshStartupState.STOPPED)

    private val TAG = "MeshMessagingManager"

    private val retryMutex = Mutex()
    private val lastKeyExchangeRequest = ConcurrentHashMap<String, Long>()
    private var beaconJob: kotlinx.coroutines.Job? = null

    init {
        setupTransferManager()
        
        keyExchangeHandler.onKeyExchangeComplete = {
            retryCoordinator.triggerEvent("key_exchange_complete")
            retryPendingMessages()
        }
    }

    private fun setupTransferManager() {
        transferManager.onSendPacket = { packet ->
            val reqEnc = userRepository.isEncryptionEnabled.first()
            val isDirect = routingCoordinator.isDirectlyConnected(packet.targetId)
            
            if (reqEnc && !isDirect) {
                val result = corePacketDispatcher.encryptAndWrapPayload(packet.payload, packet.targetId, true, packet.packetId)
                if (result != null) {
                    val (encryptedPayload, isEncrypted) = result
                    val securePacket = packet.copy(
                        payload = encryptedPayload,
                        encrypted = isEncrypted
                    )
                    meshRouter.sendMediaPacket(securePacket)
                } else {
                    MeshLogger.e(TAG, "Failed to encrypt media packet ${packet.packetId}")
                }
            } else {
                meshRouter.sendMediaPacket(packet)
            }
        }

        transferManager.onTransferCompleted = { session ->
            applicationScope.launch {
                mediaMessageHandler.receiveMediaMessage(
                    session.transferId,
                    session.filePath ?: "",
                    session.mimeType ?: "",
                    session.senderId
                )
            }
        }
    }

    suspend fun handleIncomingPacket(packet: MeshPacket) {
        incomingPacketDispatcher.dispatch(packet)
    }

    suspend fun retryPendingMessages() {
        retryMutex.withLock {
            val pending = chatDao.getMessagesByStatus(DeliveryStatus.QUEUED)
            if (pending.isEmpty()) return

            connectToAllScannedDevices()
            if (!isAnyPeerConnected()) return

            MeshLogger.d(TAG, "Retrying ${pending.size} pending messages...")
            pending.forEach { msg ->
                if (!routingCoordinator.hasDeliveryPath(msg.chatId)) {
                    return@forEach
                }
                
                val reqEncCheck = userRepository.isEncryptionEnabled.first()
                if (reqEncCheck && !cryptoManager.hasPeerKey(msg.chatId)) {
                    val now = System.currentTimeMillis()
                    val lastReq = lastKeyExchangeRequest[msg.chatId] ?: 0L
                    if (now - lastReq > 10_000L) {
                        MeshLogger.w(TAG, "Missing key for ${msg.chatId}, requesting key exchange and postponing retry")
                        val localUser = userRepository.getLocalUser()
                        if (localUser != null) {
                            val localPeerId = MeshIdNormalizer.canonicalize(localUser.meshId)
                            val packetBase = keyExchangeHandler.generateSignedKeyExchange(localPeerId, isResponse = false)
                            val packet = packetBase.copy(targetId = msg.chatId)
                            dispatchSinglePacket(msg.chatId, packet)
                            lastKeyExchangeRequest[msg.chatId] = now
                        }
                    } else {
                        MeshLogger.d(TAG, "Missing key for ${msg.chatId}, but key exchange recently requested. Waiting...")
                    }
                    return@forEach
                }
                
                when (msg.messageType) {
                    MessageType.TEXT -> {
                        val user = userRepository.getLocalUser() ?: return@forEach
                        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
                        val wrappedPayload = JSONObject().apply {
                            put("text", msg.text)
                            put("senderName", user.name)
                        }.toString()
                        
                        val packet = MeshPacket(
                            packetId = msg.messageId,
                            senderId = localPeerId,
                            targetId = msg.chatId,
                            payload = wrappedPayload,
                            type = PacketType.TEXT,
                            encrypted = false
                        )
                        
                        val result = dispatchSinglePacket(msg.chatId, packet)
                        when (result) {
                            is com.meshlink.domain.model.DispatchResult.Queued -> {
                                // Handled by DeliveryTracker
                            }
                            is com.meshlink.domain.model.DispatchResult.NoPeers,
                            is com.meshlink.domain.model.DispatchResult.QueueFull,
                            is com.meshlink.domain.model.DispatchResult.Rejected,
                            is com.meshlink.domain.model.DispatchResult.Error -> {
                                stateMachine.transitionToWaitingForRoute(msg.messageId)
                            }
                        }
                    }
                    MessageType.IMAGE, MessageType.VOICE -> {
                        val file = msg.mediaPath?.let { File(it) }
                        if (file != null && file.exists()) {
                            val targetPeerId = MeshIdNormalizer.canonicalize(msg.chatId)
                            val localPeerId = MeshIdNormalizer.canonicalize(msg.senderId)
                            val priority = if (msg.messageType == MessageType.VOICE) com.meshlink.transfer.TransferPriority.HIGH else com.meshlink.transfer.TransferPriority.MEDIUM
                            transferManager.sendFile(
                                file = file,
                                senderId = localPeerId,
                                targetId = targetPeerId,
                                priority = priority,
                                transferId = msg.messageId
                            )
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
                        val packet = MeshPacket(
                            packetId = msg.messageId,
                            senderId = MeshIdNormalizer.canonicalize(msg.senderId),
                            targetId = msg.chatId,
                            payload = payloadJson,
                            type = PacketType.LOCATION,
                            encrypted = false
                        )
                        val result = dispatchSinglePacket(msg.chatId, packet)
                        when (result) {
                            is com.meshlink.domain.model.DispatchResult.Queued -> {}
                            is com.meshlink.domain.model.DispatchResult.NoPeers,
                            is com.meshlink.domain.model.DispatchResult.QueueFull,
                            is com.meshlink.domain.model.DispatchResult.Rejected,
                            is com.meshlink.domain.model.DispatchResult.Error -> {
                                stateMachine.transitionToWaitingForRoute(msg.messageId)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun isAnyPeerConnected(): Boolean {
        return connectionManager.connectedServers.isNotEmpty() || connectionManager.activeClients.isNotEmpty()
    }

    fun generateSignedKeyExchange(localPeerId: String, isResponse: Boolean = false): MeshPacket {
        return keyExchangeHandler.generateSignedKeyExchange(localPeerId, isResponse)
    }

    suspend fun receiveMediaMessage(completedTransferId: String, completedFilePath: String, completedMimeType: String, completedSenderId: String) {
        mediaMessageHandler.receiveMediaMessage(completedTransferId, completedFilePath, completedMimeType, completedSenderId)
    }

    fun connectToAllScannedDevices() {
        if (!discoveryManager.isScanning() && !discoveryManager.isAdvertising()) {
            return
        }

        val devices = discoveryManager.scannedDevices.value.values
        if (devices.isEmpty()) return

        devices.forEach { device ->
            try {
                connectionManager.connectToDevice(device.address, isManual = false)
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Auto-connect request failed for ${device.name}: ${e.message}")
            }
        }
    }

    fun dispatchMediaPackets(targetPeerId: String, packets: List<MeshPacket>): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        packets.forEach { pkt ->
            meshRouter.sendMediaPacket(pkt.copy(encrypted = false))
        }
        return true
    }

    fun startAdvertising(name: String, meshId: String) {
        discoveryManager.startAdvertising(name, meshId, 0x01)
    }

    fun stopAdvertising() {
        discoveryManager.stopAdvertising()
    }

    fun startScanning() {
        discoveryManager.startScanning()
    }

    fun stopScanning() {
        discoveryManager.stopScanning()
    }

    fun startServer() {
        connectionManager.startServer()
    }

    fun stopServer() {
        connectionManager.stopServer()
    }

    fun connectToDevice(address: String) {
        if (com.meshlink.ble.data.BleConstants.isBluetoothAddress(address)) {
            connectionManager.connectToDevice(address)
        } else {
            val resolved = routingCoordinator.resolvePeerAddress(address)
            if (resolved != null) {
                connectionManager.connectToDevice(resolved)
            } else {
                MeshLogger.w(TAG, "Cannot directly connect to $address - MAC unknown. Relying on mesh routing.")
            }
        }
    }

    fun connectToPeer(peerIdOrAddress: String): Boolean {
        val address = routingCoordinator.resolvePeerAddress(peerIdOrAddress) ?: return false
        return try {
            connectToDevice(address)
            true
        } catch (e: Exception) {
            MeshLogger.w(TAG, "connectToPeer failed for $peerIdOrAddress: ${e.message}")
            false
        }
    }

    suspend fun autoStartMesh() {
        if (!startupState.compareAndSet(MeshStartupState.STOPPED, MeshStartupState.STARTING)) {
            MeshLogger.d(TAG, "[MeshStartup] autoStartMesh ignored: current state is ${startupState.get()}")
            return
        }

        try {
            val user = userRepository.getLocalUser()
            if (user == null) {
                MeshLogger.w(TAG, "[MeshStartup] STARTUP_ABORTED_NO_PROFILE: No local profile found. Rolling back startupState to STOPPED.")
                startupState.set(MeshStartupState.STOPPED)
                return
            }

            MeshLogger.i(TAG, "[MeshStartup] PROFILE_FOUND: User=${user.name}, MeshID=${user.meshId}")
            val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
            meshRouter.localMeshId = localPeerId
            
            discoveryManager.startAdvertising(user.name, user.meshId, 0x01)
            MeshLogger.i(TAG, "[MeshStartup] BLE_ADVERTISER_STARTED")
            startServer()
            startScanning()
            MeshLogger.i(TAG, "[MeshStartup] BLE_SCANNER_STARTED")

            delay(2000)
            connectToAllScannedDevices()

            val keyExchangePacket = keyExchangeHandler.generateSignedKeyExchange(localPeerId).copy(targetId = "BROADCAST")
            dispatchSinglePacket("BROADCAST", keyExchangePacket)

            startupState.set(MeshStartupState.RUNNING)
            MeshLogger.i(TAG, "[MeshStartup] MESH_READY")

            beaconJob?.cancel()
            beaconJob = applicationScope.launch {
                while (startupState.get() == MeshStartupState.RUNNING) {
                    delay(20_000L)
                    try {
                        val current = userRepository.getLocalUser()
                        if (current != null && isAnyPeerConnected()) {
                            val beaconPkt = beaconHandler.generateBeaconPacket(current.meshId)
                            dispatchSinglePacket("BROADCAST", beaconPkt)
                        }
                    } catch (e: Exception) {
                        MeshLogger.w(TAG, "Periodic topology beacon error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "[MeshStartup] autoStartMesh failed: ${e.message}", e)
            startupState.set(MeshStartupState.STOPPED)
            stopMesh()
            throw e
        }
    }

    fun isOperational(): Boolean = startupState.get() == MeshStartupState.RUNNING

    suspend fun refreshMesh() {
        if (startupState.get() != MeshStartupState.RUNNING) {
            MeshLogger.d(TAG, "[MeshStartup] refreshMesh: Mesh not running (${startupState.get()}). Resetting state to STOPPED and starting.")
            startupState.set(MeshStartupState.STOPPED)
            autoStartMesh()
            return
        }

        MeshLogger.d(TAG, "refreshMesh: Checking mesh component health")
        val user = userRepository.getLocalUser() ?: return

        if (!discoveryManager.isAdvertising()) {
            MeshLogger.d(TAG, "refreshMesh: Restarting advertising")
            discoveryManager.startAdvertising(user.name, user.meshId, 0x01)
        }
        if (!discoveryManager.isScanning()) {
            MeshLogger.d(TAG, "refreshMesh: Restarting scanning")
            startScanning()
        }
        
        startServer()

        delay(1000)
        connectToAllScannedDevices()
    }

    fun stopMesh() {
        startupState.set(MeshStartupState.STOPPED)
        beaconJob?.cancel()
        beaconJob = null
        stopAdvertising()
        stopScanning()
        stopServer()
    }

    suspend fun sendMessage(targetMeshId: String, message: Message) {
        textMessageHandler.sendMessage(targetMeshId, message)
    }

    suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        mediaMessageHandler.sendImage(targetMeshId, imageUri, chatName)
    }

    suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
        voiceMessageHandler.sendVoiceNote(targetMeshId, filePath, durationMs, chatName)
    }

    suspend fun sendLocation(targetMeshId: String, chatName: String) {
        locationMessageHandler.sendLocation(targetMeshId, chatName)
    }

    suspend fun sendReadReceipts(chatId: String) {
        ackManager.sendReadReceipts(chatId)
    }

    suspend fun sendSos() {
        broadcastHandler.sendSos()
    }

    suspend fun broadcastMessage(messageText: String) {
        broadcastHandler.broadcastMessage(messageText)
    }

    fun checkAndTriggerHandshake(address: String) {
        val state = connectionManager.peerStates[address] ?: return
        if (state == PeerConnectionState.READY || state == PeerConnectionState.SESSION_READY) {
            val peerId = discoveryManager.scannedDevices.value.values.firstOrNull { it.address == address }?.meshId
                ?: meshRouter.routeTable.entries.firstOrNull { it.value.nextHop == address }?.key
                
            if (peerId != null) {
                applicationScope.launch {
                    val reqEnc = userRepository.isEncryptionEnabled.first()
                    if (reqEnc) {
                        if (cryptoManager.hasPeerKey(peerId)) {
                            connectionManager.updatePeerState(address, PeerConnectionState.SESSION_READY)
                            retryCoordinator.triggerEvent("session_ready")
                            retryPendingMessages()
                        } else {
                            val currentState = connectionManager.peerStates[address]
                            if (currentState != PeerConnectionState.KEY_EXCHANGE_STARTED &&
                                currentState != PeerConnectionState.SESSION_READY &&
                                currentState != PeerConnectionState.SESSION_ESTABLISHED) {
                                connectionManager.peerStates[address] = PeerConnectionState.KEY_EXCHANGE_STARTED
                                val user = userRepository.getLocalUser()
                                if (user != null) {
                                    val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
                                    val packetBase = keyExchangeHandler.generateSignedKeyExchange(localPeerId)
                                    val packet = packetBase.copy(targetId = peerId)
                                    dispatchSinglePacket(peerId, packet)
                                }
                            }
                        }
                    } else {
                        connectionManager.updatePeerState(address, PeerConnectionState.SESSION_READY)
                        retryPendingMessages()
                    }
                }
            }
        }
    }

    @Deprecated("Use processPacket instead", ReplaceWith("processPacket(packet)"))
    override suspend fun processIncomingPacket(packet: MeshPacket) {
        handleIncomingPacket(packet)
    }

    override suspend fun processPacket(packet: MeshPacket): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            handleIncomingPacket(packet)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.UnknownError("Failed to process packet", e))
        }
    }

    @Deprecated("Use sendMessage instead", ReplaceWith("sendMessage(destinationId, payload)"))
    override suspend fun sendOutgoingMessage(destinationId: String, payload: String) {
        throw UnsupportedOperationException("Use sendMessage with Message domain model")
    }

    override suspend fun sendMessage(destinationId: String, payload: String): com.meshlink.domain.model.MeshResult<Unit> {
        return com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.UnknownError("Use sendMessage with Message domain model"))
    }
}
