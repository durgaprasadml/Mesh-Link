package com.meshlink.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * MeshRecovery — Self-healing partition recovery & route restoration visualizer.
 */
@Composable
fun MeshRecoveryCard(
    recoveryUi: MeshRecoveryUi,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Mesh Recovery",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.reconnectRippleAnimation(recoveryUi.isReconnecting)
                    )
                    Text(
                        text = "Mesh Self-Healing & Recovery",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.small,
                    color = if (recoveryUi.isMeshRestored) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (recoveryUi.isMeshRestored) "MESH RESTORED" else "RECOVERY ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (recoveryUi.isMeshRestored) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            RecoveryTransitionLayout(isRestoring = recoveryUi.isReconnecting) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecoveryStepItem(
                        title = "Reconnecting Interface",
                        status = if (recoveryUi.isReconnecting) "Scanning / Connecting..." else "Connected",
                        isComplete = true,
                        icon = Icons.AutoMirrored.Filled.BluetoothSearching
                    )

                    RecoveryStepItem(
                        title = "Peers Discovered",
                        status = "${recoveryUi.peersDiscoveredCount} peers in range",
                        isComplete = recoveryUi.peersDiscoveredCount > 0,
                        icon = Icons.Default.Group
                    )

                    RecoveryStepItem(
                        title = "Routes Rebuilt",
                        status = "${recoveryUi.routesRebuiltCount} dynamic routes compiled",
                        isComplete = recoveryUi.routesRebuiltCount > 0,
                        icon = Icons.AutoMirrored.Filled.AltRoute
                    )

                    RecoveryStepItem(
                        title = "Mesh Restored & Partition Healed",
                        status = recoveryUi.statusText,
                        isComplete = recoveryUi.isMeshRestored,
                        icon = Icons.Default.Verified
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Last event: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(recoveryUi.lastRecoveryMs))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecoveryStepItem(
    title: String,
    status: String,
    isComplete: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isComplete) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Complete",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * MeshRecovery — Alias for MeshRecoveryCard for component name consistency.
 */
@Composable
fun MeshRecovery(
    recoveryUi: MeshRecoveryUi,
    modifier: Modifier = Modifier
) {
    MeshRecoveryCard(recoveryUi = recoveryUi, modifier = modifier)
}

