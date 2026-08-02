package com.meshlink.ui.discovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import com.meshlink.ui.designsystem.theme.MeshTheme

private val SamplePreviewDevices = listOf(
    BleDevice(
        meshId = "node_alpha_01",
        name = "Alpha Recon Node",
        address = "AA:BB:CC:DD:EE:01",
        rssi = -55,
        isConnected = true,
        transport = TransportType.BLE,
        distanceMeters = 3.5
    ),
    BleDevice(
        meshId = "node_beta_02",
        name = "Beta Relay Station",
        address = "AA:BB:CC:DD:EE:02",
        rssi = -72,
        isConnected = false,
        transport = TransportType.WIFI_DIRECT,
        capabilities = 0x01
    ),
    BleDevice(
        meshId = "node_gamma_03",
        name = "Gamma Tactical Peer",
        address = "AA:BB:CC:DD:EE:03",
        rssi = -86,
        isConnected = false,
        transport = TransportType.HYBRID
    )
).map { NearbyDeviceUiState.fromDomain(it) }

@Preview(name = "Discovery Dark Phone", showBackground = true)
@Composable
fun DiscoveryPreviewDarkPhone() {
    MeshTheme(themeMode = "DARK") {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                DeviceList(
                    devices = SamplePreviewDevices,
                    connectingAddress = null,
                    selectedAddress = "AA:BB:CC:DD:EE:01",
                    searchQuery = "",
                    onDeviceClick = {},
                    onConnectClick = {}
                )
            }
        }
    }
}

@Preview(name = "Discovery Light Phone", showBackground = true)
@Composable
fun DiscoveryPreviewLightPhone() {
    MeshTheme(themeMode = "LIGHT") {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                DeviceList(
                    devices = SamplePreviewDevices,
                    connectingAddress = null,
                    selectedAddress = null,
                    searchQuery = "",
                    onDeviceClick = {},
                    onConnectClick = {}
                )
            }
        }
    }
}

@Preview(name = "Discovery Tablet", device = Devices.TABLET, showBackground = true)
@Composable
fun DiscoveryPreviewTablet() {
    MeshTheme(themeMode = "DARK") {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(24.dp)) {
                MeshRadarView(
                    devices = SamplePreviewDevices.map { it.device },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
