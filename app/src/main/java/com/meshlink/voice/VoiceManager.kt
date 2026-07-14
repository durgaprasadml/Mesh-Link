package com.meshlink.voice

import com.meshlink.ble.data.MeshPacket
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.IoDispatcher
import com.meshlink.voice.models.CallState
import com.meshlink.voice.models.CodecType
import com.meshlink.voice.models.SignalType
import com.meshlink.voice.models.SignalingMessage
import com.meshlink.voice.models.TransportType
import com.meshlink.voice.models.VoiceSession
import com.meshlink.voice.streaming.AudioStreamer
import com.meshlink.voice.streaming.VoiceSessionManager
import com.meshlink.voice.transport.VoiceTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

@Singleton
class VoiceManager @Inject constructor(
    private val sessionManager: VoiceSessionManager,
    private val transport: VoiceTransport,
    private val streamer: AudioStreamer,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "VoiceManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        transport.onIncomingSignal = { json, senderId -> handleIncomingSignal(json, senderId) }
    }

    val activeSession: StateFlow<VoiceSession?> = sessionManager.activeSessionFlow

    // ─────────────────── API ───────────────────

    fun startCall(targetId: String, isGroup: Boolean = false): String {
        val session = VoiceSession(
            initiatorId = "LOCAL", // Replaced by actual networkId later via BleRepository injection
            targetId = targetId,
            isGroupCall = isGroup,
            state = CallState.CONNECTING
        )
        sessionManager.createSession(session)
        
        val signal = SignalingMessage(SignalType.INVITE, session.callId, targetId)
        transport.sendSignal(session.initiatorId, targetId, serializeSignal(signal))
        
        return session.callId
    }

    fun acceptCall(callId: String) {
        val session = sessionManager.getSession(callId) ?: return
        sessionManager.updateSessionState(callId, CallState.ACTIVE)
        
        val signal = SignalingMessage(SignalType.ACCEPT, callId, session.initiatorId)
        transport.sendSignal("LOCAL", session.initiatorId, serializeSignal(signal))
        
        // Use BLE bitrate (16kbps) or Wi-Fi (64kbps). Defaulting to Wi-Fi for now, transport selects later.
        streamer.startStreaming("LOCAL", session.initiatorId, callId, com.meshlink.voice.codec.VoiceCodecManager.BITRATE_WIFI)
    }

    fun rejectCall(callId: String) {
        val session = sessionManager.getSession(callId) ?: return
        sessionManager.updateSessionState(callId, CallState.ENDED)
        
        val signal = SignalingMessage(SignalType.REJECT, callId, session.initiatorId)
        transport.sendSignal("LOCAL", session.initiatorId, serializeSignal(signal))
    }

    fun endCall(callId: String) {
        val session = sessionManager.getSession(callId) ?: return
        sessionManager.updateSessionState(callId, CallState.ENDED)
        
        val signal = SignalingMessage(SignalType.END, callId, session.targetId)
        transport.sendSignal("LOCAL", session.targetId, serializeSignal(signal))
        
        streamer.stopStreaming()
    }

    // ─────────────────── Incoming Handlers ───────────────────

    private fun handleIncomingSignal(json: JSONObject, senderId: String) {
        try {
            val typeStr = json.optString("type")
            val type = SignalType.valueOf(typeStr)
            val callId = json.optString("callId")
            
            when (type) {
                SignalType.INVITE -> {
                    val session = VoiceSession(
                        callId = callId,
                        initiatorId = senderId,
                        targetId = "LOCAL",
                        state = CallState.RINGING
                    )
                    sessionManager.createSession(session)
                    MeshLogger.d(TAG, "Incoming call $callId from $senderId")
                }
                SignalType.ACCEPT -> {
                    sessionManager.updateSessionState(callId, CallState.ACTIVE)
                    val session = sessionManager.getSession(callId)
                    if (session != null) {
                        streamer.startStreaming("LOCAL", session.targetId, callId, com.meshlink.voice.codec.VoiceCodecManager.BITRATE_WIFI)
                    }
                }
                SignalType.REJECT, SignalType.END -> {
                    sessionManager.updateSessionState(callId, CallState.ENDED)
                    streamer.stopStreaming()
                }
                SignalType.PTT_START -> {
                    // Start playback for PTT
                }
                SignalType.PTT_STOP -> {
                    // Stop playback for PTT
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error handling signal: ${e.message}")
        }
    }


    // ─────────────────── Helpers ───────────────────

    private fun serializeSignal(signal: SignalingMessage): String {
        val json = JSONObject()
        json.put("type", signal.type.name)
        json.put("callId", signal.callId)
        json.put("targetId", signal.targetId)
        json.put("codec", signal.codec)
        json.put("isPtt", signal.isPtt)
        return json.toString()
    }
}
