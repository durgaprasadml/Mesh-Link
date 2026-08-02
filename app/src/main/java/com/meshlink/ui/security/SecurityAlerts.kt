package com.meshlink.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun SecurityAlertsSection(
    alerts: List<SecurityAlertUi>,
    modifier: Modifier = Modifier,
    onAcknowledgeClick: ((SecurityAlertUi) -> Unit)? = null
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
            Text(
                text = "Security Alerts & Notifications (${alerts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (alerts.isEmpty()) {
            MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MeshTheme.spacing.mediumLarge),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "All security systems operational. No active alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)) {
                alerts.forEach { alert ->
                    SecurityAlertCard(
                        alert = alert,
                        onAcknowledgeClick = if (onAcknowledgeClick != null) { { onAcknowledgeClick(alert) } } else null
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityAlertCard(
    alert: SecurityAlertUi,
    modifier: Modifier = Modifier,
    onAcknowledgeClick: (() -> Unit)? = null
) {
    val (backgroundColor, icon, tint) = when (alert.severity) {
        SecurityAlertSeverity.CRITICAL -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Default.ErrorOutline,
            MaterialTheme.colorScheme.error
        )
        SecurityAlertSeverity.WARNING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            Icons.Default.Warning,
            MaterialTheme.colorScheme.tertiary
        )
        SecurityAlertSeverity.INFO -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            Icons.Default.Info,
            MaterialTheme.colorScheme.primary
        )
    }

    MeshGlassCard(
        modifier = modifier.fillMaxWidth(),
        fillColor = backgroundColor.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = alert.severity.name,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = alert.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!alert.isAcknowledged && onAcknowledgeClick != null) {
                IconButton(onClick = onAcknowledgeClick) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Dismiss Alert",
                        tint = tint
                    )
                }
            }
        }
    }
}
