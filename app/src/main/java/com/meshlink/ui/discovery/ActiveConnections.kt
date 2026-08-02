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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.MeshIdNormalizer

@Composable
fun ActiveConnections(
    connectedDevices: List<NearbyDeviceUiState>,
    onChatClick: (NearbyDeviceUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (connectedDevices.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Active Mesh Links (${connectedDevices.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = MeshSpacing.SM)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.SM)
        ) {
            connectedDevices.forEach { deviceUi ->
                MeshGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = MeshSpacing.CardCornerRadius,
                    glowColor = MeshTheme.colors.connected,
                    glowRadius = 120f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeshSpacing.MD),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active link",
                                tint = MeshTheme.colors.connected,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(MeshSpacing.SM))
                            Column {
                                Text(
                                    text = deviceUi.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${deviceUi.transportUi.label} • ${MeshIdNormalizer.canonicalize(deviceUi.address)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onChatClick(deviceUi) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat with peer",
                                tint = MeshTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
