package com.meshlink.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Timer
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
 * RetryStatus — Visual retry engine state & exponential backoff monitor.
 */
@Composable
fun RetryStatusCard(
    retryUi: RetryUi,
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
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Retry Engine",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.retryPulseAnimation(retryUi.stateName != "IDLE")
                    )
                    Text(
                        text = "Retry & Backoff Manager",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.small,
                    color = when (retryUi.stateName) {
                        "BACKOFF" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        "IN_PROGRESS" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = retryUi.stateName,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (retryUi.stateName) {
                            "BACKOFF" -> Color(0xFFFF9800)
                            "IN_PROGRESS" -> Color(0xFF2196F3)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Retries",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${retryUi.retryCount}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Next Retry In",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (retryUi.nextRetryMs > 0) "${retryUi.nextRetryMs / 1000}s" else "Active / None",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last Attempt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (retryUi.lastAttemptMs > 0)
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(retryUi.lastAttemptMs))
                        else "N/A",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (retryUi.failureReason.isNotEmpty()) {
                Surface(
                    shape = MeshTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Last Failure: ${retryUi.failureReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * RetryStatus — Alias for RetryStatusCard for component naming consistency.
 */
@Composable
fun RetryStatus(
    retryUi: RetryUi,
    modifier: Modifier = Modifier
) {
    RetryStatusCard(retryUi = retryUi, modifier = modifier)
}

