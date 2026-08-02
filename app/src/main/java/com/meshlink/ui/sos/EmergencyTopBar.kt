package com.meshlink.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyTopBar(
    state: SosUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state.status) {
                                        SosStatus.SAFE -> MeshTheme.colors.success
                                        SosStatus.BROADCASTING -> MeshTheme.colors.warning
                                        SosStatus.DELIVERED -> MeshTheme.colors.success
                                        SosStatus.FAILED -> MeshTheme.colors.danger
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EMERGENCY SOS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            ),
                            color = MeshTheme.colors.danger
                        )
                    }
                    Text(
                        text = "Tactical Mesh Protocol v2",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Battery Badge
                TelemetryBadge(
                    icon = Icons.Default.BatteryStd,
                    text = "${state.batteryPercent}%",
                    color = if (state.batteryPercent < 20) MeshTheme.colors.danger else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS Status Badge
                TelemetryPill(
                    icon = Icons.Default.LocationOn,
                    label = if (state.isFetchingLocation) "Acquiring..." else if (state.latitude != null) "GPS Fixed" else "No GPS",
                    isActive = state.latitude != null,
                    activeColor = MeshTheme.colors.success,
                    inactiveColor = MeshTheme.colors.warning
                )

                // Mesh Nodes Badge
                TelemetryPill(
                    icon = Icons.Default.Wifi,
                    label = "${state.nearbyResponders.size} Nodes",
                    isActive = state.nearbyResponders.isNotEmpty(),
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Encrypted Protocol Badge
                TelemetryPill(
                    icon = Icons.Default.Lock,
                    label = "AES-256",
                    isActive = true,
                    activeColor = MeshTheme.colors.success,
                    inactiveColor = MaterialTheme.colorScheme.onSurface
                )

                // Transports Badge
                if (state.isBleEnabled || state.isWifiDirectEnabled) {
                    TelemetryPill(
                        icon = Icons.Default.Bluetooth,
                        label = if (state.isBleEnabled && state.isWifiDirectEnabled) "Hybrid" else if (state.isBleEnabled) "BLE" else "P2P",
                        isActive = true,
                        activeColor = MaterialTheme.colorScheme.tertiary,
                        inactiveColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryBadge(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun TelemetryPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    val currentThemeColor = if (isActive) activeColor else inactiveColor

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = currentThemeColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, currentThemeColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = currentThemeColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = currentThemeColor
            )
        }
    }
}
