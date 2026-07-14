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
import com.meshlink.ble.data.MeshPacket
import com.meshlink.ble.data.PacketType
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
import com.meshlink.media.data.MediaTransferManager
import com.meshlink.routing.data.MeshRouter
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.util.NotificationHelper
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
    override val meshRouter: MeshRouter,
    private val chatDao: ChatDao,
    private val userRepository: UserRepository,
    private val mediaTransferManager: MediaTransferManager,
    private val locationProvider: LocationProvider,
    private val cryptoManager: MeshCryptoManager,
    private val wifiDirectManager: WifiDirectManager,
    private val wifiSocketTransport: WifiSocketTransport,
    private val sessionManager: com.meshlink.security.data.SessionManager,
    private val rekeyManager: com.meshlink.security.data.RekeyManager,
    private val trustManager: com.meshlink.security.data.TrustManager,
    private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,
    @ApplicationContext private val context: Context
) : MeshRepository {
    companion object {
        private const val TAG = "MeshRepository"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val peerStates = ConcurrentHashMap<String, PeerConnectionState>()

    private fun updatePeerState(address: String, newState: PeerConnectionState) {
        val current = peerStates[address] ?: PeerConnectionState.DISCONNECTED
        peerStates[address] = newState
        MeshLogger.d(TAG, "Peer $address state: $current -> $newState")
        
        if (newState == PeerConnectionState.SERVICES_DISCOVERED || newState == PeerConnectionState.MTU_READY) {
            checkAndTriggerHandshake(address)
        }
    }

    private fun checkAndTriggerHandshake(address: String) {
        val state = peerStates[address] ?: return
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
                            val currentState = peerStates[address]
                            if (currentState != PeerConnectionState.KEY_EXCHANGE_STARTED) {
                                peerStates[address] = PeerConnectionState.KEY_EXCHANGE_STARTED
                                val user = userRepository.getLocalUser()
                                if (user != null) {
                                    val localPeerId = networkId(user.meshId)
                                    val packetBase = generateSignedKeyExchange(localPeerId)
                                    val packet = packetBase.copy(targetId = peerId)
                                    dispatchSinglePacket(peerId, packet)
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

    override val scannedDevices: StateFlow<Map<String, BleDevice>> = bleDataSource.scannedDevices
    override val incomingMeshPayloads: SharedFlow<Pair<String, MeshPacket>> = meshRouter.incomingPayloads
    override val transferProgress = mediaTransferManager.transferProgress

    private fun networkId(peerId: String): String = BleConstants.toNetworkId(peerId)
    private fun normalizePeerId(peerIdOrAddress: String): String {
        if (BleConstants.isBluetoothAddress(peerIdOrAddress)) {
            val routedPeerId = meshRouter.routeTable.entries
                .firstOrNull { it.value.nextHop == peerIdOrAddress }?.key
            if (routedPeerId != null) return routedPeerId

            return scannedDevices.value.values
                .firstOrNull { it.address == peerIdOrAddress }
                ?.meshId
                ?: peerIdOrAddress
        }
        return networkId(peerIdOrAddress)
    }
    override fun resolveChatId(peerIdOrAddress: String): String = normalizePeerId(peerIdOrAddress)
    private fun outgoingChatId(targetMeshId: String): String = normalizePeerId(targetMeshId)
    private fun incomingChatId(senderMeshId: String): String = normalizePeerId(senderMeshId)

    private fun resolvePeerAddress(peerIdOrAddress: String): String? {
        if (BleConstants.isBluetoothAddress(peerIdOrAddress)) return peerIdOrAddress

        val targetId = networkId(peerIdOrAddress)
        val routeAddress = meshRouter.routeTable[targetId]?.nextHop
        if (routeAddress != null && BleConstants.isBluetoothAddress(routeAddress)) {
            return routeAddress
        }

        return scannedDevices.value.values
            .firstOrNull { it.meshId == targetId }
            ?.address
    }

    
    override suspend fun setLocalMeshId(meshId: String) {
        meshRouter.localMeshId = meshId
    }

    init {
        // Wire RekeyManager
        rekeyManager.sendPacketCallback = { peerId, packet ->
            scope.launch {
                val user = userRepository.getLocalUser()
                val senderId = user?.let { networkId(it.meshId) } ?: ""
                dispatchSinglePacket(peerId, packet.copy(senderId = senderId))
            }
        }
        rekeyManager.forceKeyExchangeCallback = { peerId ->
            scope.launch {
                val address = resolvePeerAddress(peerId)
                if (address != null) {
                    peerStates[address] = PeerConnectionState.KEY_EXCHANGE_STARTED
                    val user = userRepository.getLocalUser()
                    if (user != null) {
                        val localPeerId = networkId(user.meshId)
                        val packetBase = generateSignedKeyExchange(localPeerId)
                        val packet = packetBase.copy(targetId = peerId)
                        dispatchSinglePacket(peerId, packet)
                    }
                }
            }
        }

        // Wire MediaTransferManager so it can dispatch ACK/NACK/retried chunks via MeshRouter
        mediaTransferManager.onSendPacket = { packet ->
            meshRouter.sendMediaPacket(packet)
        }

        // Wire WifiSocketTransport so it routes incoming packets just like MeshRouter
        wifiSocketTransport.onPacketReceived = { packet ->
            scope.launch {
                handleIncomingPacket(packet)
            }
        }

        scope.launch {
            incomingMeshPayloads.collect { (_, packet) ->
                handleIncomingPacket(packet)
            }
        }

        // Phase 4: Handle Media Transfer failures
        scope.launch {
            transferProgress.collect { progressMap ->
                progressMap.forEach { (transferId, progress) ->
                    if (progress < 0f) {
                        // Transfer failed (e.g. timeout)
                        chatDao.updateMessageStatus(transferId, DeliveryStatus.FAILED)
                    }
                }
            }
        }

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
    }

    private suspend fun handleIncomingPacket(packet: MeshPacket) {
        if (packet.targetId == "BROADCAST") {
            // Broadcasts are not encrypted
        } else {
            val myMeshId = userRepository.getLocalUser()?.meshId
            if (myMeshId != null && packet.targetId != networkId(myMeshId)) {
                // Not for me, just route it without decrypting
                MeshLogger.d(TAG, "Routing packet to ${packet.targetId.takeLast(8)}")
                return
            }
        }

        var processedPacket = packet

        if (packet.encrypted && packet.type != PacketType.KEY_EXCHANGE) {
            var finalPayload = packet.payload
            var validAad: ByteArray? = null

            var usePreviousKey = false

            if (finalPayload.startsWith("v2|")) {
                val unwrapped = sessionManager.validateAndUnwrap(packet.senderId, finalPayload)
                if (unwrapped == null) {
                    MeshLogger.w(TAG, "Dropping packet: session validation failed")
                    val address = resolvePeerAddress(packet.senderId)
                    if (address != null) checkAndTriggerHandshake(address)
                    return
                }
                validAad = unwrapped.first
                finalPayload = unwrapped.second
                val packetKv = unwrapped.third
                val session = sessionManager.getSession(packet.senderId)
                if (session != null && packetKv == session.previousKeyVersion) {
                    usePreviousKey = true
                }
            }

            val decrypted = cryptoManager.decryptOrPassthrough(finalPayload, packet.senderId, validAad, usePreviousKey)
            if (decrypted == finalPayload && !finalPayload.startsWith("{")) {
                MeshLogger.w(TAG, "Dropping packet: Failed to decrypt payload.")
                val address = resolvePeerAddress(packet.senderId)
                if (address != null) checkAndTriggerHandshake(address)
                return
            }
            trustManager.increaseTrustScore(packet.senderId, 1)
            processedPacket = packet.copy(payload = decrypted)
        }

        try {
            when (processedPacket.type) {
                PacketType.KEY_EXCHANGE -> {
                    handleKeyExchange(packet)
                }
                PacketType.TEXT -> {
                    if (packet.targetId == "BROADCAST") {
                        receiveBroadcastTextMessage(packet)
                    } else {
                        receiveMessage(packet)
                    }
                }
                PacketType.MEDIA_META,
                PacketType.MEDIA_CHUNK,
                PacketType.MEDIA_ACK,
                PacketType.MEDIA_NACK -> {
                    if (packet.type == PacketType.MEDIA_META && packet.transferId != null) {
                        insertPlaceholderIncomingMedia(packet)
                    }
                    val completed = mediaTransferManager.handleIncomingMediaPacket(packet)
                    if (completed != null) {
                        receiveMediaMessage(completed)
                    }
                }
                PacketType.LOCATION -> {
                    receiveLocationMessage(packet)
                }
                PacketType.SOS -> {
                    receiveSosMessage(packet)
                }
                PacketType.DELIVERY_ACK -> {
                    chatDao.updateMessageStatus(packet.payload, DeliveryStatus.DELIVERED)
                }
                PacketType.READ_RECEIPT -> {
                    chatDao.updateMessageStatus(packet.payload, DeliveryStatus.SEEN)
                }
                PacketType.WIFI_NEGOTIATION -> {
                    handleWifiNegotiation(packet)
                }
                PacketType.SESSION_REKEY -> {
                    rekeyManager.handleRekeyPacket(packet.senderId, packet.payload, cryptoManager.getPeerPublicKey(packet.senderId))
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error handling packet: ${e.message}")
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
                        if (dispatchMediaPackets(msg.chatId, packets)) {
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
                    if (dispatchSinglePacket(msg.chatId, packet)) {
                        chatDao.updateMessageStatus(msg.messageId, DeliveryStatus.SENT)
                    }
                }
                else -> {}
            }
        }
    }

    override fun isAnyPeerConnected(): Boolean {
        return bleDataSource.connectedServers.isNotEmpty() || bleDataSource.activeClients.isNotEmpty()
    }

    /**
     * FIX ISSUE 2: Connect to ALL scanned devices to establish GATT links.
     * This ensures mesh relay works — e.g. if A sees B but not C,
     * A must have a GATT connection to B so packets relay through B to C.
     */
    override fun connectToAllScannedDevices() {
        scannedDevices.value.values.forEach { device ->
            try {
                if (!bleDataSource.activeClients.contains(device.address)) {
                    bleDataSource.connectToDevice(device.address)
                }
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Auto-connect failed for ${device.name}: ${e.message}")
            }
        }
    }

    private fun hasDeliveryPath(targetPeerIdOrAddress: String): Boolean {
        val connectedNodes = bleDataSource.connectedServers + bleDataSource.activeClients
        val directReachable = resolvePeerAddress(targetPeerIdOrAddress) != null
        return directReachable || connectedNodes.isNotEmpty()
    }

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

    private fun dispatchMediaPackets(targetPeerId: String, packets: List<MeshPacket>): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        if (!hasDeliveryPath(targetPeerId)) return false
        packets.forEach { pkt ->
            meshRouter.sendMediaPacket(pkt.copy(encrypted = false))
        }
        return true
    }

    private fun dispatchSinglePacket(targetPeerId: String, packet: MeshPacket): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        if (!hasDeliveryPath(targetPeerId)) return false
        meshRouter.sendMediaPacket(packet)
        return true
    }

    // ────────── Crypto Helpers ──────────

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
        bleDataSource.disconnectFromDevice(address)
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
        bleDataSource.startAdvertising(name, meshId)
    }

    override fun stopAdvertising() {
        bleDataSource.stopAdvertising()
    }

    override fun startScanning() {
        bleDataSource.startScanning()
    }

    override fun stopScanning() {
        bleDataSource.stopScanning()
    }

    override fun startServer() {
        bleDataSource.startServer()
    }

    override fun stopServer() {
        bleDataSource.stopServer()
    }

    override fun connectToDevice(address: String) {
        bleDataSource.connectToDevice(address)
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
        startAdvertising(user.name, user.meshId)
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
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        val targetPeerId = outgoingChatId(message.chatId)
        meshRouter.localMeshId = localPeerId

        // FIX ISSUE 2: Connect to target AND all scanned devices for mesh relay
        connectToPeer(message.chatId)
        connectToAllScannedDevices()

        val messageId = message.messageId

        // Wrap text + sender name in JSON so receiver knows who we are
        val wrappedPayload = JSONObject().apply {
            put("text", message.text)
            put("senderName", user.name)
        }.toString()

        // Encrypt the payload
        val reqEnc = userRepository.isEncryptionEnabled.first()
        val result = encryptAndWrapPayload(wrappedPayload, targetPeerId, reqEnc, messageId)
        if (result == null) return
        val (payload, isEncrypted) = result
        // FIX ISSUE 1: Pass messageId as packetId so retries use the same ID
        if (dispatchTextMessage(targetPeerId, payload, localPeerId, isEncrypted, messageId)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
        }
    }

    private suspend fun receiveMessage(packet: MeshPacket) {
        val chatId = incomingChatId(packet.senderId)

        // Decrypt if encrypted
        val rawPayload = if (packet.encrypted) {
            cryptoManager.decryptOrPassthrough(packet.payload, packet.senderId)
        } else {
            packet.payload
        }

        // Try to parse as JSON (new format with senderName), fall back to plain text
        val (plaintext, senderName) = try {
            val json = JSONObject(rawPayload)
            val text = json.optString("text", rawPayload)
            val name = json.optString("senderName", packet.senderId.takeLast(8))
            text to name
        } catch (_: Exception) {
            // Legacy plain text packet
            rawPayload to packet.senderId.takeLast(8)
        }

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = chatId,
            senderId = packet.senderId,
            text = plaintext,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)

        // FIX: Phase 3 - Send Delivery ACK
        userRepository.getLocalUser()?.let { user ->
            val localPeerId = networkId(user.meshId)
            val ackPacket = MeshPacket(
                senderId = localPeerId,
                targetId = packet.senderId,
                payload = packet.packetId,
                type = PacketType.DELIVERY_ACK,
                encrypted = false
            )
            dispatchSinglePacket(packet.senderId, ackPacket)
        }

        NotificationHelper.showMessageNotification(context, packet.senderId, senderName, plaintext)
    }

    // ────────── Image Messages (ENCRYPTED metadata) ──────────

    override suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        val targetPeerId = outgoingChatId(targetMeshId)
        meshRouter.localMeshId = localPeerId

        // Connect to target and all mesh peers for relay
        connectToPeer(targetMeshId)
        connectToAllScannedDevices()

        // Compress image: max 800px, ≤200KB JPEG
        val compressedBytes = withContext(Dispatchers.IO) {
            ImageCompressor.compress(context, imageUri)
        }
        if (compressedBytes == null) {
            MeshLogger.e(TAG, "sendImage: compression failed for $imageUri")
            return
        }
        MeshLogger.d(TAG, "sendImage: compressed to ${compressedBytes.size / 1000}KB")

        // Save local copy
        val localFile = withContext(Dispatchers.IO) {
            val mediaDir = File(context.filesDir, "mesh_media")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            File(mediaDir, "img_${System.currentTimeMillis()}.jpg").apply {
                writeBytes(compressedBytes)
            }
        }

        val chatId = targetPeerId
        val messageId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId   = messageId,
            chatId      = chatId,
            senderId    = localPeerId,
            text        = "📷 Image",
            timestamp   = System.currentTimeMillis(),
            isFromMe    = true,
            status      = DeliveryStatus.PENDING,
            messageType = MessageType.IMAGE,
            mediaPath   = localFile.absolutePath
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)

        // Phase 6: Use Wi-Fi Direct if available (high-speed data plane)
        if (wifiSocketTransport.isConnected() && wifiDirectManager.connectedPeerMac.value != null) {
            try {
                MeshLogger.d(TAG, "sendImage: Sending via Wi-Fi Direct socket...")
                val base64Data = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                
                val metaPacket = MeshPacket(
                    senderId = localPeerId,
                    targetId = targetPeerId,
                    payload = "MEDIA:image/jpeg",
                    type = PacketType.MEDIA_META,
                    transferId = messageId,
                    chunkIndex = 0,
                    totalChunks = 1,
                    mimeType = "image/jpeg",
                    encrypted = false,
                    ttl = 10
                )
                wifiSocketTransport.sendPacket(metaPacket)
                
                val packet = MeshPacket(
                    packetId = messageId,
                    senderId = localPeerId,
                    targetId = targetPeerId,
                    payload = base64Data,
                    type = PacketType.MEDIA_CHUNK,
                    transferId = messageId,
                    mimeType = "image/jpeg",
                    encrypted = false,
                    totalChunks = 1,
                    chunkIndex = 0
                )
                wifiSocketTransport.sendPacket(packet)
                chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
                return
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Wi-Fi Direct socket send failed, falling back to BLE: ${e.message}")
            }
        }

        // Use reliable createAndSendChunked (30ms inter-chunk delay, ACK tracking)
        withContext(Dispatchers.IO) {
            mediaTransferManager.createAndSendChunked(
                data       = compressedBytes,
                senderId   = localPeerId,
                targetId   = targetPeerId,
                mimeType   = "image/jpeg",
                transferId = messageId
            )
        }

        chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
    }

    private suspend fun receiveMediaMessage(completed: MediaTransferManager.CompletedTransfer) {
        val isImage = completed.mimeType.contains("image")
        val isVoice = completed.mimeType.contains("audio")

        val messageType = when {
            isImage -> MessageType.IMAGE
            isVoice -> MessageType.VOICE
            else -> MessageType.TEXT
        }

        val previewText = when {
            isImage -> "📷 Image"
            isVoice -> "🎤 Voice Note"
            else -> "📎 File"
        }

        val chatId = incomingChatId(completed.senderId)
        val senderName = completed.senderId.takeLast(8)

        val message = MessageEntity(
            messageId = completed.transferId,
            chatId = chatId,
            senderId = completed.senderId,
            text = previewText,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = messageType,
            mediaPath = completed.filePath
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
        
        // FIX: Phase 3 - Send Delivery ACK for Media
        userRepository.getLocalUser()?.let { user ->
            val localPeerId = networkId(user.meshId)
            val ackPacket = MeshPacket(
                senderId = localPeerId,
                targetId = completed.senderId,
                payload = completed.transferId,
                type = PacketType.DELIVERY_ACK,
                encrypted = false
            )
            dispatchSinglePacket(completed.senderId, ackPacket)
        }

        NotificationHelper.showMessageNotification(context, completed.senderId, senderName, previewText)
    }

    private suspend fun insertPlaceholderIncomingMedia(packet: MeshPacket) {
        val transferId = packet.transferId ?: return
        if (chatDao.getMessageByUuid(transferId) != null) return // Already have a placeholder or completed message

        val isImage = packet.mimeType?.contains("image") == true
        val isVoice = packet.mimeType?.contains("audio") == true

        val messageType = when {
            isImage -> MessageType.IMAGE
            isVoice -> MessageType.VOICE
            else -> MessageType.TEXT
        }

        val previewText = when {
            isImage -> "📷 Receiving Image..."
            isVoice -> "🎤 Receiving Voice Note..."
            else -> "📎 Receiving File..."
        }

        val chatId = incomingChatId(packet.senderId)
        val senderName = packet.senderId.takeLast(8)

        val message = MessageEntity(
            messageId = transferId,
            chatId = chatId,
            senderId = packet.senderId,
            text = previewText,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.PENDING,
            messageType = messageType,
            mediaPath = null // Missing until complete
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
    }

    // ────────── Voice Notes (ENCRYPTED metadata) ──────────

    override suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        val targetPeerId = outgoingChatId(targetMeshId)
        meshRouter.localMeshId = localPeerId
        connectToPeer(targetMeshId)

        val voiceBytes = withContext(Dispatchers.IO) {
            val voiceFile = File(filePath)
            if (!voiceFile.exists()) return@withContext null
            voiceFile.readBytes()
        } ?: return

        val chatId = targetPeerId
        val messageId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = localPeerId,
            text = "🎤 Voice Note",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.PENDING,
            messageType = MessageType.VOICE,
            mediaPath = filePath,
            mediaDurationMs = durationMs
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)

        // Phase 6: Use Wi-Fi Direct if available (high-speed data plane)
        if (wifiSocketTransport.isConnected() && wifiDirectManager.connectedPeerMac.value != null) {
            try {
                MeshLogger.d(TAG, "sendVoiceNote: Sending via Wi-Fi Direct socket...")
                val base64Data = android.util.Base64.encodeToString(voiceBytes, android.util.Base64.NO_WRAP)
                
                val metaPacket = MeshPacket(
                    senderId = localPeerId,
                    targetId = targetPeerId,
                    payload = "MEDIA:audio/m4a",
                    type = PacketType.MEDIA_META,
                    transferId = messageId,
                    chunkIndex = 0,
                    totalChunks = 1,
                    mimeType = "audio/m4a",
                    encrypted = false,
                    ttl = 10
                )
                wifiSocketTransport.sendPacket(metaPacket)
                
                val packet = MeshPacket(
                    packetId = messageId,
                    senderId = localPeerId,
                    targetId = targetPeerId,
                    payload = base64Data,
                    type = PacketType.MEDIA_CHUNK,
                    transferId = messageId,
                    mimeType = "audio/m4a",
                    encrypted = false,
                    totalChunks = 1,
                    chunkIndex = 0
                )
                wifiSocketTransport.sendPacket(packet)
                chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
                return
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Wi-Fi Direct socket send failed, falling back to BLE: ${e.message}")
            }
        }

        val packets = withContext(Dispatchers.Default) {
            mediaTransferManager.createChunkedPackets(
                data = voiceBytes,
                senderId = localPeerId,
                targetId = targetPeerId,
                mimeType = "audio/m4a",
                transferId = messageId
            )
        }
        // Media chunks: do NOT encrypt — too heavy for BLE
        if (dispatchMediaPackets(targetPeerId, packets)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
        }
    }

    // ────────── Location (ENCRYPTED GPS payload) ──────────

    override suspend fun sendLocation(targetMeshId: String, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        val targetPeerId = outgoingChatId(targetMeshId)
        meshRouter.localMeshId = localPeerId
        connectToPeer(targetMeshId)

        val location = locationProvider.getCurrentLocation()
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val battery = location?.batteryPercent ?: locationProvider.getBatteryPercent()

        val payloadJson = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            put("battery", battery)
            put("timestamp", System.currentTimeMillis())
            put("senderName", user.name)
        }.toString()

        // Encrypt GPS coordinates
        val reqEnc = userRepository.isEncryptionEnabled.first()
        val generatedMessageId = java.util.UUID.randomUUID().toString()
        val result = encryptAndWrapPayload(payloadJson, targetPeerId, reqEnc, generatedMessageId)
        if (result == null) return
        val (encPayload, isEnc) = result

        val packet = MeshPacket(
            packetId = generatedMessageId,
            senderId = localPeerId,
            targetId = targetPeerId,
            payload = encPayload,
            type = PacketType.LOCATION,
            encrypted = isEnc
        )
        val locationDispatched = dispatchSinglePacket(targetPeerId, packet)

        val chatId = targetPeerId
        val messageId = packet.packetId
        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = localPeerId,
            text = "📍 Location: $lat, $lng",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = if (locationDispatched) DeliveryStatus.SENT else DeliveryStatus.PENDING,
            messageType = MessageType.LOCATION,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)
    }

    private suspend fun receiveLocationMessage(packet: MeshPacket) {
        // Decrypt GPS payload
        val rawPayload = if (packet.encrypted) {
            cryptoManager.decryptOrPassthrough(packet.payload, packet.senderId)
        } else {
            packet.payload
        }

        val json = try { JSONObject(rawPayload) } catch (_: Exception) { return }
        val lat = json.optDouble("lat", 0.0)
        val lng = json.optDouble("lng", 0.0)
        val battery = json.optInt("battery", -1)
        val senderName = json.optString("senderName", packet.senderId.takeLast(8))

        val chatId = incomingChatId(packet.senderId)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = chatId,
            senderId = packet.senderId,
            text = "📍 Location: $lat, $lng",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.LOCATION,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
        
        // FIX: Phase 3 - Send Delivery ACK for Location
        userRepository.getLocalUser()?.let { user ->
            val localPeerId = networkId(user.meshId)
            val ackPacket = MeshPacket(
                senderId = localPeerId,
                targetId = packet.senderId,
                payload = packet.packetId,
                type = PacketType.DELIVERY_ACK,
                encrypted = false
            )
            dispatchSinglePacket(packet.senderId, ackPacket)
        }
    }

    // ────────── Read Receipts ──────────

    override suspend fun sendReadReceipts(chatId: String) {
        val unreadIds = chatDao.getUnreadIncomingMessages(chatId)
        if (unreadIds.isEmpty()) return

        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)

        // Mark as seen locally
        chatDao.markMessagesAsSeen(unreadIds)

        // Send READ_RECEIPT packets
        // The chatId is the target meshId (for direct chats)
        val targetPeerId = outgoingChatId(chatId)
        if (targetPeerId == "BROADCAST") return // No read receipts for broadcasts

        unreadIds.forEach { msgId ->
            val receiptPacket = MeshPacket(
                senderId = localPeerId,
                targetId = targetPeerId,
                payload = msgId, // The ID of the message being marked as seen
                type = PacketType.READ_RECEIPT,
                encrypted = false
            )
            dispatchSinglePacket(targetPeerId, receiptPacket)
        }
    }

    // ────────── SOS Broadcast (UNENCRYPTED — emergency must be readable by all) ──────────

    override suspend fun sendSos() {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        meshRouter.localMeshId = localPeerId

        val location = locationProvider.getCurrentLocation()
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val battery = location?.batteryPercent ?: locationProvider.getBatteryPercent()

        val payloadJson = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            put("battery", battery)
            put("timestamp", System.currentTimeMillis())
            put("senderName", user.name)
        }.toString()

        // SOS is intentionally NOT encrypted — all nodes must be able to read it
        val packet = MeshPacket(
            senderId = localPeerId,
            targetId = "BROADCAST",
            payload = payloadJson,
            type = PacketType.SOS,
            encrypted = false,
            ttl = 15
        )
        meshRouter.sendMediaPacket(packet)
    }

    override suspend fun broadcastMessage(messageText: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        meshRouter.localMeshId = localPeerId

        val payloadJson = JSONObject().apply {
            put("text", "[BROADCAST] $messageText")
            put("senderName", user.name)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val packet = MeshPacket(
            senderId = localPeerId,
            targetId = "BROADCAST",
            payload = payloadJson,
            type = PacketType.TEXT,
            encrypted = false,
            ttl = 15
        )
        meshRouter.sendMediaPacket(packet)

        // FIX ERROR 3: Store sent broadcast in Room so BroadcastScreen can display it
        val messageId = packet.packetId
        val message = MessageEntity(
            messageId = messageId,
            chatId = "BROADCAST",
            senderId = localPeerId,
            text = "[BROADCAST] $messageText",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.SENT,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessage(message)
    }

    // FIX ERROR 3: Store received broadcast TEXT packets in the BROADCAST chatId
    private suspend fun receiveBroadcastTextMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) return // Ignore duplicate

        val rawPayload = packet.payload
        val (plaintext, senderName) = try {
            val json = JSONObject(rawPayload)
            json.optString("text", rawPayload) to json.optString("senderName", packet.senderId.takeLast(8))
        } catch (_: Exception) {
            rawPayload to packet.senderId.takeLast(8)
        }

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = "BROADCAST",
            senderId = packet.senderId,
            text = plaintext,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessage(message)
        NotificationHelper.showMessageNotification(context, packet.senderId, "📢 $senderName", plaintext)
    }
    
    private suspend fun handleWifiNegotiation(packet: MeshPacket) {
        // Automatically negotiate Wi-Fi Direct connection when receiving a peer's MAC
        val json = try { JSONObject(packet.payload) } catch (_: Exception) { return }
        val peerMac = json.optString("wifiMac")
        if (peerMac.isNotEmpty()) {
            MeshLogger.d(TAG, "Received Wi-Fi Direct MAC from peer: $peerMac, initiating connect...")
            withContext(Dispatchers.Main) {
                wifiDirectManager.connectToPeer(peerMac)
            }
        }
    }


    private suspend fun receiveSosMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) return // Ignore duplicate

        val json = try { JSONObject(packet.payload) } catch (_: Exception) { return }
        val lat = json.optDouble("lat", 0.0)
        val lng = json.optDouble("lng", 0.0)
        val battery = json.optInt("battery", -1)
        val senderName = json.optString("senderName", packet.senderId.takeLast(8))

        val chatId = incomingChatId(packet.senderId)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = chatId,
            senderId = packet.senderId,
            text = "🚨 SOS EMERGENCY from $senderName — Lat: $lat, Lng: $lng — Battery: $battery%",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, "🚨 $senderName")
    }
}
