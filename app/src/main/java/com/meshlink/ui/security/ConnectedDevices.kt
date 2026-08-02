package com.meshlink.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun ConnectedDevicesSection(
    connectedDevices: List<DeviceTrustUi>,
    modifier: Modifier = Modifier,
    onDeviceClick: ((DeviceTrustUi) -> Unit)? = null,
    onVerifyClick: ((DeviceTrustUi) -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Connected Mesh Devices (${connectedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (connectedDevices.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Security,
                title = "No Active Mesh Sessions",
                description = "Nearby mesh nodes will appear here once secure keys are exchanged."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)) {
                connectedDevices.forEach { device ->
                    DeviceTrustCard(
                        device = device,
                        onClick = if (onDeviceClick != null) { { onDeviceClick(device) } } else null,
                        onVerifyClick = if (onVerifyClick != null) { { onVerifyClick(device) } } else null
                    )
                }
            }
        }
    }
}
