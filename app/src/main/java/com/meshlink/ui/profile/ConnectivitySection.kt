package com.meshlink.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.settings.SettingsUiState

@Composable
fun ConnectivitySection(
    uiState: SettingsUiState,
    onSetBleEnabled: (Boolean) -> Unit,
    onSetBleAdv: (Boolean) -> Unit,
    onSetBleScan: (Boolean) -> Unit,
    onSetTransport: (String) -> Unit,
    onSetRelayEnabled: (Boolean) -> Unit,
    onSetMaxHops: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val connectivityItems = listOf(
            SettingsItemUi(
                id = "ble_power",
                title = "Bluetooth Low Energy (BLE)",
                subtitle = "Primary transport layer for off-grid peer discovery",
                icon = Icons.Default.Bluetooth,
                isChecked = uiState.isBleEnabled,
                onClick = { onSetBleEnabled(!uiState.isBleEnabled) }
            ),
            SettingsItemUi(
                id = "ble_adv",
                title = "BLE Beacon Advertising",
                subtitle = "Broadcast node discovery frames to nearby devices",
                icon = Icons.Default.Radar,
                isChecked = uiState.bleAdvertisingEnabled,
                enabled = uiState.isBleEnabled,
                onClick = { onSetBleAdv(!uiState.bleAdvertisingEnabled) }
            ),
            SettingsItemUi(
                id = "ble_scan",
                title = "Continuous Background Scan",
                subtitle = "Scan active BLE channels every ${uiState.bleScanInterval / 1000}s",
                icon = Icons.Default.WifiTethering,
                isChecked = uiState.bleScanningEnabled,
                enabled = uiState.isBleEnabled,
                onClick = { onSetBleScan(!uiState.bleScanningEnabled) }
            ),
            SettingsItemUi(
                id = "pref_transport",
                title = "Preferred Transport Layer",
                subtitle = "Default transport protocol for data routing",
                icon = Icons.Default.Router,
                trailingText = uiState.preferredTransport,
                onClick = {
                    val next = if (uiState.preferredTransport == "BLE") "Wi-Fi Direct" else "BLE"
                    onSetTransport(next)
                }
            ),
            SettingsItemUi(
                id = "mesh_relay",
                title = "Store-and-Forward Mesh Relay",
                subtitle = "Relay encrypted packets for out-of-range peers",
                icon = Icons.Default.Memory,
                isChecked = uiState.isMeshRelayEnabled,
                onClick = { onSetRelayEnabled(!uiState.isMeshRelayEnabled) }
            ),
            SettingsItemUi(
                id = "max_hops",
                title = "Maximum Packet Hop Limit",
                subtitle = "Limit multi-hop propagation depth",
                icon = Icons.Default.Share,
                trailingText = "${uiState.meshMaxHops} Hops",
                onClick = {
                    val nextHops = if (uiState.meshMaxHops >= 7) 3 else uiState.meshMaxHops + 2
                    onSetMaxHops(nextHops)
                }
            )
        )

        SettingsGroupCard(
            title = "Network & Mesh Connectivity",
            items = connectivityItems
        )
    }
}
