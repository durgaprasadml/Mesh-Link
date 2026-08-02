package com.meshlink.ui.discovery

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType

/**
 * Connection Quality tier derived strictly from RSSI dBm.
 */
enum class ConnectionQuality(val label: String, val colorHex: Long) {
    EXCELLENT("Excellent", 0xFF00F59B), // Cyber Mint
    GOOD("Good", 0xFF00E5FF),      // Quantum Cyan
    FAIR("Fair", 0xFFFFB703),      // Warning Amber
    POOR("Weak", 0xFFFF0055),      // Crimson Red
    DISCONNECTED("Disconnected", 0xFF71717A); // Muted Gray

    companion object {
        fun fromRssi(rssi: Int): ConnectionQuality = when {
            rssi > -60 -> EXCELLENT
            rssi in -74..-60 -> GOOD
            rssi in -88..-75 -> FAIR
            rssi <= -88 && rssi != 0 -> POOR
            else -> DISCONNECTED
        }
    }
}

/**
 * Signal Strength calculations and visual metrics.
 */
@Immutable
data class SignalStrength(
    val rssi: Int,
    val percentage: Int,
    val barCount: Int,
    val quality: ConnectionQuality
) {
    companion object {
        fun fromRssi(rssi: Int): SignalStrength {
            val clamped = rssi.coerceIn(-100, -40)
            val percentage = (((clamped + 100).toFloat() / 60f) * 100).toInt().coerceIn(0, 100)
            val bars = when {
                rssi > -60 -> 4
                rssi > -72 -> 3
                rssi > -85 -> 2
                rssi != 0 -> 1
                else -> 0
            }
            return SignalStrength(
                rssi = rssi,
                percentage = percentage,
                barCount = bars,
                quality = ConnectionQuality.fromRssi(rssi)
            )
        }
    }
}

/**
 * RSSI-based or backend-provided Distance Categories.
 * Prevents hallucinated pixel/meter values when exact distance is null.
 */
enum class DistanceCategory(val label: String) {
    NEAR("Near (< 10m)"),
    MEDIUM("Medium (10 - 30m)"),
    FAR("Far (> 30m)"),
    UNKNOWN("Unknown Range");

    companion object {
        fun fromDevice(device: BleDevice): DistanceCategory {
            if (device.distanceMeters != null) {
                val d = device.distanceMeters
                return when {
                    d < 10.0 -> NEAR
                    d <= 30.0 -> MEDIUM
                    else -> FAR
                }
            }
            return when {
                device.rssi > -65 -> NEAR
                device.rssi in -85..-65 -> MEDIUM
                device.rssi < -85 && device.rssi != 0 -> FAR
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Visual status for mesh node discovery.
 */
enum class DeviceStatus(val label: String) {
    DISCOVERED("Discovered"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    DIRECT_PEER("Direct Peer"),
    RELAY_PEER("Relay Node")
}

/**
 * Mesh Node Categorization for tactical canvas.
 */
enum class MeshNodeType(val label: String) {
    LOCAL_HUB("Local Hub"),
    DIRECT_NODE("Direct Peer"),
    RELAY_NODE("Relay Node"),
    MULTI_HOP_NODE("Multi-Hop Node")
}

/**
 * UI Adapter wrapping domain TransportType with visual metadata.
 */
enum class TransportTypeUi(val label: String, val badgeColor: Color) {
    BLE("Bluetooth LE", Color(0xFF0284C7)),
    WIFI_DIRECT("Wi-Fi Direct", Color(0xFF8B5CF6)),
    HYBRID("Hybrid Dual", Color(0xFF00F59B));

    companion object {
        fun fromDomain(type: TransportType): TransportTypeUi = when (type) {
            TransportType.BLE -> BLE
            TransportType.WIFI_DIRECT -> WIFI_DIRECT
            TransportType.HYBRID -> HYBRID
        }
    }
}

/**
 * Rich Presentation UI Model wrapping BleDevice.
 */
@Immutable
data class NearbyDeviceUiState(
    val device: BleDevice,
    val meshId: String = device.meshId,
    val name: String = device.name.ifBlank { "Nearby Mesh Node" },
    val address: String = device.address,
    val rssi: Int = device.rssi,
    val signal: SignalStrength = SignalStrength.fromRssi(device.rssi),
    val distanceCategory: DistanceCategory = DistanceCategory.fromDevice(device),
    val formattedDistance: String = device.distanceMeters?.let { "%.1fm".format(it) } ?: distanceCategory.label,
    val transportUi: TransportTypeUi = TransportTypeUi.fromDomain(device.transport),
    val nodeType: MeshNodeType = if (device.isConnected) MeshNodeType.DIRECT_NODE else MeshNodeType.RELAY_NODE,
    val isConnected: Boolean = device.isConnected,
    val avatarUri: String? = device.avatarUri,
    val capabilities: Byte = device.capabilities,
    val hasRelayCapability: Boolean = (device.capabilities.toInt() and 0x01) != 0,
    val isEncrypted: Boolean = true
) {
    companion object {
        fun fromDomain(device: BleDevice): NearbyDeviceUiState = NearbyDeviceUiState(device = device)
    }
}
