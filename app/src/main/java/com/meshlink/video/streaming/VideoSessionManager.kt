package com.meshlink.video.streaming

import com.meshlink.common.logger.MeshLogger
import com.meshlink.video.models.CallState
import com.meshlink.video.models.VideoSession
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VideoSessionManager @Inject constructor() {
    companion object {
        private const val TAG = "VideoSessionManager"
    }

    private val sessions = ConcurrentHashMap<String, VideoSession>()
    private val _activeSessionFlow = MutableStateFlow<VideoSession?>(null)
    val activeSessionFlow: StateFlow<VideoSession?> = _activeSessionFlow.asStateFlow()

    fun createSession(session: VideoSession) {
        sessions[session.callId] = session
        MeshLogger.d(TAG, "Created video session: ${session.callId}")
    }

    fun getSession(callId: String): VideoSession? = sessions[callId]

    fun updateSessionState(callId: String, newState: CallState) {
        val session = sessions[callId] ?: return
        session.state = newState
        
        if (newState == CallState.ACTIVE || newState == CallState.RINGING) {
            _activeSessionFlow.value = session
        } else if (newState == CallState.ENDED || newState == CallState.FAILED) {
            if (_activeSessionFlow.value?.callId == callId) {
                _activeSessionFlow.value = null
            }
        }
        MeshLogger.d(TAG, "Video Session $callId state -> $newState")
    }

    fun removeSession(callId: String) {
        sessions.remove(callId)
        if (_activeSessionFlow.value?.callId == callId) {
            _activeSessionFlow.value = null
        }
    }

    fun getActiveSession(): VideoSession? = _activeSessionFlow.value
}
