package com.meshlink.ble.data.handlers

import android.util.Base64
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.ble.data.BleConnectionManager
import com.meshlink.ble.data.RoutingCoordinator
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PeerConnectionState
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.KeyExchangeReplayCache
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.MeshSecurityMonitor
import com.meshlink.security.data.SecurityConstants
import com.meshlink.security.data.SecurityEvent
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import com.meshlink.util.MeshIdNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyExchangeHandler @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager,
    private val trustManager: TrustManager,
    private val keyExchangeReplayCache: KeyExchangeReplayCache,
    private val securityMonitor: MeshSecurityMonitor,
    private val routingCoordinator: RoutingCoordinator,
    private val connectionManager: BleConnectionManager,
    private val userRepository: UserRepository,
    private val packetDispatcher: PacketDispatcher,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope
) {
    private val TAG = "KeyExchangeHandler"

    // Callback used by MeshMessagingManager to retry messages after successful key exchange
    var onKeyExchangeComplete: (suspend () -> Unit)? = null

    suspend fun handleKeyExchange(packet: MeshPacket) {
        try {
            val parts = packet.payload.split("|")
            val isV3 = parts.isNotEmpty() && parts[0] == "v3"
            val isV2 = parts.size >= 5 && parts[0] == "v2"

            if (isV3) {
                handleV3KeyExchange(packet, parts)
            } else if (isV2) {
                handleV2KeyExchange(packet, parts)
            } else {
                MeshLogger.w(TAG, "Rejecting legacy unauthenticated key exchange from ${packet.senderId} (Downgrade protection)")
                val address = routingCoordinator.resolvePeerAddress(packet.senderId)
                if (address != null) disconnectDevice(address)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to handle KEY_EXCHANGE: ${e.message}")
        }
    }

    private suspend fun handleV3KeyExchange(packet: MeshPacket, parts: List<String>) {
        if (parts.size < 10) {
            MeshLogger.e(TAG, "Invalid v3 KEY_EXCHANGE format")
            return
        }
        
        val peerMinProtocol = parts[1].toIntOrNull() ?: 2
        val peerMaxProtocol = parts[2].toIntOrNull() ?: 2
        val peerCryptoVersion = parts[3].toIntOrNull() ?: 1
        val peerFeatures = parts[4]
        val ecdhPublicKey = parts[5]
        val timestamp = parts[6].toLongOrNull() ?: 0L
        val nonce = parts[7]
        val signatureBase64 = parts[8]
        val signingPublicKey = parts[9]

        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (Math.abs(diff) > SecurityConstants.KEY_EXCHANGE_WINDOW_MS) {
            MeshLogger.e(TAG, "KEY_EXCHANGE timestamp expired or invalid (diff = $diff ms)")
            securityMonitor.reportEvent(packet.senderId, SecurityEvent.ReplayAttackDetected("Expired timestamp v3"))
            return
        }

        if (keyExchangeReplayCache.isReplay(packet.senderId, nonce)) {
            securityMonitor.reportEvent(packet.senderId, SecurityEvent.ReplayAttackDetected("Duplicate nonce v3"))
            return
        }

        val existingSession = sessionManager.getSession(packet.senderId)
        if (existingSession != null) {
            val currentSigningKey = cryptoManager.getPeerSigningKey(packet.senderId)
            if (currentSigningKey != null && currentSigningKey != signingPublicKey) {
                keyExchangeReplayCache.resetRateLimit(packet.senderId)
            } else if (!keyExchangeReplayCache.canProcessHandshake(packet.senderId)) {
                MeshLogger.w(TAG, "Ignoring redundant handshake from ${packet.senderId} (Rate Limited)")
                return
            }
        }

        if (peerMaxProtocol < 2) {
            MeshLogger.e(TAG, "Rejecting KEY_EXCHANGE: peer max protocol too low")
            return
        }

        val dataToVerify = "${packet.packetId}|$peerMinProtocol|$peerMaxProtocol|$peerCryptoVersion|$peerFeatures|$ecdhPublicKey|$timestamp|$nonce".toByteArray(Charsets.UTF_8)
        val sigBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
        if (!cryptoManager.verifySignature(signingPublicKey, dataToVerify, sigBytes)) {
            MeshLogger.e(TAG, "KEY_EXCHANGE signature verification failed")
            securityMonitor.reportEvent(packet.senderId, SecurityEvent.InvalidSignature("KEY_EXCHANGE_V3"))
            return
        }

        val fingerprint = cryptoManager.getDeviceFingerprint(signingPublicKey)
        trustManager.updatePeerIdentity(packet.senderId, fingerprint, null)
        val trustLevel = trustManager.getTrustLevel(packet.senderId)
        if (trustLevel == TrustLevel.BLOCKED || trustLevel == TrustLevel.REVOKED) {
            MeshLogger.w(TAG, "Rejecting KEY_EXCHANGE from rogue node ${packet.senderId}")
            val address = routingCoordinator.resolvePeerAddress(packet.senderId)
            if (address != null) disconnectDevice(address)
            return
        }

        val negotiatedProtocol = Math.max(2, Math.min(3, peerMaxProtocol))
        trustManager.updateHighestProtocol(packet.senderId, negotiatedProtocol)

        cryptoManager.storePeerPublicKey(packet.senderId, ecdhPublicKey)
        cryptoManager.storePeerSigningKey(packet.senderId, signingPublicKey)
        
        sessionManager.createSession(
            peerId = packet.senderId,
            fingerprint = fingerprint,
            sessionVersion = negotiatedProtocol,
            cryptoVersion = peerCryptoVersion,
            verified = true
        )

        keyExchangeReplayCache.recordNonce(packet.senderId, nonce)
        keyExchangeReplayCache.recordHandshake(packet.senderId)

        MeshLogger.d(TAG, "🔐 SECURE (v3) Key exchanged with: ${MeshIdNormalizer.canonicalize(packet.senderId)} [Proto: $negotiatedProtocol]")

        val address = routingCoordinator.resolvePeerAddress(packet.senderId)
        if (address != null) {
            connectionManager.updatePeerState(address, PeerConnectionState.SESSION_ESTABLISHED)
            applicationScope.launch { onKeyExchangeComplete?.invoke() }
        }

        var isResponse = false
        if (parts.size >= 11 && parts[10] == "resp") {
            isResponse = true
        }

        if (!isResponse) {
            applicationScope.launch {
                userRepository.getLocalUser()?.let { user ->
                    val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
                    if (packet.senderId != localPeerId && packet.senderId.isNotBlank()) {
                        val responseKeyEx = generateSignedKeyExchange(localPeerId, isResponse = true).copy(targetId = packet.senderId)
                        packetDispatcher.dispatchSinglePacket(packet.senderId, responseKeyEx)
                    }
                }
            }
        }
    }

    private suspend fun handleV2KeyExchange(packet: MeshPacket, parts: List<String>) {
        val highestProtocol = trustManager.getHighestProtocol(packet.senderId)
        if (highestProtocol > 2) {
            MeshLogger.e(TAG, "Rejecting v2 KEY_EXCHANGE from ${packet.senderId}: Downgrade attack detected (Expected v$highestProtocol)")
            securityMonitor.reportEvent(packet.senderId, SecurityEvent.DowngradeAttackDetected("Expected v$highestProtocol, got v2"))
            trustManager.decreaseTrustScore(packet.senderId, 50, "Protocol downgrade attempt")
            val address = routingCoordinator.resolvePeerAddress(packet.senderId)
            if (address != null) disconnectDevice(address)
            return
        }

        val ecdhPublicKey = parts[1]
        val timestamp = parts[2].toLong()
        val nonce = parts[3]
        val version = parts[4].toInt()
        val signatureBase64 = parts[5]
        val signingPublicKey = parts[6]

        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (Math.abs(diff) > SecurityConstants.KEY_EXCHANGE_WINDOW_MS) {
            MeshLogger.e(TAG, "KEY_EXCHANGE timestamp expired or invalid (diff = $diff ms)")
            securityMonitor.reportEvent(packet.senderId, SecurityEvent.ReplayAttackDetected("Expired timestamp v2"))
            return
        }

        if (keyExchangeReplayCache.isReplay(packet.senderId, nonce)) {
            securityMonitor.reportEvent(packet.senderId, SecurityEvent.ReplayAttackDetected("Duplicate nonce v2"))
            return
        }

        val existingSession = sessionManager.getSession(packet.senderId)
        if (existingSession != null) {
            val currentSigningKey = cryptoManager.getPeerSigningKey(packet.senderId)
            if (currentSigningKey != null && currentSigningKey != signingPublicKey) {
                keyExchangeReplayCache.resetRateLimit(packet.senderId)
            } else if (!keyExchangeReplayCache.canProcessHandshake(packet.senderId)) {
                MeshLogger.w(TAG, "Ignoring redundant handshake from ${packet.senderId} (Rate Limited)")
                return
            }
        }

        val dataToVerify = "${packet.packetId}|$ecdhPublicKey|$timestamp|$nonce|$version".toByteArray(Charsets.UTF_8)
        val sigBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
        if (!cryptoManager.verifySignature(signingPublicKey, dataToVerify, sigBytes)) {
            MeshLogger.e(TAG, "KEY_EXCHANGE signature verification failed")
            securityMonitor.reportEvent(packet.senderId, SecurityEvent.InvalidSignature("KEY_EXCHANGE"))
            return
        }

        val fingerprint = cryptoManager.getDeviceFingerprint(signingPublicKey)
        trustManager.updatePeerIdentity(packet.senderId, fingerprint, null)
        val trustLevel = trustManager.getTrustLevel(packet.senderId)
        if (trustLevel == TrustLevel.BLOCKED || trustLevel == TrustLevel.REVOKED) {
            MeshLogger.w(TAG, "Rejecting KEY_EXCHANGE from rogue node ${packet.senderId}")
            val address = routingCoordinator.resolvePeerAddress(packet.senderId)
            if (address != null) disconnectDevice(address)
            return
        }

        trustManager.updateHighestProtocol(packet.senderId, 2)
        cryptoManager.storePeerPublicKey(packet.senderId, ecdhPublicKey)
        cryptoManager.storePeerSigningKey(packet.senderId, signingPublicKey)
        
        sessionManager.createSession(
            peerId = packet.senderId,
            fingerprint = fingerprint,
            sessionVersion = version,
            cryptoVersion = 1,
            verified = true
        )

        keyExchangeReplayCache.recordNonce(packet.senderId, nonce)
        keyExchangeReplayCache.recordHandshake(packet.senderId)

        MeshLogger.d(TAG, "🔐 SECURE (v2) Key exchanged with: ${MeshIdNormalizer.canonicalize(packet.senderId)}")

        val address = routingCoordinator.resolvePeerAddress(packet.senderId)
        if (address != null) {
            connectionManager.updatePeerState(address, PeerConnectionState.SESSION_ESTABLISHED)
            applicationScope.launch { onKeyExchangeComplete?.invoke() }
        }

        var isResponse = false
        if (parts.size >= 8 && parts[7] == "resp") {
            isResponse = true
        }

        if (!isResponse) {
            applicationScope.launch {
                userRepository.getLocalUser()?.let { user ->
                    val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
                    if (packet.senderId != localPeerId && packet.senderId.isNotBlank()) {
                        val responseKeyEx = generateSignedKeyExchange(localPeerId, isResponse = true).copy(targetId = packet.senderId)
                        packetDispatcher.dispatchSinglePacket(packet.senderId, responseKeyEx)
                    }
                }
            }
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
        val uuid = UUID.randomUUID().toString()
        
        val minProtocol = 2
        val maxProtocol = 3
        val cryptoVersion = 2
        val supportedFeatures = "SECURE_CHAT,VOICE,MEDIA"

        val dataToSign = "$uuid|$minProtocol|$maxProtocol|$cryptoVersion|$supportedFeatures|$ecdhPublicKey|$timestamp|$nonce".toByteArray(Charsets.UTF_8)
        val signature = cryptoManager.sign(dataToSign)
        val signatureBase64 = Base64.encodeToString(signature, Base64.NO_WRAP)

        val respTag = if (isResponse) "|resp" else ""
        val payload = "v3|$minProtocol|$maxProtocol|$cryptoVersion|$supportedFeatures|$ecdhPublicKey|$timestamp|$nonce|$signatureBase64|$signingPublicKey$respTag"
        
        return MeshPacket(
            packetId = uuid,
            senderId = localPeerId,
            targetId = "",
            payload = payload,
            type = PacketType.KEY_EXCHANGE,
            encrypted = false
        )
    }
}
