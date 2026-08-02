package com.meshlink.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun EmergencyHero(
    uiState: EmergencyUiState,
    modifier: Modifier = Modifier
) {
    val state = uiState.rawState

    val statusColor = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.onSurfaceVariant
        SosStatus.BROADCASTING -> MeshTheme.colors.warning
        SosStatus.DELIVERED -> MeshTheme.colors.success
        SosStatus.FAILED -> MeshTheme.colors.danger
    }

    val containerBg = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.surfaceContainerHigh
        SosStatus.BROADCASTING -> MeshTheme.colors.warning.copy(alpha = 0.12f)
        SosStatus.DELIVERED -> MeshTheme.colors.success.copy(alpha = 0.12f)
        SosStatus.FAILED -> MeshTheme.colors.danger.copy(alpha = 0.15f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Priority / Stage Label
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (state.status) {
                                SosStatus.SAFE -> Icons.Default.Security
                                SosStatus.BROADCASTING -> Icons.Default.Radar
                                SosStatus.DELIVERED -> Icons.Default.CheckCircle
                                SosStatus.FAILED -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (state.status) {
                                SosStatus.SAFE -> "EMERGENCY STANDBY"
                                SosStatus.BROADCASTING -> "BROADCASTING DISTRESS"
                                SosStatus.DELIVERED -> "SOS ACKNOWLEDGED"
                                SosStatus.FAILED -> "TRANSMISSION FAILED"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = statusColor
                        )
                    }
                }

                // Responders counter
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${state.nearbyResponders.size} Mesh Responders",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Hero Text
            Text(
                text = when (state.status) {
                    SosStatus.SAFE -> "Ready to Broadcast Emergency Alert"
                    SosStatus.BROADCASTING -> "Broadcasting Emergency Packet Across Mesh..."
                    SosStatus.DELIVERED -> "SOS Delivered & Reached Mesh Network"
                    SosStatus.FAILED -> "Broadcasting Failed — Retry Immediate"
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (state.status) {
                    SosStatus.SAFE -> "Press and hold the SOS button for 3 seconds to emit high-priority distress signal over BLE & Wi-Fi Direct mesh."
                    SosStatus.BROADCASTING -> "Transmitting encrypted GPS coordinates to nearby mesh nodes. Relaying across peers..."
                    SosStatus.DELIVERED -> "Distress signal confirmed delivered to ${state.relaysReached} mesh nodes in area."
                    SosStatus.FAILED -> state.errorMessage ?: "Network packet delivery failed. Please check radio connectivity and retry."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )

            if (state.isSending || state.status == SosStatus.BROADCASTING) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MeshTheme.colors.warning,
                    trackColor = MeshTheme.colors.warning.copy(alpha = 0.2f)
                )
            }
        }
    }
}
