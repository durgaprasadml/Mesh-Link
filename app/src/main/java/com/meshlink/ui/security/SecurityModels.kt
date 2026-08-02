package com.meshlink.ui.security

import androidx.compose.runtime.Immutable

/**
 * Trust level classifications for mesh network peers.
 */
enum class TrustLevel {
    VERIFIED,
    TRUSTED,
    UNKNOWN,
    WARNING,
    BLOCKED
}

/**
 * Security alert severity levels.
 */
enum class SecurityAlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Overall End-to-End Encryption presentation model.
 */
@Immutable
data class EncryptionUi(
    val isE2eeActive: Boolean = true,
    val cipherSuite: String = "AES-256-GCM",
    val keyExchangeAlg: String = "ECDH X25519",
    val ratchetProtocol: String = "Double Ratchet v2",
    val isHardwareKeystoreUsed: Boolean = true,
    val broadcastKeyVersion: Int = 1,
    val sessionEstablishedCount: Int = 0,
    val perfectForwardSecrecy: Boolean = true
)

/**
 * Overall Trust metrics summary UI model.
 */
@Immutable
data class TrustUi(
    val overallLevel: TrustLevel = TrustLevel.TRUSTED,
    val securityScore: Int = 98,
    val verifiedDevicesCount: Int = 0,
    val totalDevicesCount: Int = 0,
    val pendingVerificationsCount: Int = 0
)

/**
 * Individual device trust details UI representation.
 */
@Immutable
data class DeviceTrustUi(
    val meshId: String,
    val alias: String,
    val avatarUri: String? = null,
    val trustLevel: TrustLevel = TrustLevel.UNKNOWN,
    val connectionType: String = "BLE Mesh",
    val sessionAge: String = "0m",
    val isSessionActive: Boolean = false,
    val keyVersion: Int = 1,
    val fingerprint: String = ""
)

/**
 * Secure E2EE session presentation model.
 */
@Immutable
data class SessionUi(
    val sessionId: String,
    val peerId: String,
    val peerName: String = "Peer",
    val state: String = "ACTIVE",
    val keyVersion: Int = 1,
    val previousKeyVersion: Int = 0,
    val totalEncryptedPackets: Long = 0L,
    val totalDecryptedPackets: Long = 0L,
    val pfsActive: Boolean = true,
    val sessionDuration: String = "0m",
    val lastRekeyTimestamp: String = "Just now"
)

/**
 * Cryptographic Identity Key Fingerprint UI model.
 */
@Immutable
data class FingerprintUi(
    val fullFingerprint: String = "",
    val shortFingerprint: String = "",
    val formattedHexBlocks: List<String> = emptyList(),
    val algorithm: String = "SHA-256",
    val keyType: String = "ED25519 Identity Key"
)

/**
 * Security Audit Alert item UI model.
 */
@Immutable
data class SecurityAlertUi(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val severity: SecurityAlertSeverity = SecurityAlertSeverity.INFO,
    val isAcknowledged: Boolean = false,
    val alertType: String = "SECURITY_EVENT"
)

/**
 * Node Privacy & Authentication settings UI model.
 */
@Immutable
data class PrivacyUi(
    val discoverabilityEnabled: Boolean = true,
    val onlineVisibility: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val advancedEncryptionEnforced: Boolean = true
)

/**
 * Identity Verification state UI model.
 */
@Immutable
data class VerificationUi(
    val peerId: String,
    val peerName: String,
    val sasNumericCode: String = "123456",
    val sasEmojiList: List<String> = listOf("🛡️", "🔑", "⚡", "🔒"),
    val verificationStatus: TrustLevel = TrustLevel.UNKNOWN,
    val publicKeyFingerprint: String = ""
)

/**
 * Mission Control Security Statistics UI model.
 */
@Immutable
data class SecurityStatsUi(
    val trustedDevices: Int = 0,
    val activeSessions: Int = 0,
    val verifiedKeys: Int = 0,
    val replayAttacksRejected: Long = 0L,
    val authFailuresCount: Long = 0L,
    val signatureFailuresCount: Long = 0L,
    val tamperedPacketsRejected: Long = 0L,
    val keyRotationsExecuted: Long = 0L,
    val expiredSessionsCleaned: Long = 0L,
    val isHardwareKeystoreActive: Boolean = true
)

/**
 * Timeline Audit event UI model.
 */
@Immutable
data class SecurityTimelineEventUi(
    val id: String,
    val title: String,
    val detail: String,
    val timestamp: String,
    val type: String = "INFO",
    val trustLevel: TrustLevel = TrustLevel.TRUSTED
)

/**
 * Filter categories for Security Center.
 */
enum class SecurityFilterCategory(val label: String) {
    ALL("All"),
    IDENTITY("Identity"),
    DEVICES("Devices"),
    SESSIONS("Sessions"),
    PRIVACY("Privacy"),
    PERMISSIONS("Permissions"),
    DIAGNOSTICS("Diagnostics")
}

/**
 * Permission status enumeration.
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    LIMITED
}

/**
 * System Runtime Permission item UI model.
 */
@Immutable
data class PermissionItemUi(
    val id: String,
    val name: String,
    val group: String,
    val status: PermissionStatus,
    val description: String
)

/**
 * Identity Backup & Key Export UI status model.
 */
@Immutable
data class BackupStatusUi(
    val identityBackupConfigured: Boolean = true,
    val recoveryPhraseCreated: Boolean = true,
    val offlineKeyExportAvailable: Boolean = true,
    val cloudStorageIsolated: Boolean = true,
    val lastBackupTimestamp: String = "Today, 10:15 AM",
    val backupLocation: String = "Local Encrypted Storage"
)

/**
 * Security Diagnostics System Health UI status model.
 */
enum class DiagnosticHealthStatus {
    HEALTHY,
    WARNING,
    UNAVAILABLE
}

@Immutable
data class DiagnosticComponentUi(
    val id: String,
    val name: String,
    val category: String,
    val status: DiagnosticHealthStatus,
    val details: String,
    val metricValue: String? = null
)

/**
 * Advanced Security Settings presentation UI model.
 */
@Immutable
data class AdvancedSecurityUi(
    val screenshotProtectionEnabled: Boolean = true,
    val autoLockTimeoutMinutes: Int = 5,
    val developerModeAllowed: Boolean = false,
    val encryptedLogsEnabled: Boolean = true,
    val strictTransportSecurity: Boolean = true
)

