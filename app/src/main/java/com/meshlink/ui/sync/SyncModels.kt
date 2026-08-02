package com.meshlink.ui.sync

import androidx.compose.runtime.Immutable

@Immutable
data class MeshSyncUiState(
    val syncUi: SyncUi = SyncUi(),
    val queueUi: QueueUi = QueueUi(),
    val retryUi: RetryUi = RetryUi(),
    val deliveryUi: DeliveryUi = DeliveryUi(),
    val offlineUi: OfflineUi = OfflineUi(),
    val queueStatsUi: QueueStatisticsUi = QueueStatisticsUi(),
    val peers: List<PeerStatusUi> = emptyList(),
    val pendingMessages: List<PendingMessageUi> = emptyList(),
    val deliveryHistory: List<DeliveryHistoryUi> = emptyList(),
    val conflicts: List<ConflictUi> = emptyList(),
    val timelineEvents: List<SyncTimelineUi> = emptyList(),
    val recoveryStatus: MeshRecoveryUi = MeshRecoveryUi()
)

@Immutable
data class SyncUi(
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false,
    val progressFraction: Float = 1.0f,
    val remainingItems: Int = 0,
    val speedKbps: Float = 0.0f,
    val estimatedCompletionSec: Long = 0L,
    val currentPhase: String = "Idle",
    val statusMessage: String = "Mesh Synchronized"
)

@Immutable
data class QueueUi(
    val pendingCount: Int = 0,
    val retryingCount: Int = 0,
    val failedCount: Int = 0,
    val processingCount: Int = 0,
    val completedCount: Int = 0
) {
    val totalQueueSize: Int get() = pendingCount + retryingCount + processingCount
}

@Immutable
data class RetryUi(
    val retryCount: Int = 0,
    val stateName: String = "IDLE", // IDLE, WAITING, BACKOFF, IN_PROGRESS
    val nextRetryMs: Long = 0L,
    val lastAttemptMs: Long = 0L,
    val failureReason: String = ""
)

@Immutable
data class DeliveryUi(
    val activeDeliveries: Int = 0,
    val activeRelays: Int = 0,
    val successRate: Float = 1.0f,
    val totalDelivered: Long = 0L,
    val totalForwarded: Long = 0L,
    val totalFailed: Long = 0L
)

@Immutable
data class OfflineUi(
    val cachedMessagesCount: Int = 0,
    val pendingUploadsCount: Int = 0,
    val pendingDownloadsCount: Int = 0,
    val localStorageBytes: Long = 0L
)

enum class PeerAvailabilityStatus {
    ONLINE,
    OFFLINE,
    RECENTLY_SEEN,
    REACHABLE,
    RELAY_AVAILABLE
}

@Immutable
data class PeerStatusUi(
    val peerId: String,
    val name: String,
    val status: PeerAvailabilityStatus,
    val rssi: Int = -70,
    val lastSeenMs: Long = System.currentTimeMillis(),
    val isReachable: Boolean = true,
    val isRelay: Boolean = false
)

@Immutable
data class QueueStatisticsUi(
    val pending: Int = 0,
    val delivered: Int = 0,
    val failed: Int = 0,
    val retried: Int = 0,
    val avgQueueTimeMs: Long = 0L,
    val healthScore: Int = 100
)

@Immutable
data class DeliveryHistoryUi(
    val id: String,
    val messageId: String,
    val destination: String,
    val type: String, // Delivered, Queued, Failed, Retried, Forwarded
    val timestamp: Long,
    val status: String,
    val retryCount: Int = 0
)

@Immutable
data class ConflictUi(
    val id: String,
    val messageId: String,
    val conflictType: String, // Duplicate Messages, Pending Merge, Sync Conflict, Resolved
    val timestamp: Long,
    val status: String,
    val details: String
)

@Immutable
data class SyncTimelineUi(
    val id: String,
    val eventType: String, // Message Queued, Peer Connected, Route Established, Delivered, Retry, Sync Complete
    val title: String,
    val detail: String,
    val timestamp: Long,
    val statusLevel: String = "INFO" // INFO, SUCCESS, WARNING, ERROR
)

@Immutable
data class PendingMessageUi(
    val id: String,
    val recipientId: String,
    val recipientName: String,
    val previewText: String,
    val hasAttachment: Boolean = false,
    val priority: String = "NORMAL",
    val timestamp: Long = System.currentTimeMillis(),
    val nextRetryMs: Long = 0L,
    val attemptCount: Int = 0,
    val status: String = "QUEUED"
)

@Immutable
data class MeshRecoveryUi(
    val isReconnecting: Boolean = false,
    val peersDiscoveredCount: Int = 0,
    val routesRebuiltCount: Int = 0,
    val isMeshRestored: Boolean = true,
    val lastRecoveryMs: Long = System.currentTimeMillis(),
    val statusText: String = "Mesh operational"
)
