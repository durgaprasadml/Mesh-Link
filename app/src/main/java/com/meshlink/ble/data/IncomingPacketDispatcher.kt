package com.meshlink.ble.data

import com.meshlink.ble.data.handlers.AckManager
import com.meshlink.ble.data.handlers.BroadcastHandler
import com.meshlink.ble.data.handlers.KeyExchangeHandler
import com.meshlink.ble.data.handlers.LocationMessageHandler
import com.meshlink.ble.data.handlers.MediaMessageHandler
import com.meshlink.ble.data.handlers.TextMessageHandler
import com.meshlink.ble.data.handlers.VoiceMessageHandler
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import com.meshlink.security.policy.PacketEncryptionPolicy
import com.meshlink.transfer.TransferManager
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.voice.transport.VoiceTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class IncomingPacketDispatcher @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager,
    private val trustManager: TrustManager,
    private val rekeyManager: RekeyManager,
    private val transferManager: TransferManager,
    private val voiceTransport: VoiceTransport,
    private val keyExchangeHandler: KeyExchangeHandler,
    private val textMessageHandler: TextMessageHandler,
    private val mediaMessageHandler: MediaMessageHandler,
    private val locationMessageHandler: LocationMessageHandler,
    private val voiceMessageHandler: VoiceMessageHandler,
    private val broadcastHandler: BroadcastHandler,
    private val ackManager: AckManager
) {
    private val TAG = "IncomingPacketDispatcher"

    suspend fun dispatch(packet: MeshPacket) {
        if (packet.targetId != "BROADCAST") {
            val myMeshId = userRepository.getLocalUser()?.meshId
            val myNetworkId = if (myMeshId != null) MeshIdNormalizer.canonicalize(myMeshId) else null

            if (myMeshId != null && packet.targetId != MeshIdNormalizer.canonicalize(myMeshId)) {
                MeshLogger.d(TAG, "Routing packet to ${MeshIdNormalizer.canonicalize(packet.targetId)}")
                return
            }
        }

        var processedPacket = packet

        // 1. Attempt decryption if packet is marked as encrypted or payload matches encrypted headers
        if (packet.encrypted || packet.payload.startsWith("bcast_v") || packet.payload.startsWith("v2|")) {
            var finalPayload = packet.payload

            if (finalPayload.startsWith("bcast_v")) {
                val parts = finalPayload.split("|", limit = 2)
                if (parts.size == 2) {
                    val versionStr = parts[0].removePrefix("bcast_v")
                    val version = versionStr.toIntOrNull() ?: 1
                    val ciphertext = parts[1]
                    val decrypted = cryptoManager.decryptBroadcast(ciphertext, version)
                    if (decrypted == null) {
                        MeshLogger.w(TAG, "Dropping broadcast packet: Failed broadcast key decryption (v$version)")
                        return
                    }
                    processedPacket = packet.copy(payload = decrypted, encrypted = false)
                }
            } else if (finalPayload.startsWith("v2|")) {
                val unwrapped = sessionManager.validateAndUnwrap(packet.senderId, finalPayload)
                if (unwrapped == null) {
                    MeshLogger.w(TAG, "Dropping packet: Session validation or AAD header check failed")
                    return
                }
                val validAad = unwrapped.first
                finalPayload = unwrapped.second
                val packetKv = unwrapped.third
                val session = sessionManager.getSession(packet.senderId)
                val usePreviousKey = session != null && packetKv == session.previousKeyVersion

                val decrypted = cryptoManager.decryptOrPassthrough(finalPayload, packet.senderId, validAad, usePreviousKey)
                if (decrypted == finalPayload && !finalPayload.startsWith("{")) {
                    MeshLogger.w(TAG, "Dropping packet: Failed to decrypt session payload.")
                    return
                }
                trustManager.increaseTrustScore(packet.senderId, 1)
                processedPacket = packet.copy(payload = decrypted, encrypted = false)
            } else if (packet.encrypted) {
                val decrypted = cryptoManager.decryptOrPassthrough(finalPayload, packet.senderId)
                if (decrypted == finalPayload && !finalPayload.startsWith("{")) {
                    MeshLogger.w(TAG, "Dropping packet: Failed to decrypt raw payload.")
                    return
                }
                processedPacket = packet.copy(payload = decrypted, encrypted = false)
            }
        }

        // 2. Validate central encryption policy AFTER decryption
        val hasSecureSession = sessionManager.getSession(processedPacket.senderId) != null
        val strictMode = settingsRepository.advancedEncryptionEnforcement.first()

        if (!PacketEncryptionPolicy.validatePacketEncryption(processedPacket, strictMode, hasSecureSession)) {
            MeshLogger.w(TAG, "Dropping packet ${processedPacket.packetId}: fails central encryption policy")
            return
        }

        // 3. Dispatch to appropriate handler
        try {
            when (processedPacket.type) {
                PacketType.KEY_EXCHANGE -> {
                    keyExchangeHandler.handleKeyExchange(processedPacket)
                }
                PacketType.TEXT -> {
                    if (processedPacket.targetId == "BROADCAST") {
                        broadcastHandler.receiveBroadcastTextMessage(processedPacket)
                    } else {
                        textMessageHandler.receiveMessage(processedPacket)
                    }
                }
                PacketType.MEDIA_META,
                PacketType.MEDIA_CHUNK,
                PacketType.MEDIA_ACK,
                PacketType.MEDIA_NACK -> {
                    if (processedPacket.type == PacketType.MEDIA_META && processedPacket.transferId != null) {
                        mediaMessageHandler.insertPlaceholderIncomingMedia(processedPacket)
                    }
                    transferManager.handleIncomingPacket(processedPacket)
                }
                PacketType.LOCATION -> {
                    locationMessageHandler.receiveLocationMessage(processedPacket)
                }
                PacketType.SOS -> {
                    broadcastHandler.receiveSosMessage(processedPacket)
                }
                PacketType.DELIVERY_ACK -> {
                    ackManager.handleDeliveryAck(processedPacket)
                }
                PacketType.READ_RECEIPT -> {
                    ackManager.handleReadReceipt(processedPacket)
                }
                PacketType.SESSION_REKEY -> {
                    rekeyManager.handleRekeyPacket(
                        processedPacket.senderId,
                        processedPacket.payload,
                        cryptoManager.getPeerPublicKey(processedPacket.senderId)
                    )
                }
                PacketType.VOICE_SIGNAL,
                PacketType.VOICE_FRAME -> {
                    voiceTransport.handleIncomingPacket(processedPacket)
                }
                PacketType.ROUTE_REQUEST,
                PacketType.ROUTE_REPLY,
                PacketType.ROUTE_ERROR -> {
                    MeshLogger.d(TAG, "Routing control packet ${processedPacket.type} processed by router layer")
                }
                else -> {
                    MeshLogger.d(TAG, "Received unhandled packet type: ${processedPacket.type}")
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error handling packet: ${e.message}")
        }
    }
}
