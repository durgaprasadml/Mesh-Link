package com.meshlink.voice.transport

import android.util.Base64
import com.meshlink.ble.data.MeshPacket
import com.meshlink.ble.data.PacketType
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "VoiceTransport"
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    // Callback for BleRepositoryImpl or MeshRouter to actually dispatch over network
    var onSendPacket: ((MeshPacket) -> Unit)? = null

    // Callback for AudioStreamer/VoiceManager when a frame/signal arrives
    var onIncomingSignal: ((JSONObject, String) -> Unit)? = null
    var onIncomingFrame: ((ByteArray, String, Long) -> Unit)? = null

    fun sendSignal(senderId: String, targetId: String, signalJson: String) {
        scope.launch {
            try {
                // Signals are encrypted like normal messages
                val encrypted = cryptoManager.encrypt(signalJson, targetId)
                if (encrypted != null) {
                    val packet = MeshPacket(
                        senderId = senderId,
                        targetId = targetId,
                        payload = encrypted,
                        type = PacketType.VOICE_SIGNAL,
                        encrypted = true
                    )
                    onSendPacket?.invoke(packet)
                }
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
            
            if (encrypted != null) {
                val packet = MeshPacket(
                    senderId = senderId,
                    targetId = targetId,
                    payload = encrypted,
                    type = PacketType.VOICE_FRAME,
                    encrypted = true
                )
                onSendPacket?.invoke(packet)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to send frame: ${e.message}")
        }
    }

    fun handleIncomingPacket(packet: MeshPacket) {
        if (!packet.encrypted) return
        
        scope.launch {
            try {
                val decrypted = cryptoManager.decryptOrPassthrough(packet.payload, packet.senderId)
                
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
                MeshLogger.e(TAG, "Failed to decrypt voice packet: ${e.message}")
            }
        }
    }
}
