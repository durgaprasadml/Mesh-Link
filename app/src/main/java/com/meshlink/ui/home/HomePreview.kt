package com.meshlink.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.TransportType
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.designsystem.theme.MeshTheme

@Preview(name = "Tactical Hero Section", showBackground = true)
@Composable
fun HomeHeroSectionPreview() {
    MeshTheme {
        HomeHeroSection(
            userIdentity = UserIdentity(
                userId = "node_alpha_123",
                displayName = "Tactical Operator",
                lastUpdated = System.currentTimeMillis()
            ),
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Mesh Overview Telemetry", showBackground = true)
@Composable
fun MeshOverviewSectionPreview() {
    MeshTheme {
        MeshOverviewSection(
            nearbyDevices = listOf(
                BleDevice(
                    meshId = "node_1",
                    name = "Alpha Squad",
                    address = "AA:BB:CC:DD:EE:FF",
                    rssi = -55,
                    transport = TransportType.HYBRID,
                    isConnected = true
                ),
                BleDevice(
                    meshId = "node_2",
                    name = "Bravo Base",
                    address = "11:22:33:44:55:66",
                    rssi = -72,
                    transport = TransportType.BLE,
                    isConnected = false
                )
            ),
            onNavigateToNearby = {}
        )
    }
}

@Preview(name = "Quick Actions Section", showBackground = true)
@Composable
fun QuickActionsSectionPreview() {
    MeshTheme {
        QuickActionsSection(
            onNavigateToNearby = {},
            onNavigateToBroadcast = {},
            onNavigateToSos = {},
            onNavigateToDiagnostics = {},
            onStartConversation = {},
            nearbyCount = 2
        )
    }
}
