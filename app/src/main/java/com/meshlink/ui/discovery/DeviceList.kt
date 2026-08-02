package com.meshlink.ui.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DeviceList — Clean messaging-style lazy list of discovered devices categorized into Connected, Nearby, and Relay.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceList(
    devices: List<NearbyDeviceUiState>,
    connectingAddress: String?,
    selectedAddress: String?,
    searchQuery: String,
    onDeviceClick: (NearbyDeviceUiState) -> Unit,
    onConnectClick: (NearbyDeviceUiState) -> Unit,
    onRefresh: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val connectedDevices = remember(devices) { devices.filter { it.isConnected } }
    val nearbyDevices = remember(devices) { devices.filter { !it.isConnected && !it.hasRelayCapability } }
    val relayDevices = remember(devices) { devices.filter { !it.isConnected && it.hasRelayCapability } }

    if (devices.isEmpty()) {
        NoNearbyDevices(
            onRefresh = onRefresh,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Connected Devices Section
        if (connectedDevices.isNotEmpty()) {
            item(key = "header_connected") {
                SectionHeader(title = "CONNECTED (${connectedDevices.size})")
            }
            items(
                items = connectedDevices,
                key = { it.address }
            ) { dev ->
                NearbyDeviceRow(
                    deviceUi = dev,
                    isConnecting = dev.address == connectingAddress,
                    onDeviceClick = { onDeviceClick(dev) },
                    onConnectClick = { onConnectClick(dev) },
                    modifier = Modifier.animateItem()
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 86.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }

        // 2. Nearby Devices Section
        if (nearbyDevices.isNotEmpty()) {
            item(key = "header_nearby") {
                SectionHeader(title = "NEARBY (${nearbyDevices.size})")
            }
            items(
                items = nearbyDevices,
                key = { it.address }
            ) { dev ->
                NearbyDeviceRow(
                    deviceUi = dev,
                    isConnecting = dev.address == connectingAddress,
                    onDeviceClick = { onDeviceClick(dev) },
                    onConnectClick = { onConnectClick(dev) },
                    modifier = Modifier.animateItem()
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 86.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }

        // 3. Relay Devices Section
        if (relayDevices.isNotEmpty()) {
            item(key = "header_relay") {
                SectionHeader(title = "RELAY NODES (${relayDevices.size})")
            }
            items(
                items = relayDevices,
                key = { it.address }
            ) { dev ->
                NearbyDeviceRow(
                    deviceUi = dev,
                    isConnecting = dev.address == connectingAddress,
                    onDeviceClick = { onDeviceClick(dev) },
                    onConnectClick = { onConnectClick(dev) },
                    modifier = Modifier.animateItem()
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 86.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
