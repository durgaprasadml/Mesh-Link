package com.meshlink.ui.notifications

import androidx.compose.runtime.Immutable

enum class NotificationCategory(val label: String) {
    ALL("All"),
    MESSAGES("Messages"),
    BROADCASTS("Broadcasts"),
    NEARBY_DEVICES("Nearby Devices"),
    SOS("SOS"),
    TRANSFERS("Transfers"),
    ANALYTICS("Analytics"),
    SECURITY("Security"),
    SYSTEM("System")
}

enum class NotificationStatus(val label: String) {
    DELIVERED("Delivered"),
    READ("Read"),
    DISMISSED("Dismissed"),
    MISSED("Missed"),
    FAILED("Failed")
}

enum class PriorityAlertType(val label: String) {
    SOS_ALERT("Emergency SOS"),
    FAILED_DELIVERY("Delivery Failed"),
    CONNECTION_LOST("Mesh Connection Lost"),
    CRITICAL_SECURITY("Security Alert"),
    BATTERY_WARNING("Battery Optimization")
}

enum class ServiceState(val label: String) {
    RUNNING("Running"),
    PAUSED("Paused"),
    STOPPED("Stopped"),
    WAITING("Waiting")
}

@Immutable
data class NotificationItemUi(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val category: NotificationCategory,
    val status: NotificationStatus = NotificationStatus.DELIVERED,
    val senderName: String? = null,
    val avatarUri: String? = null,
    val isRead: Boolean = false,
    val actionLabel: String? = null
)

@Immutable
data class PriorityAlertUi(
    val id: String,
    val title: String,
    val message: String,
    val alertType: PriorityAlertType,
    val timestamp: String,
    val actionLabel: String? = null,
    val isDismissed: Boolean = false
)

@Immutable
data class BackgroundServiceUi(
    val serviceId: String,
    val serviceName: String,
    val description: String,
    val state: ServiceState,
    val activeConnections: Int = 0,
    val uptime: String = "Active"
)

@Immutable
data class MeshStatusUi(
    val isConnected: Boolean = true,
    val peerCount: Int = 8,
    val activeRelays: Int = 3,
    val queueSize: Int = 0,
    val networkReach: String = "High (94%)",
    val isRelayEnabled: Boolean = true
)

@Immutable
data class NotificationCategoryUi(
    val category: NotificationCategory,
    val title: String,
    val unreadCount: Int = 0,
    val isEnabled: Boolean = true
)

@Immutable
data class NotificationHistoryUi(
    val id: String,
    val title: String,
    val timestamp: String,
    val statusBadge: NotificationStatus,
    val category: NotificationCategory
)

@Immutable
data class NotificationSettingsUi(
    val messageAlerts: Boolean = true,
    val broadcastAlerts: Boolean = true,
    val sosAlerts: Boolean = true,
    val nearbyDiscovery: Boolean = true,
    val transferAlerts: Boolean = true,
    val analyticsAlerts: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val badgeCountEnabled: Boolean = true
)

@Immutable
data class NotificationCenterUiState(
    val searchQuery: String = "",
    val selectedCategory: NotificationCategory = NotificationCategory.ALL,
    val priorityAlerts: List<PriorityAlertUi> = emptyList(),
    val notifications: List<NotificationItemUi> = emptyList(),
    val backgroundServices: List<BackgroundServiceUi> = emptyList(),
    val meshStatus: MeshStatusUi = MeshStatusUi(),
    val categories: List<NotificationCategoryUi> = emptyList(),
    val historyItems: List<NotificationHistoryUi> = emptyList(),
    val settings: NotificationSettingsUi = NotificationSettingsUi(),
    val isLoading: Boolean = false
)
