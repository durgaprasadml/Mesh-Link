package com.meshlink.voice.streaming

import com.meshlink.common.logger.MeshLogger
import com.meshlink.voice.models.CallState
import com.meshlink.voice.models.VoiceSession
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VoiceSessionManager @Inject constructor() {
    companion object {
        private const val TAG = "VoiceSessionManager"
    }

    private val sessions = ConcurrentHashMap<String, VoiceSession>()
    private val _activeSessionFlow = MutableStateFlow<VoiceSession?>(null)
    val activeSessionFlow: StateFlow<VoiceSession?> = _activeSessionFlow.asStateFlow()

    fun createSession(session: VoiceSession) {
        sessions[session.callId] = session
        MeshLogger.d(TAG, "Created session: ${session.callId}")
    }

    fun getSession(callId: String): VoiceSession? = sessions[callId]

    fun updateSessionState(callId: String, newState: CallState) {
        val session = sessions[callId] ?: return
        session.state = newState
        
        if (newState == CallState.ACTIVE || newState == CallState.RINGING) {
            _activeSessionFlow.value = session
        } else if (newState == CallState.ENDED || newState == CallState.FAILED) {
            if (_activeSessionFlow.value?.callId == callId) {
                _activeSessionFlow.value = null
            }
            // Retain in map for analytics briefly, but we can also remove it
        }
        MeshLogger.d(TAG, "Session $callId state -> $newState")
    }

    fun removeSession(callId: String) {
        sessions.remove(callId)
        if (_activeSessionFlow.value?.callId == callId) {
            _activeSessionFlow.value = null
        }
    }

    fun getActiveSession(): VoiceSession? = _activeSessionFlow.value
}
