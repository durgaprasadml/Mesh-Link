package com.meshlink.ble.data.handlers

import android.content.Context
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.common.logger.MeshLogger
import com.meshlink.data.location.LocationProvider
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

import com.meshlink.database.data.local.UserDao
import com.meshlink.database.data.local.UserEntity
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class BroadcastHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val locationProvider: LocationProvider,
    private val router: Router,
    private val settingsRepository: com.meshlink.domain.repository.SettingsRepository? = null,
    private val cryptoManager: com.meshlink.security.data.MeshCryptoManager? = null,
    private val wifiDirectManagerProvider: javax.inject.Provider<com.meshlink.wifi.manager.WifiDirectManager>? = null
) {
    private val TAG = "BroadcastHandler"

    companion object {
        const val MAX_BROADCAST_PAYLOAD_BYTES = 500
        private const val MAX_REPLAY_CACHE_SIZE = 2048
        internal const val BROADCAST_REPLAY_WINDOW_MS = 15 * 60 * 1000L
        internal const val BROADCAST_MAX_FUTURE_SKEW_MS = 5 * 60 * 1000L
    }

    private val seenBroadcastKeys = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > MAX_REPLAY_CACHE_SIZE
            }
        }
    )
    private val latestSenderTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val activeSosAnnouncements = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun markBroadcastSeen(key: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        synchronized(seenBroadcastKeys) {
            val previous = seenBroadcastKeys[key]
            if (previous != null && nowMs - previous <= BROADCAST_REPLAY_WINDOW_MS) {
                return false
            }
            seenBroadcastKeys[key] = nowMs
            return true
        }
    }

    private fun isValidAndFreshSenderTimestamp(
        senderId: String,
        timestamp: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (timestamp <= 0L || timestamp > nowMs + BROADCAST_MAX_FUTURE_SKEW_MS) {
            return false
        }
        val maxSeen = latestSenderTimestamps[senderId]
        if (maxSeen != null && timestamp < maxSeen - BROADCAST_REPLAY_WINDOW_MS) {
            return false
        }
        latestSenderTimestamps.compute(senderId) { _, prev ->
            if (prev == null || timestamp > prev) timestamp else prev
        }
        return true
    }

    suspend fun sendSos(sosEventId: String = java.util.UUID.randomUUID().toString()) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        router.localMeshId = localPeerId

        val shareLocation = try {
            settingsRepository?.shareLiveLocation?.firstOrNull() ?: true
        } catch (_: Exception) { true }
        val customTemplate = try {
            settingsRepository?.emergencyTemplate?.firstOrNull()?.trim().orEmpty()
        } catch (_: Exception) { "" }

        val location = if (shareLocation) locationProvider.getCurrentLocation() else null
        val lat = if (shareLocation) (location?.latitude ?: 0.0) else 0.0
        val lng = if (shareLocation) (location?.longitude ?: 0.0) else 0.0
        val battery = location?.batteryPercent ?: locationProvider.getBatteryPercent()

        val rawName = user.name.trim()
        val displayName = if (!com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(rawName, localPeerId)) rawName else ""
        val timestamp = System.currentTimeMillis()

        val signingPubKey = try { cryptoManager?.getOrCreateSigningKey() } catch (_: Exception) { null }
        val signature = if (signingPubKey != null && cryptoManager != null) {
            try {
                val dataToSign = "$sosEventId:$localPeerId:$lat:$lng:$battery:$timestamp".toByteArray(Charsets.UTF_8)
                android.util.Base64.encodeToString(cryptoManager.sign(dataToSign), android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Failed to digitally sign SOS payload: ${e.message}")
                null
            }
        } else null

        val payloadJson = JSONObject().apply {
            put("sosEventId", sosEventId)
            put("lat", lat)
            put("lng", lng)
            put("battery", battery)
            put("timestamp", timestamp)
            put("senderName", displayName)
            if (signingPubKey != null && signature != null) {
                put("signingPubKey", signingPubKey)
                put("signature", signature)
            }
            if (customTemplate.isNotBlank()) {
                put("message", customTemplate)
            }
        }.toString()

        val packet = MeshPacket(
            packetId = sosEventId,
            senderId = localPeerId,
            targetId = "BROADCAST",
            payload = payloadJson,
            type = PacketType.SOS,
            priority = com.meshlink.domain.model.PacketPriority.CRITICAL,
            broadcastType = com.meshlink.domain.model.BroadcastType.SOS,
            encrypted = false,
            ttl = 1
        )
        router.routeMediaPacket(packet)

        val existing = chatDao.getMessageByUuid(sosEventId)
        if (existing == null) {
            val outgoingMsg = MessageEntity(
                messageId = sosEventId,
                chatId = localPeerId,
                senderId = localPeerId,
                text = "🚨 SOS EMERGENCY (Dispatched) — Lat: $lat, Lng: $lng — Battery: $battery%",
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                status = DeliveryStatus.SENT,
                messageType = MessageType.SOS,
                latitude = lat,
                longitude = lng,
                batteryPercent = battery
            )
            chatDao.insertMessageAndUpdateChat(outgoingMsg, "🚨 SOS Emergency")
        }
    }

    suspend fun receiveSosAnnouncement(announcement: com.meshlink.ble.data.SosAnnouncement, deviceAddress: String = "") {
        val canonicalSenderId = MeshIdNormalizer.canonicalize(announcement.senderMeshId)
        if (canonicalSenderId.isBlank()) {
            MeshLogger.w(TAG, "Rejected SOS announcement: invalid or blank sender ID")
            return
        }

        val nowMs = System.currentTimeMillis()
        val announcementTsMs = announcement.timestampSeconds * 1000L

        // 1. Freshness & replay protection
        if (announcementTsMs < nowMs - BROADCAST_REPLAY_WINDOW_MS) {
            MeshLogger.w(TAG, "Dropped stale SOS announcement from $canonicalSenderId (ts=$announcementTsMs older than replay window)")
            return
        }
        if (!isValidAndFreshSenderTimestamp(canonicalSenderId, announcementTsMs, nowMs)) {
            MeshLogger.w(TAG, "Dropped stale or future-skewed SOS announcement from $canonicalSenderId (ts=$announcementTsMs)")
            return
        }

        // 2. Deduplication check
        val sosIdHex = announcement.sosIdHex
        activeSosAnnouncements[canonicalSenderId] = sosIdHex
        val dedupKey = "sos-announcement:$canonicalSenderId:$sosIdHex"
        if (!markBroadcastSeen(dedupKey, nowMs)) {
            MeshLogger.d(TAG, "Dropped duplicate SOS announcement for $dedupKey")
            return
        }

        // 3. Check if full details already persisted
        val existing = chatDao.getMessageByUuid(sosIdHex) ?: chatDao.getMessageByUuid(canonicalSenderId)
        if (existing != null && existing.latitude != null && existing.longitude != null) {
            MeshLogger.d(TAG, "Full SOS details already recorded for $sosIdHex; skipping announcement")
            return
        }

        // 4. Resolve sender display name & trust status
        val isVerified = cryptoManager?.hasPeerKey(canonicalSenderId) == true
        val resolvedSenderName = userRepository.getUserDisplayName(canonicalSenderId)
        val warning = if (!isVerified) " (Unverified Sender)" else ""

        // 5. Persist Stage A alert in ChatDao
        if (existing == null) {
            val incomingMsg = MessageEntity(
                messageId = sosIdHex,
                chatId = canonicalSenderId,
                senderId = canonicalSenderId,
                text = "🚨 SOS EMERGENCY from $resolvedSenderName$warning — Direct Range Alert Announced. Awaiting full details via Wi-Fi Direct.",
                timestamp = announcementTsMs,
                isFromMe = false,
                status = DeliveryStatus.DELIVERED,
                messageType = MessageType.SOS,
                latitude = null,
                longitude = null,
                batteryPercent = null
            )
            chatDao.insertMessageAndUpdateChat(incomingMsg, "🚨 $resolvedSenderName")
        }

        // 6. Trigger short vibration notification (one-shot 250ms, user opens details manually)
        NotificationHelper.showSosNotification(
            context = context,
            sosEventId = sosIdHex,
            senderId = canonicalSenderId,
            senderName = resolvedSenderName,
            alertText = "🚨 SOS EMERGENCY from $resolvedSenderName$warning",
            latitude = null,
            longitude = null,
            batteryPercent = null
        )

        // 7. Stage B: Initiate Wi-Fi Direct discovery / connection to fetch full package
        try {
            wifiDirectManagerProvider?.get()?.let { wifiMgr ->
                wifiMgr.discoverPeers()
                wifiMgr.ensureConnected(canonicalSenderId, timeoutMs = 15000L, isInitiator = false)
            }
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Failed to initiate Wi-Fi Direct for full package retrieval: ${e.message}")
        }
    }

    suspend fun receiveSosMessage(packet: MeshPacket) {
        if (packet.packetId.isBlank() || !markBroadcastSeen("sos-pkt:${packet.packetId}")) {
            MeshLogger.w(TAG, "Dropped duplicate SOS packetId=${packet.packetId}")
            return
        }

        val json = try { JSONObject(packet.payload) } catch (_: Exception) { null }
        val sosEventId = json?.optString("sosEventId", packet.packetId)?.ifBlank { packet.packetId } ?: packet.packetId
        val canonicalSenderId = MeshIdNormalizer.canonicalize(packet.senderId)

        if (json != null && json.has("timestamp")) {
            val payloadTs = json.optLong("timestamp", -1L)
            if (!isValidAndFreshSenderTimestamp(canonicalSenderId, payloadTs)) {
                MeshLogger.w(TAG, "Dropped stale or future-skewed SOS broadcast from $canonicalSenderId (ts=$payloadTs)")
                return
            }
        }

        if (!markBroadcastSeen("sos-evt:$canonicalSenderId:$sosEventId")) {
            MeshLogger.w(TAG, "Dropped replayed SOS event $sosEventId from $canonicalSenderId")
            return
        }

        val placeholderId = activeSosAnnouncements.remove(canonicalSenderId)
        val existing = chatDao.getMessageByUuid(sosEventId)
            ?: placeholderId?.let { chatDao.getMessageByUuid(it) }
        val lat = json?.optDouble("lat", 0.0) ?: 0.0
        val lng = json?.optDouble("lng", 0.0) ?: 0.0
        val battery = json?.optInt("battery", -1) ?: -1
        val payloadSenderName = json?.optString("senderName", "")?.trim() ?: ""

        // Cryptographic identity & packet integrity verification
        if (cryptoManager != null && json != null && json.has("signingPubKey") && json.has("signature")) {
            val pubKey = json.getString("signingPubKey")
            val sigBase64 = json.getString("signature")
            val payloadTs = json.optLong("timestamp", 0L)
            val dataToVerify = "$sosEventId:$canonicalSenderId:$lat:$lng:$battery:$payloadTs".toByteArray(Charsets.UTF_8)
            val sigBytes = try { android.util.Base64.decode(sigBase64, android.util.Base64.NO_WRAP) } catch (_: Exception) { ByteArray(0) }
            val isSigValid = cryptoManager.verifySignature(pubKey, dataToVerify, sigBytes)
            if (!isSigValid) {
                MeshLogger.w(TAG, "Dropped SOS from $canonicalSenderId: digital signature verification failed")
                return
            }
        }

        if (payloadSenderName.isNotBlank() && !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(payloadSenderName, canonicalSenderId)) {
            userRepository.saveOrUpdatePeerProfile(packet.senderId, payloadSenderName)
        }

        val resolvedSenderName = userRepository.getUserDisplayName(packet.senderId)
        val chatId = canonicalSenderId

        if (existing != null) {
            if (existing.latitude != null && existing.longitude != null) {
                MeshLogger.w(TAG, "SOS message $sosEventId already persisted with coordinates; ignoring replay")
                return
            }
            // If already exists as placeholder from announcement or media, update details while preserving existing mediaPath
            if (existing.messageId != sosEventId) {
                chatDao.deleteMessage(existing.messageId)
            }
            val updatedMessage = existing.copy(
                messageId = sosEventId,
                text = "🚨 SOS EMERGENCY from $resolvedSenderName — Lat: $lat, Lng: $lng — Battery: $battery%",
                latitude = lat,
                longitude = lng,
                batteryPercent = battery,
                status = DeliveryStatus.DELIVERED
            )
            chatDao.insertMessageAndUpdateChat(updatedMessage, "🚨 $resolvedSenderName")

            val alertText = "🚨 SOS EMERGENCY: Lat $lat, Lng $lng — Battery: $battery%"
            NotificationHelper.showSosNotification(
                context = context,
                sosEventId = sosEventId,
                senderId = packet.senderId,
                senderName = resolvedSenderName,
                alertText = alertText,
                latitude = lat,
                longitude = lng,
                batteryPercent = battery
            )
            return
        }

        val message = MessageEntity(
            messageId = sosEventId,
            chatId = chatId,
            senderId = packet.senderId,
            text = "🚨 SOS EMERGENCY from $resolvedSenderName — Lat: $lat, Lng: $lng — Battery: $battery%",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, "🚨 $resolvedSenderName")

        val alertText = "🚨 SOS EMERGENCY: Lat $lat, Lng $lng — Battery: $battery%"
        NotificationHelper.showSosNotification(
            context = context,
            sosEventId = sosEventId,
            senderId = packet.senderId,
            senderName = resolvedSenderName,
            alertText = alertText,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
    }


    suspend fun broadcastMessage(messageText: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        router.localMeshId = localPeerId

        val rawName = user.name.trim()
        val displayName = if (!com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(rawName, localPeerId)) rawName else ""
        val cleanText = messageText.trim()

        val payloadJson = JSONObject().apply {
            put("senderName", displayName)
            put("text", cleanText)
        }.toString()

        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        if (payloadBytes.size > MAX_BROADCAST_PAYLOAD_BYTES) {
            throw IllegalArgumentException(
                "Broadcast message payload exceeds maximum capacity (${payloadBytes.size} bytes > $MAX_BROADCAST_PAYLOAD_BYTES bytes)"
            )
        }

        val packet = MeshPacket(
            senderId = localPeerId,
            targetId = "BROADCAST",
            payload = payloadJson,
            type = PacketType.TEXT,
            encrypted = false,
            ttl = 1
        )
        router.routeMediaPacket(packet)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = "BROADCAST",
            senderId = localPeerId,
            text = cleanText,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.SENT,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessage(message)
    }

    suspend fun receiveBroadcastTextMessage(packet: MeshPacket) {
        if (packet.packetId.isBlank() || !markBroadcastSeen("txt-pkt:${packet.packetId}")) {
            MeshLogger.w(TAG, "Dropped duplicate broadcast text packetId=${packet.packetId}")
            return
        }
        if (chatDao.getMessageByUuid(packet.packetId) != null) return

        val rawPayload = packet.payload
        val internalKeywords = setOf("KEY_EXCHANGE", "ACK", "RELAY", "ROUTING", "HANDSHAKE")
        val canonicalSenderId = MeshIdNormalizer.canonicalize(packet.senderId)

        var hasExplicitTimestamp = false
        val (rawText, payloadSenderName, payloadTimestamp) = try {
            val json = JSONObject(rawPayload)
            if (json.has("text")) {
                hasExplicitTimestamp = json.has("timestamp")
                Triple(
                    json.getString("text"),
                    json.optString("senderName", "").trim(),
                    json.optLong("timestamp", System.currentTimeMillis())
                )
            } else {
                MeshLogger.w(TAG, "Filtering out JSON protocol packet masquerading as broadcast text: $rawPayload")
                return
            }
        } catch (_: Exception) {
            Triple(rawPayload, "", System.currentTimeMillis())
        }

        val effectiveTimestamp = if (hasExplicitTimestamp) payloadTimestamp else packet.timestamp
        if (!isValidAndFreshSenderTimestamp(canonicalSenderId, effectiveTimestamp)) {
            MeshLogger.w(TAG, "Dropped stale or future-skewed broadcast text from $canonicalSenderId (ts=$effectiveTimestamp)")
            return
        }

        val cleanText = if (rawText.startsWith("[BROADCAST]")) {
            rawText.removePrefix("[BROADCAST]").trim()
        } else {
            rawText.trim()
        }

        if (cleanText.startsWith("v2|") || internalKeywords.contains(cleanText)) {
            MeshLogger.w(TAG, "Filtering out internal protocol packet from broadcast UI (after JSON extraction): $cleanText")
            return
        }

        if (!markBroadcastSeen("txt-sig:$canonicalSenderId:$effectiveTimestamp:$cleanText")) {
            MeshLogger.w(TAG, "Dropped replayed broadcast text signature from $canonicalSenderId")
            return
        }

        if (payloadSenderName.isNotBlank() && !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(payloadSenderName, canonicalSenderId)) {
            userRepository.saveOrUpdatePeerProfile(packet.senderId, payloadSenderName)
        }

        val resolvedSenderName = userRepository.getUserDisplayName(packet.senderId)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = "BROADCAST",
            senderId = packet.senderId,
            text = cleanText,
            timestamp = if (effectiveTimestamp > 0L) effectiveTimestamp else System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessage(message)
        NotificationHelper.showMessageNotification(context, packet.senderId, "📢 Broadcast from $resolvedSenderName", cleanText)
    }
}
