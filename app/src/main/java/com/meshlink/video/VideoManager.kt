package com.meshlink.video

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.IoDispatcher
import com.meshlink.video.models.CallState
import com.meshlink.video.models.SignalType
import com.meshlink.video.models.VideoSession
import com.meshlink.video.models.VideoSignalingMessage
import com.meshlink.video.streaming.VideoSessionManager
import com.meshlink.video.streaming.VideoStreamManager
import com.meshlink.video.transport.VideoTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

@Singleton
class VideoManager @Inject constructor(
    private val sessionManager: VideoSessionManager,
    private val transport: VideoTransport,
    private val streamManager: VideoStreamManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "VideoManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        transport.onIncomingSignal = { json, senderId -> handleIncomingSignal(json, senderId) }
    }

    val activeSession: StateFlow<VideoSession?> = sessionManager.activeSessionFlow

    // ─────────────────── API ───────────────────

    fun startCall(targetId: String, isGroup: Boolean = false): String {
        val session = VideoSession(
            initiatorId = "LOCAL",
            targetId = targetId,
            isGroupCall = isGroup,
            state = CallState.CONNECTING
        )
        sessionManager.createSession(session)
        
        val signal = VideoSignalingMessage(SignalType.INVITE, session.callId, targetId)
        transport.sendSignal(session.initiatorId, targetId, serializeSignal(signal))
        
        return session.callId
    }

    fun acceptCall(callId: String) {
        val session = sessionManager.getSession(callId) ?: return
        sessionManager.updateSessionState(callId, CallState.ACTIVE)
        
        val signal = VideoSignalingMessage(SignalType.ACCEPT, callId, session.initiatorId)
        transport.sendSignal("LOCAL", session.initiatorId, serializeSignal(signal))
        
        streamManager.startStreaming("LOCAL", session.initiatorId, callId)
    }

    fun rejectCall(callId: String) {
        val session = sessionManager.getSession(callId) ?: return
        sessionManager.updateSessionState(callId, CallState.ENDED)
        
        val signal = VideoSignalingMessage(SignalType.REJECT, callId, session.initiatorId)
        transport.sendSignal("LOCAL", session.initiatorId, serializeSignal(signal))
    }

    fun endCall(callId: String) {
        val session = sessionManager.getSession(callId) ?: return
        sessionManager.updateSessionState(callId, CallState.ENDED)
        
        val signal = VideoSignalingMessage(SignalType.END, callId, session.targetId)
        transport.sendSignal("LOCAL", session.targetId, serializeSignal(signal))
        
        streamManager.stopStreaming()
    }

    fun toggleCamera() {
        streamManager.switchCamera()
    }

    fun toggleScreenShare() {
        streamManager.toggleScreenShare()
    }

    // ─────────────────── Incoming Handlers ───────────────────

    private fun handleIncomingSignal(json: JSONObject, senderId: String) {
        try {
            val typeStr = json.optString("type")
            val type = SignalType.valueOf(typeStr)
            val callId = json.optString("callId")
            
            when (type) {
                SignalType.INVITE -> {
                    val session = VideoSession(
                        callId = callId,
                        initiatorId = senderId,
                        targetId = "LOCAL",
                        state = CallState.RINGING
                    )
                    sessionManager.createSession(session)
                    MeshLogger.d(TAG, "Incoming video call $callId from $senderId")
                }
                SignalType.ACCEPT -> {
                    sessionManager.updateSessionState(callId, CallState.ACTIVE)
                    val session = sessionManager.getSession(callId)
                    if (session != null) {
                        streamManager.startStreaming("LOCAL", session.targetId, callId)
                    }
                }
                SignalType.REJECT, SignalType.END -> {
                    sessionManager.updateSessionState(callId, CallState.ENDED)
                    streamManager.stopStreaming()
                }
                SignalType.REQUEST_I_FRAME -> {
                    streamManager.requestKeyFrame()
                }
                SignalType.PAUSE_VIDEO, SignalType.RESUME_VIDEO -> {
                    // Handle remote video pause/resume
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error handling video signal: ${e.message}")
        }
    }

    // ─────────────────── Helpers ───────────────────

    private fun serializeSignal(signal: VideoSignalingMessage): String {
        val json = JSONObject()
        json.put("type", signal.type.name)
        json.put("callId", signal.callId)
        json.put("targetId", signal.targetId)
        json.put("codec", signal.codec)
        json.put("resolution", signal.resolution)
        json.put("isScreenShare", signal.isScreenShare)
        return json.toString()
    }
}
