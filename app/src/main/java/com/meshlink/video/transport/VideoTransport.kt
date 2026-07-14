package com.meshlink.video.transport

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
class VideoTransport @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "VideoTransport"
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    var onSendPacket: ((MeshPacket) -> Unit)? = null
    var onIncomingSignal: ((JSONObject, String) -> Unit)? = null
    
    // (NAL unit, Sender ID, Sequence Number, Presentation Time)
    var onIncomingFrame: ((ByteArray, String, Long, Long) -> Unit)? = null

    fun sendSignal(senderId: String, targetId: String, signalJson: String) {
        scope.launch {
            try {
                val encrypted = cryptoManager.encryptPayload(signalJson, targetId)
                if (encrypted != null) {
                    val packet = MeshPacket(
                        senderId = senderId,
                        targetId = targetId,
                        payload = encrypted,
                        type = PacketType.VIDEO_SIGNAL,
                        encrypted = true
                    )
                    onSendPacket?.invoke(packet)
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to encrypt video signal: ${e.message}")
            }
        }
    }

    fun sendVideoFrame(senderId: String, targetId: String, seqNum: Long, ptsUs: Long, nalUnit: ByteArray) {
        try {
            // Encode the frame along with its metadata
            val payloadString = "${seqNum}:${ptsUs}:${Base64.encodeToString(nalUnit, Base64.NO_WRAP)}"
            val encrypted = cryptoManager.encryptPayload(payloadString, targetId)
            
            if (encrypted != null) {
                val packet = MeshPacket(
                    senderId = senderId,
                    targetId = targetId,
                    payload = encrypted,
                    type = PacketType.VIDEO_FRAME,
                    encrypted = true
                )
                onSendPacket?.invoke(packet)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to send video frame: ${e.message}")
        }
    }

    fun handleIncomingPacket(packet: MeshPacket) {
        if (!packet.encrypted) return
        
        scope.launch {
            try {
                val decrypted = cryptoManager.decryptOrPassthrough(packet.payload, packet.senderId)
                
                when (packet.type) {
                    PacketType.VIDEO_SIGNAL -> {
                        val json = JSONObject(decrypted)
                        onIncomingSignal?.invoke(json, packet.senderId)
                    }
                    PacketType.VIDEO_FRAME -> {
                        val parts = decrypted.split(":", limit = 3)
                        if (parts.size == 3) {
                            val seqNum = parts[0].toLongOrNull() ?: return@launch
                            val ptsUs = parts[1].toLongOrNull() ?: return@launch
                            val nalUnit = Base64.decode(parts[2], Base64.NO_WRAP)
                            onIncomingFrame?.invoke(nalUnit, packet.senderId, seqNum, ptsUs)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to decrypt video packet: ${e.message}")
            }
        }
    }
}
