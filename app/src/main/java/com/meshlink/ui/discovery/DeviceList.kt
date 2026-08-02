package com.meshlink.ui.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.components.nearby.MeshScanningEmptyState
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceList(
    devices: List<NearbyDeviceUiState>,
    connectingAddress: String?,
    selectedAddress: String?,
    searchQuery: String,
    onDeviceClick: (NearbyDeviceUiState) -> Unit,
    onConnectClick: (NearbyDeviceUiState) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    if (devices.isEmpty()) {
        MeshScanningEmptyState(
            title = if (searchQuery.isBlank()) "Scanning for Mesh Peers" else "No matching peers found",
            description = if (searchQuery.isBlank()) "Looking for active Mesh Link devices over BLE & Wi-Fi Direct..." else "Try adjusting your search query or clear filters.",
            modifier = modifier
        )
    } else {
        val connectedPeers = devices.filter { it.isConnected }
        val nearbyPeers = devices.filter { !it.isConnected }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = MeshSpacing.ListBottomSpacing),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            // Connected Peers Category Header & Items
            if (connectedPeers.isNotEmpty()) {
                item(key = "header_connected") {
                    CategoryHeader(
                        title = "Active Connections",
                        count = connectedPeers.size,
                        color = MeshTheme.colors.connected
                    )
                }

                items(
                    items = connectedPeers,
                    key = { it.address },
                    contentType = { "device_card" }
                ) { deviceUi ->
                    Box(modifier = Modifier.animateItem()) {
                        DeviceCard(
                            deviceUi = deviceUi,
                            isConnecting = connectingAddress == deviceUi.address,
                            isSelected = selectedAddress == deviceUi.address,
                            onClick = { onDeviceClick(deviceUi) },
                            onConnectClick = { onConnectClick(deviceUi) }
                        )
                    }
                }
            }

            // Discovered Nearby Peers Category Header & Items
            if (nearbyPeers.isNotEmpty()) {
                item(key = "header_nearby") {
                    CategoryHeader(
                        title = "Discovered Mesh Nodes",
                        count = nearbyPeers.size,
                        color = MeshTheme.colors.primary
                    )
                }

                items(
                    items = nearbyPeers,
                    key = { it.address },
                    contentType = { "device_card" }
                ) { deviceUi ->
                    Box(modifier = Modifier.animateItem()) {
                        DeviceCard(
                            deviceUi = deviceUi,
                            isConnecting = connectingAddress == deviceUi.address,
                            isSelected = selectedAddress == deviceUi.address,
                            onClick = { onDeviceClick(deviceUi) },
                            onConnectClick = { onConnectClick(deviceUi) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MeshSpacing.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
