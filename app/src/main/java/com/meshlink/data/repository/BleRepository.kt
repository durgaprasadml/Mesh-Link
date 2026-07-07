package com.meshlink.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.meshlink.data.ble.BleAdvertiserManager
import com.meshlink.data.ble.BleConstants
import com.meshlink.data.ble.BleGattManager
import com.meshlink.data.ble.BleScannerManager
import com.meshlink.data.ble.MeshPacket
import com.meshlink.data.ble.MeshRouter
import com.meshlink.data.ble.PacketType
import com.meshlink.data.crypto.MeshCryptoManager
import com.meshlink.data.local.ChatDao
import com.meshlink.data.local.DeliveryStatus
import com.meshlink.data.local.MessageEntity
import com.meshlink.data.local.MessageType
import com.meshlink.data.location.LocationProvider
import com.meshlink.data.media.ImageCompressor
import com.meshlink.data.media.MediaTransferManager
import com.meshlink.util.NotificationHelper
import org.json.JSONObject
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepository @Inject constructor(
    private val scanner: BleScannerManager,
    private val advertiser: BleAdvertiserManager,
    private val gattManager: BleGattManager,
    val meshRouter: MeshRouter,
    private val chatDao: ChatDao,
    private val userRepository: UserRepository,
    private val mediaTransferManager: MediaTransferManager,
    private val locationProvider: LocationProvider,
    private val cryptoManager: MeshCryptoManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BleRepository"
    }

    val scannedDevices: StateFlow<Map<String, BleDevice>> = scanner.scannedDevices
    val incomingMeshPayloads: SharedFlow<Pair<String, MeshPacket>> = meshRouter.incomingPayloads
    val transferProgress = mediaTransferManager.transferProgress

    private fun networkId(peerId: String): String = BleConstants.toNetworkId(peerId)
    private fun normalizePeerId(peerIdOrAddress: String): String {
        if (BleConstants.isBluetoothAddress(peerIdOrAddress)) {
            val routedPeerId = meshRouter.routeTable.entries
                .firstOrNull { it.value == peerIdOrAddress }
                ?.key
            if (routedPeerId != null) return routedPeerId

            return scannedDevices.value.values
                .firstOrNull { it.address == peerIdOrAddress }
                ?.meshId
                ?: peerIdOrAddress
        }
        return networkId(peerIdOrAddress)
    }
    fun resolveChatId(peerIdOrAddress: String): String = normalizePeerId(peerIdOrAddress)
    private fun outgoingChatId(targetMeshId: String): String = normalizePeerId(targetMeshId)
    private fun incomingChatId(senderMeshId: String): String = normalizePeerId(senderMeshId)

    private fun resolvePeerAddress(peerIdOrAddress: String): String? {
        if (BleConstants.isBluetoothAddress(peerIdOrAddress)) return peerIdOrAddress

        val targetId = networkId(peerIdOrAddress)
        val routeAddress = meshRouter.routeTable[targetId]
        if (routeAddress != null && BleConstants.isBluetoothAddress(routeAddress)) {
            return routeAddress
        }

        return scannedDevices.value.values
            .firstOrNull { it.meshId == targetId }
            ?.address
    }

    init {
        // Wire MediaTransferManager so it can dispatch ACK/NACK/retried chunks via MeshRouter
        mediaTransferManager.onSendPacket = { packet ->
            meshRouter.sendMediaPacket(packet)
        }

        CoroutineScope(Dispatchers.IO).launch {
            incomingMeshPayloads.collect { (_, packet) ->
                try {
                    when (packet.type) {
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
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling packet: ${e.message}")
                }
            }
        }

        // Periodically retry sending PENDING messages if we are connected to anyone
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(15000)
                retryPendingMessages()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            scannedDevices.collect { devices ->
                if (devices.isNotEmpty()) {
                    retryPendingMessages()
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

        Log.d(TAG, "Retrying ${pending.size} pending messages...")
        pending.forEach { msg ->
            when (msg.messageType) {
                MessageType.TEXT -> {
                    val user = userRepository.getLocalUser() ?: return@forEach
                    val localPeerId = networkId(user.meshId)
                    val wrappedPayload = JSONObject().apply {
                        put("text", msg.text)
                        put("senderName", user.name)
                    }.toString()
                    val (payload, isEncrypted) = cryptoManager.encryptOrPassthrough(wrappedPayload, msg.chatId)
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
                    val (encPayload, isEnc) = cryptoManager.encryptOrPassthrough(payloadJson, msg.chatId)
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

    fun isAnyPeerConnected(): Boolean {
        return gattManager.connectedServers.isNotEmpty() || gattManager.activeClients.isNotEmpty()
    }

    /**
     * FIX ISSUE 2: Connect to ALL scanned devices to establish GATT links.
     * This ensures mesh relay works — e.g. if A sees B but not C,
     * A must have a GATT connection to B so packets relay through B to C.
     */
    private fun connectToAllScannedDevices() {
        scannedDevices.value.values.forEach { device ->
            try {
                if (!gattManager.activeClients.containsKey(device.address)) {
                    gattManager.connectToDevice(device.address)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auto-connect failed for ${device.name}: ${e.message}")
            }
        }
    }

    private fun hasDeliveryPath(targetPeerIdOrAddress: String): Boolean {
        val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
        val directReachable = resolvePeerAddress(targetPeerIdOrAddress) != null
        return directReachable || connectedNodes.isNotEmpty()
    }

    /**
     * FIX ISSUE 1 & 2: Dispatch text via mesh.
     * - Auto-connects to all scanned peers for relay
     * - Accepts packetId so retries use the same ID
     * - Only sends when delivery path exists
     */
    private fun dispatchTextMessage(
        targetPeerId: String,
        payload: String,
        localPeerId: String,
        encrypted: Boolean,
        packetId: String? = null
    ): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        if (!hasDeliveryPath(targetPeerId)) {
            Log.d(TAG, "No delivery path for text to $targetPeerId, keeping PENDING")
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

    /**
     * Decrypt a packet's payload if the encrypted flag is set.
     * Returns a copy of the packet with decrypted payload.
     */
    private fun decryptPacketPayload(packet: MeshPacket): MeshPacket {
        if (!packet.encrypted) return packet
        val decrypted = cryptoManager.decryptOrPassthrough(packet.payload, packet.senderId)
        return packet.copy(payload = decrypted)
    }

    /**
     * Handle incoming KEY_EXCHANGE: store the peer's ECDH public key.
     */
    private fun handleKeyExchange(packet: MeshPacket) {
        cryptoManager.storePeerPublicKey(packet.senderId, packet.payload)
        Log.d(TAG, "🔐 Key exchanged with: ${packet.senderId.takeLast(8)}")
    }

    // ────────── BLE Lifecycle ──────────

    fun startAdvertising(name: String, meshId: String) {
        advertiser.startAdvertising(name, meshId)
    }

    fun stopAdvertising() {
        advertiser.stopAdvertising()
    }

    fun startScanning() {
        scanner.startScanning()
    }

    fun stopScanning() {
        scanner.stopScanning()
    }

    fun startServer() {
        gattManager.startServer()
    }

    fun stopServer() {
        gattManager.stopServer()
    }

    fun connectToDevice(address: String) {
        gattManager.connectToDevice(address)
    }

    fun connectToPeer(peerIdOrAddress: String): Boolean {
        val address = resolvePeerAddress(peerIdOrAddress) ?: return false
        return try {
            connectToDevice(address)
            true
        } catch (e: Exception) {
            Log.w(TAG, "connectToPeer failed for $peerIdOrAddress: ${e.message}")
            false
        }
    }

    /**
     * Auto-start BLE advertising + scanning + GATT server.
     * FIX ISSUE 2: Also auto-connects to all scanned devices for mesh relay.
     */
    suspend fun autoStartMesh() {
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
    fun stopMesh() {
        stopAdvertising()
        stopScanning()
        stopServer()
    }

    // ────────── Text Messages (ENCRYPTED) ──────────

    suspend fun sendMessage(targetMeshId: String, messageText: String, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = networkId(user.meshId)
        val targetPeerId = outgoingChatId(targetMeshId)
        meshRouter.localMeshId = localPeerId

        // FIX ISSUE 2: Connect to target AND all scanned devices for mesh relay
        connectToPeer(targetMeshId)
        connectToAllScannedDevices()

        val chatId = targetPeerId
        val messageId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = localPeerId,
            text = messageText,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.PENDING,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)

        // Wrap text + sender name in JSON so receiver knows who we are
        val wrappedPayload = JSONObject().apply {
            put("text", messageText)
            put("senderName", user.name)
        }.toString()

        // Encrypt the payload
        val (payload, isEncrypted) = cryptoManager.encryptOrPassthrough(wrappedPayload, targetPeerId)
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
        NotificationHelper.showMessageNotification(context, packet.senderId, senderName, plaintext)
    }

    // ────────── Image Messages (ENCRYPTED metadata) ──────────

    suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
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
            Log.e(TAG, "sendImage: compression failed for $imageUri")
            return
        }
        Log.d(TAG, "sendImage: compressed to ${compressedBytes.size / 1000}KB")

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
        NotificationHelper.showMessageNotification(context, completed.senderId, senderName, previewText)
    }

    // ────────── Voice Notes (ENCRYPTED metadata) ──────────

    suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
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

    suspend fun sendLocation(targetMeshId: String, chatName: String) {
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
        val (encPayload, isEnc) = cryptoManager.encryptOrPassthrough(payloadJson, targetPeerId)

        val packet = MeshPacket(
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
    }

    // ────────── SOS Broadcast (UNENCRYPTED — emergency must be readable by all) ──────────

    suspend fun sendSos() {
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

    suspend fun broadcastMessage(messageText: String) {
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


    private suspend fun receiveSosMessage(packet: MeshPacket) {
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
