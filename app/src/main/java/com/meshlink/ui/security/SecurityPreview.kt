package com.meshlink.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.ui.designsystem.theme.MeshTheme

private val sampleEncryption = EncryptionUi(
    isE2eeActive = true,
    cipherSuite = "AES-256-GCM",
    keyExchangeAlg = "ECDH X25519",
    ratchetProtocol = "Double Ratchet v2",
    isHardwareKeystoreUsed = true,
    broadcastKeyVersion = 3,
    sessionEstablishedCount = 4
)

private val sampleTrust = TrustUi(
    overallLevel = TrustLevel.VERIFIED,
    securityScore = 98,
    verifiedDevicesCount = 4,
    totalDevicesCount = 4
)

private val sampleStats = SecurityStatsUi(
    trustedDevices = 4,
    activeSessions = 4,
    verifiedKeys = 4,
    replayAttacksRejected = 12,
    authFailuresCount = 0,
    signatureFailuresCount = 0,
    tamperedPacketsRejected = 0,
    keyRotationsExecuted = 3,
    isHardwareKeystoreActive = true
)

private val sampleFingerprint = FingerprintUi(
    fullFingerprint = "A1B2C3D4E5F67890123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0",
    formattedHexBlocks = listOf("A1B2", "C3D4", "E5F6", "7890", "1234", "5678", "9ABC", "DEF0"),
    algorithm = "SHA-256",
    keyType = "ED25519 Identity Key"
)

private val sampleDevices = listOf(
    DeviceTrustUi(
        meshId = "node_alpha_8f",
        alias = "Command Alpha",
        trustLevel = TrustLevel.VERIFIED,
        connectionType = "BLE Mesh",
        sessionAge = "24m",
        isSessionActive = true,
        fingerprint = "A1B2C3D4E5F6"
    ),
    DeviceTrustUi(
        meshId = "node_bravo_2c",
        alias = "Relay Bravo",
        trustLevel = TrustLevel.TRUSTED,
        connectionType = "Wi-Fi Direct",
        sessionAge = "12m",
        isSessionActive = true,
        fingerprint = "9ABCDEF01234"
    )
)

private val sampleSessions = listOf(
    SessionUi(
        sessionId = "sess_001",
        peerId = "node_alpha_8f",
        peerName = "Command Alpha",
        state = "ACTIVE",
        keyVersion = 4,
        totalEncryptedPackets = 1240,
        totalDecryptedPackets = 980
    )
)

private val sampleAlerts = listOf(
    SecurityAlertUi(
        id = "1",
        title = "Replay Attack Blocked",
        description = "Rejected duplicate packet sequence from node_charlie.",
        timestamp = "2m ago",
        severity = SecurityAlertSeverity.WARNING
    )
)

private val sampleTimeline = listOf(
    SecurityTimelineEventUi(
        id = "1",
        title = "Broadcast Key Rotated",
        detail = "Ratcheted symmetric broadcast key to version 3.",
        timestamp = "10m ago",
        trustLevel = TrustLevel.VERIFIED
    )
)

@Preview(name = "Light Phone Preview", showBackground = true)
@Composable
fun SecurityScreenLightPreview() {
    MeshTheme(themeMode = "LIGHT") {
        MeshSecurityScreen(
            encryptionUi = sampleEncryption,
            trustUi = sampleTrust,
            statsUi = sampleStats,
            fingerprintUi = sampleFingerprint,
            privacyUi = PrivacyUi(),
            connectedDevices = sampleDevices,
            activeSessions = sampleSessions,
            alerts = sampleAlerts,
            timelineEvents = sampleTimeline,
            selectedVerification = null,
            onBackClick = {}
        )
    }
}

@Preview(name = "Dark Phone Preview", showBackground = true)
@Composable
fun SecurityScreenDarkPreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSecurityScreen(
            encryptionUi = sampleEncryption,
            trustUi = sampleTrust,
            statsUi = sampleStats,
            fingerprintUi = sampleFingerprint,
            privacyUi = PrivacyUi(),
            connectedDevices = sampleDevices,
            activeSessions = sampleSessions,
            alerts = sampleAlerts,
            timelineEvents = sampleTimeline,
            selectedVerification = null,
            onBackClick = {}
        )
    }
}

@Preview(name = "Tablet Preview", device = Devices.TABLET, showBackground = true)
@Composable
fun SecurityScreenTabletPreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSecurityScreen(
            encryptionUi = sampleEncryption,
            trustUi = sampleTrust,
            statsUi = sampleStats,
            fingerprintUi = sampleFingerprint,
            privacyUi = PrivacyUi(),
            connectedDevices = sampleDevices,
            activeSessions = sampleSessions,
            alerts = sampleAlerts,
            timelineEvents = sampleTimeline,
            selectedVerification = null,
            onBackClick = {}
        )
    }
}
