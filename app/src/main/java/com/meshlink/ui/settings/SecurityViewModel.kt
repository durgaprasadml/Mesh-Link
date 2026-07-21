package com.meshlink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

data class SecurityUiState(
    // Trust
    val trustedDevices: Map<String, TrustLevel> = emptyMap(),
    
    // Sessions
    val activeSessions: Set<String> = emptySet(),
    
    // Dialogs & Ephemeral State
    val message: String? = null
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val trustManager: TrustManager
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    
    // Combine state
    val uiState: StateFlow<SecurityUiState> = combine(
        trustManager.trustStates,
        _message
    ) { trust, msg ->
        SecurityUiState(
            trustedDevices = trust,
            activeSessions = sessionManager.getAllSessionPeers(),
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityUiState())

    fun clearMessage() {
        _message.value = null
    }

    // ────────── Trust & Sessions ──────────

    fun blockDevice(peerId: String) {
        trustManager.blockPeer(peerId)
        sessionManager.removeSession(peerId)
    }

    fun removeTrust(peerId: String) {
        trustManager.decreaseTrustScore(peerId, 100, "User removed trust")
    }

    fun terminateSession(peerId: String) {
        sessionManager.removeSession(peerId)
    }

    fun terminateAllSessions() {
        val peers = sessionManager.getAllSessionPeers()
        peers.forEach { sessionManager.removeSession(it) }
    }
}
