package com.meshlink.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.MeshIdNormalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailSheet(
    deviceUi: NearbyDeviceUiState?,
    onDismiss: () -> Unit,
    onConnectChat: (NearbyDeviceUiState) -> Unit,
    isConnecting: Boolean,
    modifier: Modifier = Modifier
) {
    if (deviceUi == null) return

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = MeshTheme.elevation.level3
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = MeshSpacing.ScreenPadding)
                .padding(bottom = MeshSpacing.XL)
        ) {
            // Header: Close Action & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Node Tactical Inspector",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close inspector",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshSpacing.MD))

            // Device Avatar & Primary Info Card
            MeshGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = MeshSpacing.CardCornerRadius,
                glowColor = if (deviceUi.isConnected) MeshTheme.colors.connected else MeshTheme.colors.primary,
                glowRadius = 160f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MeshSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = deviceUi.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MeshTheme.colors.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(MeshSpacing.MD))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deviceUi.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = MeshIdNormalizer.canonicalize(deviceUi.address),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshSpacing.LG))

            // Detailed Property Grid
            Text(
                text = "Telemetry & Security Attributes",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MeshSpacing.SM))

            Column(
                verticalArrangement = Arrangement.spacedBy(MeshSpacing.SM)
            ) {
                DetailRow(
                    label = "Signal Strength (RSSI)",
                    value = "${deviceUi.signal.rssi} dBm (${deviceUi.signal.quality.label})"
                )
                DetailRow(
                    label = "Estimated Range",
                    value = deviceUi.formattedDistance
                )
                DetailRow(
                    label = "Primary Transport Protocol",
                    value = deviceUi.transportUi.label
                )
                DetailRow(
                    label = "Encryption Status",
                    value = "AES-256-GCM Encrypted Link"
                )
                DetailRow(
                    label = "Relay Node Capability",
                    value = if (deviceUi.hasRelayCapability) "Supported (Packet Forwarding Active)" else "Standard Peer Node"
                )
            }

            Spacer(modifier = Modifier.height(MeshSpacing.XL))

            // Primary Launch Action
            ElevatedButton(
                onClick = { onConnectChat(deviceUi) },
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MeshTheme.colors.primary,
                    contentColor = Color.White
                )
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Start Chat",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(MeshSpacing.SM))
                    Text(
                        text = if (deviceUi.isConnected) "Open Tactical Chat" else "Connect & Start Chat",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = MeshSpacing.MD, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
