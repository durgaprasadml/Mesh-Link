package com.meshlink.ui.notifications

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.ui.designsystem.theme.MeshTheme

object SampleNotificationData {
    val sampleState = NotificationCenterUiState(
        priorityAlerts = listOf(
            PriorityAlertUi(
                id = "alert_1",
                title = "Emergency SOS Beacon Detected",
                message = "Node 'Node_Echo' initiated emergency broadcast in sector 4.",
                alertType = PriorityAlertType.SOS_ALERT,
                timestamp = "2 mins ago",
                actionLabel = "View SOS Map"
            ),
            PriorityAlertUi(
                id = "alert_2",
                title = "Battery Optimization Warning",
                message = "Background BLE scanning restricted by OS battery saver.",
                alertType = PriorityAlertType.BATTERY_WARNING,
                timestamp = "10 mins ago",
                actionLabel = "Fix Settings"
            )
        ),
        notifications = listOf(
            NotificationItemUi(
                id = "notif_1",
                title = "Sarah Jenkins",
                message = "Received packet relay: 'We have re-established relay hop #3.'",
                timestamp = "Just now",
                category = NotificationCategory.MESSAGES,
                senderName = "Sarah Jenkins",
                actionLabel = "Reply"
            ),
            NotificationItemUi(
                id = "notif_2",
                title = "Mesh Broadcast",
                message = "Community Channel: Weather advisory issued for northern region.",
                timestamp = "15 mins ago",
                category = NotificationCategory.BROADCASTS
            ),
            NotificationItemUi(
                id = "notif_3",
                title = "New Node Discovered",
                message = "Node 'Mesh_Alpha_99' joined BLE range (RSSI -64dBm).",
                timestamp = "1 hour ago",
                category = NotificationCategory.NEARBY_DEVICES,
                isRead = true
            ),
            NotificationItemUi(
                id = "notif_4",
                title = "File Transfer Complete",
                message = "Received 'topographic_map_v2.pdf' via Wi-Fi Direct.",
                timestamp = "Yesterday",
                category = NotificationCategory.TRANSFERS,
                isRead = true
            )
        ),
        backgroundServices = listOf(
            BackgroundServiceUi(
                serviceId = "mesh_svc",
                serviceName = "Mesh Relay Service",
                description = "Store-and-forward packet routing",
                state = ServiceState.RUNNING,
                activeConnections = 8,
                uptime = "4h 12m"
            ),
            BackgroundServiceUi(
                serviceId = "discovery_svc",
                serviceName = "Discovery Engine",
                description = "BLE & Wi-Fi Direct peer scanner",
                state = ServiceState.RUNNING,
                activeConnections = 12,
                uptime = "4h 12m"
            ),
            BackgroundServiceUi(
                serviceId = "sync_svc",
                serviceName = "Database Sync Engine",
                description = "CRDT message sync queue",
                state = ServiceState.WAITING,
                activeConnections = 0,
                uptime = "Idle"
            )
        ),
        categories = listOf(
            NotificationCategoryUi(NotificationCategory.MESSAGES, "Messages", unreadCount = 1),
            NotificationCategoryUi(NotificationCategory.BROADCASTS, "Broadcasts", unreadCount = 1),
            NotificationCategoryUi(NotificationCategory.SOS, "SOS Alerts", unreadCount = 1),
            NotificationCategoryUi(NotificationCategory.NEARBY_DEVICES, "Nearby", unreadCount = 0),
            NotificationCategoryUi(NotificationCategory.TRANSFERS, "Transfers", unreadCount = 0),
            NotificationCategoryUi(NotificationCategory.SECURITY, "Security", unreadCount = 0)
        ),
        historyItems = listOf(
            NotificationHistoryUi(
                id = "h1",
                title = "Key Exchange Completed with Node_Echo",
                timestamp = "Today 12:40 PM",
                statusBadge = NotificationStatus.DELIVERED,
                category = NotificationCategory.SECURITY
            ),
            NotificationHistoryUi(
                id = "h2",
                title = "Broadcast Digest Sync Failed",
                timestamp = "Yesterday 09:15 AM",
                statusBadge = NotificationStatus.FAILED,
                category = NotificationCategory.BROADCASTS
            )
        )
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MeshNotificationScreenPreview() {
    MeshTheme {
        Surface {
            MeshNotificationScreen(
                state = SampleNotificationData.sampleState,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }
    }
}

@Preview(name = "Tablet Expanded", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun MeshNotificationScreenTabletPreview() {
    MeshTheme {
        Surface {
            MeshNotificationScreen(
                state = SampleNotificationData.sampleState,
                windowWidthSizeClass = WindowWidthSizeClass.Expanded
            )
        }
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
fun MeshNotificationEmptyPreview() {
    MeshTheme {
        Surface {
            MeshNotificationScreen(
                state = NotificationCenterUiState(
                    notifications = emptyList(),
                    priorityAlerts = emptyList()
                ),
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }
    }
}
