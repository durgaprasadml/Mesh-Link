package com.meshlink.ble.data

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.meshlink.ble.data.PeerConnectionState
import com.meshlink.common.logger.MeshLogger
import com.meshlink.data.location.LocationProvider
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.media.data.ImageCompressor
import com.meshlink.media.data.MediaTransferManager
import com.meshlink.routing.data.MeshRouter
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.util.NotificationHelper
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject


@Singleton
class MeshMessagingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val cryptoManager: MeshCryptoManager,
    private val meshRouter: MeshRouter,
    private val transferManager: com.meshlink.transfer.TransferManager,
    private val mediaTransferManager: com.meshlink.media.data.MediaTransferManager,
    private val wifiDirectManager: com.meshlink.wifi.data.WifiDirectManager,
    private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,
    private val locationProvider: LocationProvider,
    private val routingCoordinator: RoutingCoordinator,
    private val sessionManager: com.meshlink.security.data.SessionManager,
    private val trustManager: com.meshlink.security.data.TrustManager,
    private val rekeyManager: com.meshlink.security.data.RekeyManager,
    private val voiceTransport: com.meshlink.voice.transport.VoiceTransport,
    private val videoTransport: com.meshlink.video.transport.VideoTransport,
    private val connectionManager: BleConnectionManager,
    private val discoveryManager: DiscoveryManager
) {
    private val TAG = "MeshMessagingManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    suspend fun handleIncomingPacket(packet: MeshPacket) {
        if (packet.targetId == "BROADCAST") {
            // Broadcasts are not encrypted
            MeshLogger.d(TAG, "[DIAG-Stage8] handleIncomingPacket: BROADCAST packet type=${packet.type}")
        } else {
            val myMeshId = userRepository.getLocalUser()?.meshId
            val myNetworkId = if (myMeshId != null) routingCoordinator.networkId(myMeshId) else null
            val targetsMe = myNetworkId != null && packet.targetId == myNetworkId

            // ── DIAGNOSTIC Stage 8 ───────────────────────────────────────────────
            MeshLogger.d(TAG, "[DIAG-Stage8] ═══ handleIncomingPacket() guard ═══")
            MeshLogger.d(TAG, "[DIAG-Stage8]   packet.packetId (last-6) : '${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}'")
            MeshLogger.d(TAG, "[DIAG-Stage8]   packet.senderId           : '${packet.senderId}'")
            MeshLogger.d(TAG, "[DIAG-Stage8]   packet.targetId           : '${packet.targetId}'")
            MeshLogger.d(TAG, "[DIAG-Stage8]   RAW myMeshId              : '$myMeshId'")
            MeshLogger.d(TAG, "[DIAG-Stage8]   networkId(myMeshId)       : '$myNetworkId'  [take(8) = FIRST-8]")
            MeshLogger.d(TAG, "[DIAG-Stage8]   packet.targetId == myNetworkId  : $targetsMe")
            if (!targetsMe) {
                MeshLogger.w(TAG, "[DIAG-Stage8]   ⚠ GUARD FIRES — packet NOT for me, will RETURN")
                MeshLogger.w(TAG, "[DIAG-Stage8]   ⚠ targetId='${packet.targetId}'  networkId='$myNetworkId'  MISMATCH=${packet.targetId != myNetworkId}")
            } else {
                MeshLogger.d(TAG, "[DIAG-Stage8]   ✓ Guard passes — packet IS for me, continuing")
            }
            // ─────────────────────────────────────────────────────────────────────

            if (myMeshId != null && packet.targetId != routingCoordinator.networkId(myMeshId)) {
                // Not for me, just route it without decrypting
                MeshLogger.d(TAG, "Routing packet to ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.targetId)}")
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
                    transferManager.handleIncomingPacket(packet)
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
                PacketType.VOICE_SIGNAL,
                PacketType.VOICE_FRAME -> {
                    voiceTransport.handleIncomingPacket(packet)
                }
                PacketType.VIDEO_SIGNAL,
                PacketType.VIDEO_FRAME -> {
                    videoTransport.handleIncomingPacket(packet)
                }
                PacketType.BEACON,
                PacketType.INCIDENT_REPORT,
                PacketType.CHECK_IN,
                PacketType.FORM_SYNC,
                PacketType.RESOURCE_SYNC,
                PacketType.MAP_SYNC -> {
                    // Handled elsewhere or not needed right now
                }
                else -> {
                    // Fallback
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error handling packet: ${e.message}")
        }
    }

    private val retryMutex = kotlinx.coroutines.sync.Mutex()
    private val lastKeyExchangeRequest = java.util.concurrent.ConcurrentHashMap<String, Long>()

    suspend fun retryPendingMessages() {
        retryMutex.withLock {
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
                val now = System.currentTimeMillis()
                val lastReq = lastKeyExchangeRequest[msg.chatId] ?: 0L
                if (now - lastReq > 10_000L) {
                    MeshLogger.w(TAG, "Missing key for ${msg.chatId}, requesting key exchange and postponing retry")
                    val localUser = userRepository.getLocalUser()
                    if (localUser != null) {
                        val localPeerId = routingCoordinator.networkId(localUser.meshId)
                        val packetBase = generateSignedKeyExchange(localPeerId, isResponse = false)
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
                    val localPeerId = routingCoordinator.networkId(user.meshId)
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
                MessageType.IMAGE, MessageType.VOICE, MessageType.DOCUMENT -> {
                    val file = msg.mediaPath?.let { File(it) }
                    if (file != null && file.exists()) {
                        val targetPeerId = routingCoordinator.outgoingChatId(msg.chatId)
                        val localPeerId = routingCoordinator.networkId(msg.senderId)
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
                    val reqEnc = userRepository.isEncryptionEnabled.first()
                    val result = encryptAndWrapPayload(payloadJson, msg.chatId, reqEnc, msg.messageId)
                    if (result == null) return@forEach
                    val (encPayload, isEnc) = result
                    val packet = MeshPacket(
                        packetId = msg.messageId, // Use original messageId
                        senderId = routingCoordinator.networkId(msg.senderId),
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
    }

    fun isAnyPeerConnected(): Boolean {
        return connectionManager.connectedServers.isNotEmpty() || connectionManager.activeClients.isNotEmpty()
    }

    /**
     * FIX ISSUE 2: Connect to ALL scanned devices to establish GATT links.
     * This ensures mesh relay works — e.g. if A sees B but not C,
     * A must have a GATT connection to B so packets relay through B to C.
     */
    fun connectToAllScannedDevices() {
        discoveryManager.scannedDevices.value.values.forEach { device ->
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
    fun dispatchTextMessage(
        targetPeerId: String,
        payload: String,
        localPeerId: String,
        encrypted: Boolean,
        packetId: String?
    ): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        meshRouter.sendPayload(targetPeerId, payload, localPeerId, encrypted, packetId)
        return true
    }

    fun dispatchMediaPackets(targetPeerId: String, packets: List<MeshPacket>): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        packets.forEach { pkt ->
            meshRouter.sendMediaPacket(pkt.copy(encrypted = false))
        }
        return true
    }

    fun dispatchSinglePacket(targetPeerId: String, packet: MeshPacket): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
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
                    val address = routingCoordinator.resolvePeerAddress(packet.senderId)
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

                MeshLogger.d(TAG, "🔐 SECURE Key exchanged with: ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId)}")

                val address = routingCoordinator.resolvePeerAddress(packet.senderId)
                if (address != null) {
                    connectionManager.updatePeerState(address, PeerConnectionState.SESSION_ESTABLISHED)
                    scope.launch { retryPendingMessages() }
                }

                var isResponse = false
                if (parts.size >= 8 && parts[7] == "resp") {
                    isResponse = true
                }

                if (!isResponse) {
                    scope.launch {
                        userRepository.getLocalUser()?.let { user ->
                            val localPeerId = routingCoordinator.networkId(user.meshId)
                            if (packet.senderId != localPeerId && packet.senderId.isNotBlank()) {
                                val responseKeyEx = generateSignedKeyExchange(localPeerId, isResponse = true).copy(targetId = packet.senderId)
                                dispatchSinglePacket(packet.senderId, responseKeyEx)
                            }
                        }
                    }
                }
            } else {
                // Legacy unauthenticated key exchange
                cryptoManager.storePeerPublicKey(packet.senderId, packet.payload)
                MeshLogger.d(TAG, "🔐 LEGACY Key exchanged with: ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId)}")
                val address = routingCoordinator.resolvePeerAddress(packet.senderId)
                if (address != null) {
                    connectionManager.updatePeerState(address, PeerConnectionState.SESSION_READY)
                    scope.launch { retryPendingMessages() }
                }

                scope.launch {
                    userRepository.getLocalUser()?.let { user ->
                        val localPeerId = routingCoordinator.networkId(user.meshId)
                        if (packet.senderId != localPeerId && packet.senderId.isNotBlank()) {
                            val responseKeyEx = generateSignedKeyExchange(localPeerId, isResponse = true).copy(targetId = packet.senderId)
                            dispatchSinglePacket(packet.senderId, responseKeyEx)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to handle KEY_EXCHANGE: ${e.message}")
        }
    }

    private fun disconnectDevice(address: String) {
        connectionManager.disconnectFromDevice(address)
        connectionManager.updatePeerState(address, PeerConnectionState.DISCONNECTED)
    }

    fun generateSignedKeyExchange(localPeerId: String, isResponse: Boolean = false): MeshPacket {
        val ecdhPublicKey = cryptoManager.getOrCreatePublicKey()
        val signingPublicKey = cryptoManager.getOrCreateSigningKey()
        val timestamp = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString()
        val version = 2
        val uuid = UUID.randomUUID().toString()
        
        val dataToSign = "$uuid|$ecdhPublicKey|$timestamp|$nonce|$version".toByteArray(Charsets.UTF_8)
        val signature = cryptoManager.sign(dataToSign)
        val signatureBase64 = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)

        val respTag = if (isResponse) "|resp" else ""
        val payload = "v2|$ecdhPublicKey|$timestamp|$nonce|$version|$signatureBase64|$signingPublicKey$respTag"
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

    fun startAdvertising(name: String, meshId: String) {
        discoveryManager.startAdvertising(name, meshId, 0x01) // 0x01 = Routing Support
    }

    fun stopAdvertising() {
        discoveryManager.stopAdvertising()
    }

    fun startScanning() {
        discoveryManager.startScanning()
        // Start the intelligent engine loop
        // (Assuming BleScannerManager delegates this internally, but we can also trigger engine here)
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

    /**
     * Auto-start BLE advertising + scanning + GATT server.
     * FIX ISSUE 2: Also auto-connects to all scanned devices for mesh relay.
     */
    suspend fun autoStartMesh() {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
        meshRouter.localMeshId = localPeerId
        
        // Broadcast routing capabilities
        discoveryManager.startAdvertising(user.name, user.meshId, 0x01)
        startServer()
        startScanning()

        // Wait briefly for scan results before connecting
        delay(2000)
        connectToAllScannedDevices()

        // Broadcast our public key so all peers can set up E2E
        val keyExchangePacket = generateSignedKeyExchange(localPeerId).copy(targetId = "BROADCAST")
        dispatchSinglePacket("BROADCAST", keyExchangePacket)
    }

    /**
     * Stop all BLE operations.
     */
    fun stopMesh() {
        stopAdvertising()
        stopScanning()
        stopServer()
    }

    @VisibleForTesting
    fun cancelScope() {
        scope.cancel()
    }

    // ────────── Text Messages (ENCRYPTED) ──────────

    suspend fun sendMessage(targetMeshId: String, message: com.meshlink.domain.model.Message, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
        val targetPeerId = routingCoordinator.outgoingChatId(targetMeshId)
        meshRouter.localMeshId = localPeerId

        // ── DIAGNOSTIC Stage 3/4 ──────────────────────────────────────────────────
        MeshLogger.d(TAG, "[DIAG-Stage3/4] ═══ sendMessage() ═══")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]   RAW user.meshId           : '${user.meshId}'")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]   RAW targetMeshId          : '$targetMeshId'")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]   networkId(user.meshId)    : '$localPeerId'  [take(8) = FIRST-8]")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]   outgoingChatId(target)    : '$targetPeerId'  [takeLast(8) = LAST-8]")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]   meshRouter.localMeshId    : '${meshRouter.localMeshId}'")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]   Packet will be:")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]     senderId = '$localPeerId'")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]     targetId = '$targetPeerId'")
        MeshLogger.d(TAG, "[DIAG-Stage3/4]   MISMATCH? localMeshId('$localPeerId') == targetPeerId('$targetPeerId'): ${localPeerId == targetPeerId}")
        // ─────────────────────────────────────────────────────────────────────────

        // FIX ISSUE 2: Connect to target AND all scanned devices for mesh relay
        connectToPeer(targetMeshId)
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
        MeshLogger.d(TAG, "[DIAG-Stage9] ═══ receiveMessage() ═══")
        MeshLogger.d(TAG, "[DIAG-Stage9]   packet.packetId (last-6): '${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}'")
        MeshLogger.d(TAG, "[DIAG-Stage9]   packet.senderId          : '${packet.senderId}'")
        MeshLogger.d(TAG, "[DIAG-Stage9]   packet.targetId          : '${packet.targetId}'")

        if (chatDao.getMessageByUuid(packet.packetId) != null) {
            MeshLogger.d(TAG, "[DIAG-Stage9]   Already processed (duplicate) — sending ACK only")
            // Already processed this message! Just send ACK in case it was lost.
            userRepository.getLocalUser()?.let { user ->
                val localPeerId = routingCoordinator.networkId(user.meshId)
                val ackPacket = MeshPacket(
                    senderId = localPeerId,
                    targetId = packet.senderId,
                    payload = packet.packetId,
                    type = PacketType.DELIVERY_ACK,
                    encrypted = false
                )
                dispatchSinglePacket(packet.senderId, ackPacket)
            }
            return
        }

        val chatId = routingCoordinator.incomingChatId(packet.senderId)

        // ── DIAGNOSTIC Stage 9 (DATABASE WRITE) ────────────────────────────────
        val myMeshId = userRepository.getLocalUser()?.meshId
        MeshLogger.d(TAG, "[DIAG-Stage9]   RAW myMeshId                      : '$myMeshId'")
        MeshLogger.d(TAG, "[DIAG-Stage9]   incomingChatId(packet.senderId)   : '$chatId'  [normalizePeerId = takeLast(8)]")
        MeshLogger.d(TAG, "[DIAG-Stage9]   DB insert: chatId='$chatId'  senderId='${packet.senderId}'")
        MeshLogger.d(TAG, "[DIAG-Stage9]   NOTE: UI queries chatId via resolveChatId(peer) = normalizePeerId(peer) = takeLast(8)")
        MeshLogger.d(TAG, "[DIAG-Stage9]   MATCH? chatId stored='$chatId' vs chatId queried by UI='${routingCoordinator.incomingChatId(packet.senderId)}'")
        // ─────────────────────────────────────────────────────────────────────

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
            val name = json.optString("senderName", com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId))
            text to name
        } catch (_: Exception) {
            // Legacy plain text packet
            rawPayload to com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId)
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
        MeshLogger.d(TAG, "[DIAG-Stage9]   Inserting MessageEntity: messageId=${com.meshlink.util.MeshIdNormalizer.canonicalize(message.messageId)} chatId=${message.chatId} senderId=${message.senderId}")
        chatDao.insertMessageAndUpdateChat(message, senderName)
        MeshLogger.d(TAG, "[DIAG-Stage9]   ✓ chatDao.insertMessageAndUpdateChat() called for chatId='${message.chatId}'")

        // FIX: Phase 3 - Send Delivery ACK
        userRepository.getLocalUser()?.let { user ->
            val localPeerId = routingCoordinator.networkId(user.meshId)
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

    suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
        val targetPeerId = routingCoordinator.outgoingChatId(targetMeshId)
        meshRouter.localMeshId = localPeerId

        // Connect to target and all mesh peers for relay
        connectToPeer(targetMeshId)
        connectToAllScannedDevices()

        // Compress image: max 480px, ≤80KB JPEG
        val compressedBytes = withContext(Dispatchers.IO) {
            ImageCompressor.compress(context, imageUri)
        }
        if (compressedBytes == null) {
            MeshLogger.e(TAG, "sendImage: compression failed for $imageUri")
            return
        }
        MeshLogger.d(TAG, "sendImage: compressed to ${compressedBytes.size / 1000}KB")

        val thumbnailBase64 = withContext(Dispatchers.IO) {
            ImageCompressor.generateThumbnailBase64(context, imageUri)
        }

        // Clean up temp camera file if applicable
        if (imageUri.scheme == "content" && imageUri.authority?.contains("fileprovider") == true) {
            try {
                val tempFile = File(context.cacheDir, "images/${imageUri.lastPathSegment}")
                if (tempFile.exists()) tempFile.delete()
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Failed to delete temp camera file")
            }
        }

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
            messageId       = messageId,
            chatId          = chatId,
            senderId        = localPeerId,
            text            = "📷 Image",
            timestamp       = System.currentTimeMillis(),
            isFromMe        = true,
            status          = DeliveryStatus.PENDING,
            messageType     = MessageType.IMAGE,
            mediaPath       = localFile.absolutePath,
            mimeType        = "image/jpeg",
            mediaSize       = localFile.length(),
            thumbnailBase64 = thumbnailBase64
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)
        transferManager.sendFile(
            file = localFile,
            senderId = localPeerId,
            targetId = targetPeerId,
            transferId = messageId
        )
    }

    suspend fun sendDocument(targetMeshId: String, documentUri: Uri, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
        val targetPeerId = routingCoordinator.outgoingChatId(targetMeshId)
        meshRouter.localMeshId = localPeerId

        // Connect to target and all mesh peers for relay
        connectToPeer(targetMeshId)
        connectToAllScannedDevices()

        // Read document bytes
        val documentBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(documentUri)?.use { it.readBytes() }
        }
        if (documentBytes == null) {
            MeshLogger.e(TAG, "sendDocument: failed to read $documentUri")
            return
        }

        // Try to get filename
        var fileName = "doc_${System.currentTimeMillis()}.file"
        try {
            context.contentResolver.query(documentUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) fileName = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {}

        // Save local copy
        val localFile = withContext(Dispatchers.IO) {
            val mediaDir = File(context.filesDir, "mesh_documents")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            File(mediaDir, fileName).apply {
                writeBytes(documentBytes)
            }
        }

        val chatId = targetPeerId
        val messageId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId   = messageId,
            chatId      = chatId,
            senderId    = localPeerId,
            text        = "📄 $fileName",
            timestamp   = System.currentTimeMillis(),
            isFromMe    = true,
            status      = DeliveryStatus.PENDING,
            messageType = MessageType.DOCUMENT,
            mediaPath   = localFile.absolutePath
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)
        transferManager.sendFile(
            file = localFile,
            senderId = localPeerId,
            targetId = targetPeerId,
            transferId = messageId
        )
    }

    suspend fun receiveMediaMessage(completedTransferId: String, completedFilePath: String, completedMimeType: String, completedSenderId: String) {
        val isImage = completedMimeType.contains("image")
        val isVoice = completedMimeType.contains("audio")
        val isVideo = completedMimeType.contains("video")

        val messageType = when {
            isImage -> MessageType.IMAGE
            isVoice -> MessageType.VOICE
            else -> MessageType.DOCUMENT
        }

        val previewText = when {
            isImage -> "📷 Image"
            isVoice -> "🎤 Voice Note"
            else -> "📄 Document"
        }

        val chatId = routingCoordinator.incomingChatId(completedSenderId)
        val senderName = com.meshlink.util.MeshIdNormalizer.canonicalize(completedSenderId)

        val message = MessageEntity(
            messageId = completedTransferId,
            chatId = chatId,
            senderId = completedSenderId,
            text = previewText,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = messageType,
            mediaPath = completedFilePath
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
        
        // FIX: Phase 3 - Send Delivery ACK for Media
        userRepository.getLocalUser()?.let { user ->
            val localPeerId = routingCoordinator.networkId(user.meshId)
            val ackPacket = MeshPacket(
                senderId = localPeerId,
                targetId = completedSenderId,
                payload = completedTransferId,
                type = PacketType.DELIVERY_ACK,
                encrypted = false
            )
            dispatchSinglePacket(completedSenderId, ackPacket)
        }

        NotificationHelper.showMessageNotification(context, completedSenderId, senderName, previewText)
    }

    private suspend fun insertPlaceholderIncomingMedia(packet: MeshPacket) {
        val transferId = packet.transferId ?: return
        if (chatDao.getMessageByUuid(transferId) != null) return // Already have a placeholder or completed message

        val isImage = packet.mimeType?.contains("image") == true
        val isVoice = packet.mimeType?.contains("audio") == true

        val messageType = when {
            isImage -> MessageType.IMAGE
            isVoice -> MessageType.VOICE
            else -> MessageType.DOCUMENT
        }

        val previewText = when {
            isImage -> "📷 Receiving Image..."
            isVoice -> "🎤 Receiving Voice Note..."
            else -> "📄 Receiving Document..."
        }

        val chatId = routingCoordinator.incomingChatId(packet.senderId)
        val senderName = com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId)

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

    suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
        val targetPeerId = routingCoordinator.outgoingChatId(targetMeshId)
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
        transferManager.sendFile(
            file = File(filePath),
            senderId = localPeerId,
            targetId = targetPeerId,
            transferId = messageId,
            priority = com.meshlink.transfer.TransferPriority.HIGH
        )
    }

    // ────────── Location (ENCRYPTED GPS payload) ──────────

    suspend fun sendLocation(targetMeshId: String, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
        val targetPeerId = routingCoordinator.outgoingChatId(targetMeshId)
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
        if (chatDao.getMessageByUuid(packet.packetId) != null) {
            // Already processed this message! Just send ACK in case it was lost.
            userRepository.getLocalUser()?.let { user ->
                val localPeerId = routingCoordinator.networkId(user.meshId)
                val ackPacket = MeshPacket(
                    senderId = localPeerId,
                    targetId = packet.senderId,
                    payload = packet.packetId,
                    type = PacketType.DELIVERY_ACK,
                    encrypted = false
                )
                dispatchSinglePacket(packet.senderId, ackPacket)
            }
            return
        }

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
        val senderName = json.optString("senderName", com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId))

        val chatId = routingCoordinator.incomingChatId(packet.senderId)

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
            val localPeerId = routingCoordinator.networkId(user.meshId)
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

    suspend fun sendReadReceipts(chatId: String) {
        val unreadIds = chatDao.getUnreadIncomingMessages(chatId)
        if (unreadIds.isEmpty()) return

        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)

        // Mark as seen locally
        chatDao.markMessagesAsSeen(unreadIds)

        // Send READ_RECEIPT packets
        // The chatId is the target meshId (for direct chats)
        val targetPeerId = routingCoordinator.outgoingChatId(chatId)
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

    suspend fun sendSos() {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
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

    suspend fun broadcastMessage(messageText: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = routingCoordinator.networkId(user.meshId)
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
            json.optString("text", rawPayload) to json.optString("senderName", com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId))
        } catch (_: Exception) {
            rawPayload to com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId)
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

    fun checkAndTriggerHandshake(address: String) {
        val state = connectionManager.peerStates[address] ?: return
        if (state == com.meshlink.ble.data.PeerConnectionState.SERVICES_DISCOVERED || state == com.meshlink.ble.data.PeerConnectionState.MTU_READY) {
            val peerId = discoveryManager.scannedDevices.value.values.firstOrNull { it.address == address }?.meshId
                ?: meshRouter.routeTable.entries.firstOrNull { it.value.nextHop == address }?.key
                
            if (peerId != null) {
                scope.launch {
                    val reqEnc = userRepository.isEncryptionEnabled.first()
                    if (reqEnc) {
                        if (cryptoManager.hasPeerKey(peerId)) {
                            connectionManager.updatePeerState(address, com.meshlink.ble.data.PeerConnectionState.SESSION_READY)
                            retryPendingMessages()
                        } else {
                            val currentState = connectionManager.peerStates[address]
                            if (currentState != com.meshlink.ble.data.PeerConnectionState.KEY_EXCHANGE_STARTED &&
                                currentState != com.meshlink.ble.data.PeerConnectionState.SESSION_READY &&
                                currentState != com.meshlink.ble.data.PeerConnectionState.SESSION_ESTABLISHED) {
                                connectionManager.peerStates[address] = com.meshlink.ble.data.PeerConnectionState.KEY_EXCHANGE_STARTED
                                val user = userRepository.getLocalUser()
                                if (user != null) {
                                    val localPeerId = routingCoordinator.networkId(user.meshId)
                                    val packetBase = generateSignedKeyExchange(localPeerId)
                                    val packet = packetBase.copy(targetId = peerId)
                                    dispatchSinglePacket(peerId, packet)
                                }
                            }
                        }
                    } else {
                        connectionManager.updatePeerState(address, com.meshlink.ble.data.PeerConnectionState.SESSION_READY)
                        retryPendingMessages()
                    }
                }
            }
        }
    }

    private suspend fun receiveSosMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) return // Ignore duplicate

        val json = try { JSONObject(packet.payload) } catch (_: Exception) { return }
        val lat = json.optDouble("lat", 0.0)
        val lng = json.optDouble("lng", 0.0)
        val battery = json.optInt("battery", -1)
        val senderName = json.optString("senderName", com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId))

        val chatId = routingCoordinator.incomingChatId(packet.senderId)

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

    private suspend fun handleWifiNegotiation(packet: MeshPacket) {
        val json = try { JSONObject(packet.payload) } catch (_: Exception) { return }
        val peerMac = json.optString("wifiMac")
        if (peerMac.isNotEmpty()) {
            MeshLogger.d(TAG, "Received Wi-Fi Direct MAC from peer: $peerMac, initiating connect...")
            withContext(Dispatchers.Main) {
                wifiDirectManager.connectToPeer(peerMac)
            }
        }
    }
}
