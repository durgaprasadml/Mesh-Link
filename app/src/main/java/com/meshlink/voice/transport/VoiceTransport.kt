package com.meshlink.voice.transport

import android.util.Base64
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.IoDispatcher
import com.meshlink.security.data.MeshCryptoManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

@Singleton
class VoiceTransport @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    @com.meshlink.di.IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val meshConfig: com.meshlink.config.MeshConfig
,
    @com.meshlink.di.ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope) : com.meshlink.voice.api.VoiceTransport {
    companion object {
        private const val TAG = "VoiceTransport"
    }

// Callback for BleRepositoryImpl or MeshRouter to actually dispatch over network
    var onSendPacket: ((MeshPacket) -> Unit)? = null

    // Callback for AudioStreamer/VoiceManager when a frame/signal arrives
    var onIncomingSignal: ((JSONObject, String) -> Unit)? = null
    var onIncomingFrame: ((ByteArray, String, Long) -> Unit)? = null

    fun sendSignal(senderId: String, targetId: String, signalJson: String) {
        applicationScope.launch {
            try {
                // Signals are encrypted like normal messages
                val encrypted = cryptoManager.encrypt(signalJson, targetId)
                val packet = MeshPacket(
                    senderId = senderId,
                    targetId = targetId,
                    payload = encrypted,
                    type = PacketType.VOICE_SIGNAL,
                    encrypted = true
                )
                onSendPacket?.invoke(packet)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to encrypt signal: ${e.message}")
            }
        }
    }

    fun sendVoiceFrame(senderId: String, targetId: String, callId: String, seqNum: Long, pcmData: ByteArray) {
        // Run synchronously to avoid coroutine overhead for real-time streaming, but network IO will happen later
        try {
            // For real-time streaming, we Base64 encode the frame. 
            // In a production AES-GCM setup, we might skip GCM for raw frames to save CPU and just use a fast stream cipher,
            // but for now we stick to MeshCryptoManager's encryptPayload (or just raw Base64 for PTT if performance drops)
            
            val payloadString = "${callId}:${seqNum}:${Base64.encodeToString(pcmData, Base64.NO_WRAP)}"
            val encrypted = cryptoManager.encrypt(payloadString, targetId)
            
            val packet = MeshPacket(
                senderId = senderId,
                targetId = targetId,
                payload = encrypted,
                type = PacketType.VOICE_FRAME,
                encrypted = true
            )
            onSendPacket?.invoke(packet)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to send frame: ${e.message}")
        }
    }

    fun handleIncomingPacket(packet: MeshPacket) {
        if (!packet.encrypted) return
        
        applicationScope.launch {
            try {
                // Payload is already decrypted by the routing layer
                val decrypted = packet.payload
                
                when (packet.type) {
                    PacketType.VOICE_SIGNAL -> {
                        val json = JSONObject(decrypted)
                        onIncomingSignal?.invoke(json, packet.senderId)
                    }
                    PacketType.VOICE_FRAME -> {
                        val parts = decrypted.split(":", limit = 3)
                        if (parts.size == 3) {
                            val callId = parts[0]
                            val seqNum = parts[1].toLongOrNull() ?: return@launch
                            val pcmBytes = Base64.decode(parts[2], Base64.NO_WRAP)
                            onIncomingFrame?.invoke(pcmBytes, callId, seqNum)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to parse voice packet: ${e.message}")
            }
        }
    }

    @Deprecated("Use initiateVoiceCall instead", ReplaceWith("initiateVoiceCall(peerId)"))
    override suspend fun startVoiceCall(peerId: String) {
        throw UnsupportedOperationException("Not supported in this layer")
    }

    override suspend fun initiateVoiceCall(peerId: String): com.meshlink.domain.model.MeshResult<Unit> {
        return com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.UnknownError("Not supported in this layer"))
    }

    @Deprecated("Use terminateVoiceCall instead", ReplaceWith("terminateVoiceCall(peerId)"))
    override suspend fun endVoiceCall(peerId: String) {
        throw UnsupportedOperationException("Not supported in this layer")
    }

    override suspend fun terminateVoiceCall(peerId: String): com.meshlink.domain.model.MeshResult<Unit> {
        return com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.UnknownError("Not supported in this layer"))
    }
}
