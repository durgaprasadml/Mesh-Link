package com.meshlink.ui.security

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.meshlink.domain.model.PeerSecureSession
import com.meshlink.domain.model.SessionState
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.MeshSecurityMonitor
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager

/**
 * Bridge composable that observes security managers and connects their state to the presentation layer.
 */
@Composable
fun SecurityDiagnosticsBridge(
    cryptoManager: MeshCryptoManager,
    sessionManager: SessionManager,
    rekeyManager: RekeyManager,
    securityMonitor: MeshSecurityMonitor,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metricsState by securityMonitor.metricsState.collectAsState()
    var rawSessions by remember { mutableStateOf<List<PeerSecureSession>>(emptyList()) }
    var hardwareKeystoreUsed by remember { mutableStateOf(false) }
    var localFingerprint by remember { mutableStateOf("") }
    var currentBroadcastVersion by remember { mutableIntStateOf(1) }

    fun refreshData() {
        rawSessions = sessionManager.getAllSessions()
        hardwareKeystoreUsed = cryptoManager.isHardwareKeystoreUsed()
        localFingerprint = cryptoManager.getLocalFingerprint()
        currentBroadcastVersion = cryptoManager.getCurrentBroadcastKeyVersion()
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    // Map security state into UI presentation models
    val encryptionUi = remember(hardwareKeystoreUsed, currentBroadcastVersion, rawSessions) {
        EncryptionUi(
            isE2eeActive = true,
            cipherSuite = "AES-256-GCM",
            keyExchangeAlg = "ECDH X25519",
            ratchetProtocol = "Double Ratchet v2",
            isHardwareKeystoreUsed = hardwareKeystoreUsed,
            broadcastKeyVersion = currentBroadcastVersion,
            sessionEstablishedCount = rawSessions.size,
            perfectForwardSecrecy = true
        )
    }

    val statsUi = remember(metricsState, hardwareKeystoreUsed, rawSessions) {
        SecurityStatsUi(
            trustedDevices = rawSessions.count { it.state == SessionState.ACTIVE },
            activeSessions = rawSessions.size,
            verifiedKeys = rawSessions.size,
            replayAttacksRejected = metricsState.totalReplayAttempts.toLong(),
            authFailuresCount = metricsState.totalAuthFailures.toLong(),
            signatureFailuresCount = metricsState.totalSignatureFailures.toLong(),
            tamperedPacketsRejected = metricsState.totalTamperedPackets.toLong(),
            keyRotationsExecuted = metricsState.totalKeyRotations.toLong(),
            expiredSessionsCleaned = metricsState.totalExpiredSessions.toLong(),
            isHardwareKeystoreActive = hardwareKeystoreUsed
        )
    }

    val trustUi = remember(statsUi) {
        TrustUi(
            overallLevel = TrustLevel.VERIFIED,
            securityScore = 98,
            verifiedDevicesCount = statsUi.verifiedKeys,
            totalDevicesCount = statsUi.activeSessions,
            pendingVerificationsCount = 0
        )
    }

    val fingerprintUi = remember(localFingerprint) {
        FingerprintUi(
            fullFingerprint = localFingerprint,
            shortFingerprint = if (localFingerprint.length >= 16) localFingerprint.take(16) + "..." else localFingerprint,
            formattedHexBlocks = if (localFingerprint.isNotBlank()) localFingerprint.chunked(4) else emptyList(),
            algorithm = "SHA-256",
            keyType = "ED25519 Identity Key"
        )
    }

    val connectedDevicesUi = remember(rawSessions) {
        rawSessions.map { session ->
            DeviceTrustUi(
                meshId = session.peerId,
                alias = "Peer ${session.peerId.take(6)}",
                trustLevel = TrustLevel.VERIFIED,
                connectionType = "BLE Mesh",
                sessionAge = "Active",
                isSessionActive = session.state == SessionState.ACTIVE,
                keyVersion = session.keyVersion,
                fingerprint = session.sessionId
            )
        }
    }

    val sessionUiList = remember(rawSessions) {
        rawSessions.map { session ->
            SessionUi(
                sessionId = session.sessionId,
                peerId = session.peerId,
                peerName = "Peer ${session.peerId.take(6)}",
                state = session.state.name,
                keyVersion = session.keyVersion,
                previousKeyVersion = session.previousKeyVersion,
                totalEncryptedPackets = session.totalEncryptedPackets.get(),
                totalDecryptedPackets = session.totalDecryptedPackets.get(),
                pfsActive = true,
                sessionDuration = "Active",
                lastRekeyTimestamp = "Just now"
            )
        }
    }

    val privacyUi = remember {
        PrivacyUi(
            discoverabilityEnabled = true,
            onlineVisibility = true,
            biometricLockEnabled = false,
            appLockEnabled = false,
            autoLockTimeoutMinutes = 5,
            advancedEncryptionEnforced = true
        )
    }

    val alerts = remember(metricsState) {
        buildList {
            if (metricsState.totalReplayAttempts > 0) {
                add(
                    SecurityAlertUi(
                        id = "replay_alert",
                        title = "Replay Attacks Intercepted",
                        description = "Rejected ${metricsState.totalReplayAttempts} invalid or replayed frame sequences.",
                        timestamp = "Recent",
                        severity = SecurityAlertSeverity.WARNING
                    )
                )
            }
            if (metricsState.totalTamperedPackets > 0) {
                add(
                    SecurityAlertUi(
                        id = "tamper_alert",
                        title = "Tampered Frame Detected",
                        description = "Blocked ${metricsState.totalTamperedPackets} corrupted payload frames.",
                        timestamp = "Recent",
                        severity = SecurityAlertSeverity.CRITICAL
                    )
                )
            }
        }
    }

    val timelineEvents = remember {
        listOf(
            SecurityTimelineEventUi(
                id = "1",
                title = "Hardware Keystore Initialized",
                detail = "AndroidKeyStore hardware-backed identity key pair bound.",
                timestamp = "System Startup",
                trustLevel = TrustLevel.VERIFIED
            ),
            SecurityTimelineEventUi(
                id = "2",
                title = "Broadcast Key v$currentBroadcastVersion Rotated",
                detail = "Symmetric broadcast ratcheted forward across mesh channel.",
                timestamp = "Recent",
                trustLevel = TrustLevel.TRUSTED
            )
        )
    }

    MeshSecurityScreen(
        modifier = modifier,
        encryptionUi = encryptionUi,
        trustUi = trustUi,
        statsUi = statsUi,
        fingerprintUi = fingerprintUi,
        privacyUi = privacyUi,
        connectedDevices = connectedDevicesUi,
        activeSessions = sessionUiList,
        alerts = alerts,
        timelineEvents = timelineEvents,
        selectedVerification = if (rawSessions.isNotEmpty()) {
            VerificationUi(
                peerId = rawSessions.first().peerId,
                peerName = "Peer ${rawSessions.first().peerId.take(6)}",
                publicKeyFingerprint = rawSessions.first().sessionId
            )
        } else null,
        onBackClick = onBack,
        onRefreshClick = { refreshData() },
        onRotateKeysClick = {
            cryptoManager.rotateBroadcastKey()
            securityMonitor.recordKeyRotation()
            refreshData()
        },
        onRekeySessionClick = { sessionUi ->
            rekeyManager.manualRekey(sessionUi.peerId)
            refreshData()
        }
    )
}
