package com.meshlink.ui.sos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun EmergencyStatusCard(
    state: SosUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.onSurfaceVariant
        SosStatus.BROADCASTING -> MeshTheme.colors.warning
        SosStatus.DELIVERED -> MeshTheme.colors.success
        SosStatus.FAILED -> MeshTheme.colors.danger
    }

    val cardBg = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.surfaceVariant
        SosStatus.BROADCASTING -> MeshTheme.colors.warning.copy(alpha = 0.15f)
        SosStatus.DELIVERED -> MeshTheme.colors.success.copy(alpha = 0.15f)
        SosStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (state.status) {
                        SosStatus.SAFE -> Icons.Default.Info
                        SosStatus.BROADCASTING -> Icons.Default.Radar
                        SosStatus.DELIVERED -> Icons.Default.CheckCircle
                        SosStatus.FAILED -> Icons.Default.ErrorOutline
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EMERGENCY STATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = when (state.status) {
                            SosStatus.SAFE -> "Ready to Broadcast"
                            SosStatus.BROADCASTING -> "Broadcasting Alert via Mesh..."
                            SosStatus.DELIVERED -> "Delivered to Mesh Responders"
                            SosStatus.FAILED -> "Transmission Failed"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }

                if (state.status == SosStatus.DELIVERED || state.status == SosStatus.BROADCASTING) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = "Cancel SOS",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else if (state.status == SosStatus.FAILED) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry Transmission",
                            tint = MeshTheme.colors.danger
                        )
                    }
                }
            }

            // Supplemental Status Detail
            if (state.status == SosStatus.DELIVERED) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = statusColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reached ${state.relaysReached} active relay devices across local mesh cluster.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
