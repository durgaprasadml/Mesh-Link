package com.meshlink.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.ui.designsystem.theme.MeshTheme

private val sampleUiState = MeshSyncUiState(
    syncUi = SyncUi(
        isOffline = false,
        isSyncing = true,
        progressFraction = 0.65f,
        remainingItems = 12,
        speedKbps = 48.5f,
        estimatedCompletionSec = 5L,
        currentPhase = "Syncing Vector Clock",
        statusMessage = "12 Items Queued for Peer Delivery"
    ),
    queueUi = QueueUi(
        pendingCount = 8,
        retryingCount = 3,
        failedCount = 1,
        processingCount = 2,
        completedCount = 142
    ),
    retryUi = RetryUi(
        retryCount = 4,
        stateName = "BACKOFF",
        nextRetryMs = 3500L,
        lastAttemptMs = System.currentTimeMillis() - 12000L,
        failureReason = "Route timeout to target node"
    ),
    deliveryUi = DeliveryUi(
        activeDeliveries = 2,
        activeRelays = 5,
        successRate = 0.96f,
        totalDelivered = 230L,
        totalForwarded = 840L,
        totalFailed = 3L
    ),
    offlineUi = OfflineUi(
        cachedMessagesCount = 45,
        pendingUploadsCount = 8,
        pendingDownloadsCount = 2,
        localStorageBytes = 4850000L
    ),
    queueStatsUi = QueueStatisticsUi(
        pending = 8,
        delivered = 230,
        failed = 3,
        retried = 4,
        avgQueueTimeMs = 120L,
        healthScore = 94
    ),
    peers = listOf(
        PeerStatusUi("peer_alpha", "Alpha Tactical Node", PeerAvailabilityStatus.ONLINE, -55, System.currentTimeMillis(), true, true),
        PeerStatusUi("peer_bravo", "Bravo Emergency Relay", PeerAvailabilityStatus.RELAY_AVAILABLE, -68, System.currentTimeMillis(), true, true),
        PeerStatusUi("peer_charlie", "Charlie Mobile Base", PeerAvailabilityStatus.RECENTLY_SEEN, -82, System.currentTimeMillis() - 45000L, false, false),
        PeerStatusUi("peer_delta", "Delta Field Patrol", PeerAvailabilityStatus.OFFLINE, -95, System.currentTimeMillis() - 180000L, false, false)
    ),
    pendingMessages = listOf(
        PendingMessageUi("msg_1", "peer_alpha", "Alpha Tactical Node", "Coordinates updated for evacuation zone B", false, "HIGH", System.currentTimeMillis() - 5000L, 0L, 1, "PROCESSING"),
        PendingMessageUi("msg_2", "peer_bravo", "Bravo Emergency Relay", "Resource manifest file attached", true, "NORMAL", System.currentTimeMillis() - 15000L, 3500L, 2, "RETRYING")
    ),
    deliveryHistory = listOf(
        DeliveryHistoryUi("h_1", "msg_102", "peer_alpha", "Delivered", System.currentTimeMillis() - 2000L, "Ack received", 0),
        DeliveryHistoryUi("h_2", "msg_101", "peer_bravo", "Forwarded", System.currentTimeMillis() - 10000L, "Relayed via Charlie", 0)
    ),
    conflicts = listOf(
        ConflictUi("c_1", "msg_88", "Duplicate Messages", System.currentTimeMillis() - 60000L, "Ignored", "Identical message hash dropped by CRDT deduplicator")
    ),
    timelineEvents = listOf(
        SyncTimelineUi("t_1", "Message Queued", "Message Queued for Alpha", "Size: 240 bytes", System.currentTimeMillis() - 5000L, "INFO"),
        SyncTimelineUi("t_2", "Peer Connected", "Peer Connected: Alpha Node", "BLE Transport direct route established", System.currentTimeMillis() - 10000L, "SUCCESS")
    ),
    recoveryStatus = MeshRecoveryUi(
        isReconnecting = false,
        peersDiscoveredCount = 3,
        routesRebuiltCount = 4,
        isMeshRestored = true,
        lastRecoveryMs = System.currentTimeMillis() - 30000L,
        statusText = "Mesh network operational"
    )
)

@Preview(name = "Light Mode Phone", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun MeshSyncScreenLightPreview() {
    MeshTheme(themeMode = "LIGHT") {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}

@Preview(name = "Dark Mode Phone", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun MeshSyncScreenDarkPreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}

@Preview(name = "AMOLED Dark Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun MeshSyncScreenAmoledPreview() {
    MeshTheme(themeMode = "DARK", amoledDark = true) {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}

@Preview(name = "Tablet Wide Screen", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
fun MeshSyncScreenTabletPreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}

@Preview(name = "Foldable Layout", widthDp = 600, heightDp = 800, showBackground = true)
@Composable
fun MeshSyncScreenFoldablePreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}

@Preview(name = "Landscape Mode", widthDp = 840, heightDp = 400, showBackground = true)
@Composable
fun MeshSyncScreenLandscapePreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}

@Preview(name = "Large Font Scaling", fontScale = 1.5f, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun MeshSyncScreenLargeFontPreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}

@Preview(name = "RTL Layout Preview", locale = "ar", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun MeshSyncScreenRtlPreview() {
    MeshTheme(themeMode = "DARK") {
        MeshSyncScreen(state = sampleUiState, onBackClick = {})
    }
}
