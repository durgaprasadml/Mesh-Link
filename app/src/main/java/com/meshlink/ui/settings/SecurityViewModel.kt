package com.meshlink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.SecurityRepository
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SecurityLogEntry
import com.meshlink.security.data.SecurityLogManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SecurityUiState(
    // Identity
    val meshId: String = "",
    val deviceFingerprint: String = "",
    val publicKey: String = "",
    val keyAgeMs: Long = 0,
    val lastRotationMs: Long = 0,
    
    // Auth & App Lock
    val isAppLockEnabled: Boolean = false,
    val autoLockTimeoutMs: Long = 60000L,
    val isBiometricsEnabled: Boolean = false,
    val hasPinConfigured: Boolean = false,
    
    // Trust
    val trustedDevices: Map<String, TrustLevel> = emptyMap(),
    
    // Sessions
    val activeSessions: Set<String> = emptySet(),
    
    // Logs
    val securityLogs: List<SecurityLogEntry> = emptyList(),
    
    // Dialogs & Ephemeral State
    val message: String? = null,
    val exportedIdentityBase64: String? = null,
    val exportedLogFile: File? = null
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager,
    private val trustManager: TrustManager,
    private val securityRepository: SecurityRepository,
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val logManager: SecurityLogManager
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _exportedIdentity = MutableStateFlow<String?>(null)
    private val _exportedLogFile = MutableStateFlow<File?>(null)
    
    // Combine state
    val uiState: StateFlow<SecurityUiState> = combine(
        flow { emit(userRepository.getLocalUser()) },
        settingsRepository.isAppLockEnabled,
        settingsRepository.autoLockTimeoutMs,
        settingsRepository.isBiometricsEnabled,
        settingsRepository.appLockPinHash,
        trustManager.trustStates,
        logManager.logsFlow,
        _message,
        _exportedIdentity,
        _exportedLogFile
    ) { args ->
        val user = args[0] as User?
        val appLock = args[1] as Boolean
        val timeout = args[2] as Long
        val biometrics = args[3] as Boolean
        val pinHash = args[4] as String?
        val trust = args[5] as Map<String, TrustLevel>
        val logs = args[6] as List<SecurityLogEntry>
        val msg = args[7] as String?
        val exportId = args[8] as String?
        val exportLog = args[9] as File?

        SecurityUiState(
            meshId = user?.meshId ?: "Unknown",
            deviceFingerprint = cryptoManager.getLocalFingerprint(),
            publicKey = cryptoManager.getOrCreatePublicKey(),
            keyAgeMs = System.currentTimeMillis() - cryptoManager.getKeyCreationTime(),
            lastRotationMs = cryptoManager.getLastRotationTime(),
            isAppLockEnabled = appLock,
            autoLockTimeoutMs = timeout,
            isBiometricsEnabled = biometrics,
            hasPinConfigured = pinHash != null,
            trustedDevices = trust,
            activeSessions = sessionManager.getAllSessionPeers(),
            securityLogs = logs,
            message = msg,
            exportedIdentityBase64 = exportId,
            exportedLogFile = exportLog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityUiState())

    fun clearMessage() {
        _message.value = null
    }

    fun clearExportedIdentity() {
        _exportedIdentity.value = null
    }
    
    fun clearExportedLogFile() {
        _exportedLogFile.value = null
    }

    // ────────── App Lock & Auth ──────────

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricsEnabled(enabled) }
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        viewModelScope.launch { settingsRepository.setAutoLockTimeoutMs(timeoutMs) }
    }

    fun configurePin(newPin: String) {
        viewModelScope.launch {
            securityRepository.setAppLockPin(newPin)
            settingsRepository.setAppLockEnabled(true)
            logManager.log("App Lock", "PIN configured")
            _message.value = "PIN configured successfully"
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            securityRepository.clearAppLockPin()
            settingsRepository.setAppLockEnabled(false)
            settingsRepository.setBiometricsEnabled(false)
            logManager.log("App Lock", "PIN cleared")
            _message.value = "PIN removed"
        }
    }

    // ────────── Identity & Keys ──────────

    fun rotateIdentityKeys() {
        viewModelScope.launch {
            try {
                cryptoManager.rotateIdentityKeys()
                logManager.log("Key Management", "Identity keys rotated")
                _message.value = "Keys rotated successfully"
            } catch (e: Exception) {
                _message.value = "Error rotating keys: \${e.message}"
            }
        }
    }

    fun exportIdentity() {
        viewModelScope.launch {
            try {
                val exported = cryptoManager.exportIdentity()
                logManager.log("Key Management", "Identity exported")
                _exportedIdentity.value = exported
            } catch (e: Exception) {
                _message.value = "Export failed: \${e.message}"
            }
        }
    }

    fun importIdentity(identityBase64: String) {
        viewModelScope.launch {
            try {
                cryptoManager.importIdentity(identityBase64)
                logManager.log("Key Management", "Identity imported", SecurityLogEntry.Severity.WARNING)
                _message.value = "Identity imported successfully. App restart recommended."
            } catch (e: Exception) {
                _message.value = "Import failed: \${e.message}"
            }
        }
    }

    // ────────── Trust & Sessions ──────────

    fun blockDevice(peerId: String) {
        trustManager.blockPeer(peerId)
        sessionManager.removeSession(peerId)
        logManager.log("Trust Management", "Device blocked: \$peerId", SecurityLogEntry.Severity.WARNING)
    }

    fun removeTrust(peerId: String) {
        trustManager.decreaseTrustScore(peerId, 100, "User removed trust")
        logManager.log("Trust Management", "Trust removed for: \$peerId")
    }

    fun terminateSession(peerId: String) {
        sessionManager.removeSession(peerId)
        logManager.log("Session", "Terminated session: \$peerId")
    }

    fun terminateAllSessions() {
        val peers = sessionManager.getAllSessionPeers()
        peers.forEach { sessionManager.removeSession(it) }
        logManager.log("Session", "All sessions terminated")
    }

    // ────────── Logs ──────────

    fun exportSecurityLogs() {
        val file = logManager.exportLogs()
        if (file != null) {
            _exportedLogFile.value = file
            _message.value = "Logs exported to \${file.name}"
        } else {
            _message.value = "Failed to export logs"
        }
    }
}
