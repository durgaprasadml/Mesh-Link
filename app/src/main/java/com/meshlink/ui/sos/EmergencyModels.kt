package com.meshlink.ui.sos

import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType

enum class EmergencyStage {
    SAFE,
    COUNTDOWN,
    BROADCASTING,
    DELIVERED,
    FAILED,
    CANCELLED
}

enum class EmergencyPriority(val label: String) {
    CRITICAL("CRITICAL - SOS"),
    HIGH("HIGH PRIORITY"),
    MEDIUM("STANDARD ALERT"),
    LOW("ADVISORY")
}

data class EmergencyContactUi(
    val address: String,
    val name: String,
    val transport: TransportType,
    val isConnected: Boolean = true,
    val relayBadge: String = "1-Hop"
) {
    companion object {
        fun fromDomain(device: BleDevice): EmergencyContactUi {
            return EmergencyContactUi(
                address = device.address,
                name = device.name.ifBlank { "Responder ${device.address.takeLast(4)}" },
                transport = device.transport,
                isConnected = true,
                relayBadge = when (device.transport) {
                    TransportType.BLE -> "BLE Node"
                    TransportType.WIFI_DIRECT -> "Wi-Fi P2P"
                    TransportType.HYBRID -> "Mesh Relay"
                }
            )
        }
    }
}

data class EmergencyLocationUi(
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val isFetching: Boolean,
    val batteryPercent: Int,
    val formattedCoordinates: String = if (latitude != null && longitude != null) {
        String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude)
    } else {
        "Acquiring GPS Fix..."
    }
)

data class EmergencyDeliveryUi(
    val status: SosStatus,
    val isSending: Boolean,
    val relaysReached: Int,
    val errorMessage: String?,
    val progressFraction: Float = when (status) {
        SosStatus.SAFE -> 0f
        SosStatus.BROADCASTING -> 0.65f
        SosStatus.DELIVERED -> 1.0f
        SosStatus.FAILED -> 0.35f
    }
)

data class EmergencyTimelineItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val stage: EmergencyStage
)

data class EmergencyStatistics(
    val respondersCount: Int,
    val relaysReached: Int,
    val meshHealth: String,
    val isBleActive: Boolean,
    val isWifiDirectActive: Boolean
)

data class EmergencyUiState(
    val rawState: SosUiState,
    val stage: EmergencyStage = when (rawState.status) {
        SosStatus.SAFE -> EmergencyStage.SAFE
        SosStatus.BROADCASTING -> EmergencyStage.BROADCASTING
        SosStatus.DELIVERED -> EmergencyStage.DELIVERED
        SosStatus.FAILED -> EmergencyStage.FAILED
    },
    val locationUi: EmergencyLocationUi = EmergencyLocationUi(
        latitude = rawState.latitude,
        longitude = rawState.longitude,
        address = rawState.address,
        isFetching = rawState.isFetchingLocation,
        batteryPercent = rawState.batteryPercent
    ),
    val contactsUi: List<EmergencyContactUi> = rawState.nearbyResponders.map { EmergencyContactUi.fromDomain(it) },
    val deliveryUi: EmergencyDeliveryUi = EmergencyDeliveryUi(
        status = rawState.status,
        isSending = rawState.isSending,
        relaysReached = rawState.relaysReached,
        errorMessage = rawState.errorMessage
    ),
    val statistics: EmergencyStatistics = EmergencyStatistics(
        respondersCount = rawState.nearbyResponders.size,
        relaysReached = rawState.relaysReached,
        meshHealth = rawState.meshHealth,
        isBleActive = rawState.isBleEnabled,
        isWifiDirectActive = rawState.isWifiDirectEnabled
    )
)
