package com.meshlink.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ConnectedDevicesSection — Pinned section for active connected mesh peers.
 */
@Composable
fun ConnectedDevicesSection(
    connectedDevices: List<NearbyDeviceUiState>,
    connectingAddress: String?,
    onDeviceClick: (NearbyDeviceUiState) -> Unit,
    onConnectClick: (NearbyDeviceUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (connectedDevices.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00F59B))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CONNECTED DEVICED (${connectedDevices.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        connectedDevices.forEach { dev ->
            NearbyDeviceRow(
                deviceUi = dev,
                isConnecting = dev.address == connectingAddress,
                onDeviceClick = { onDeviceClick(dev) },
                onConnectClick = { onConnectClick(dev) }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
