package com.meshlink.ui.sos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun EmergencyDiagnostics(
    state: SosUiState,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Diagnostics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TACTICAL DIAGNOSTICS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse Diagnostics" else "Expand Diagnostics",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    DiagnosticRow(
                        icon = Icons.Default.Bluetooth,
                        title = "Bluetooth Low Energy (BLE)",
                        value = if (state.isBleEnabled) "Active & Advertising" else "Disabled / Off",
                        isSuccess = state.isBleEnabled
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        icon = Icons.Default.WifiTethering,
                        title = "Wi-Fi Direct P2P Mesh",
                        value = if (state.isWifiDirectEnabled) "Active & Listening" else "Disabled",
                        isSuccess = state.isWifiDirectEnabled
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        icon = Icons.Default.Lock,
                        title = "Payload Encryption",
                        value = "AES-256 / Elliptic Curve DH",
                        isSuccess = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow(
                        icon = Icons.Default.BatteryStd,
                        title = "Device Power Level",
                        value = "${state.batteryPercent}% Remaining",
                        isSuccess = state.batteryPercent >= 20
                    )

                    if (state.isFlashlightOn) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticRow(
                            icon = Icons.Default.FlashlightOn,
                            title = "High-Lumen Flashlight",
                            value = "Strobe / Beacon Active",
                            isSuccess = true
                        )
                    }

                    if (state.isAlarmPlaying) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Distress Siren Alarm",
                            value = "Max Volume Playing",
                            isSuccess = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    title: String,
    value: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isSuccess) MeshTheme.colors.success else MeshTheme.colors.warning,
            modifier = Modifier.size(16.dp)
        )
    }
}
