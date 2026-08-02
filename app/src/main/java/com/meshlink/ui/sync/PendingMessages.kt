package com.meshlink.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * PendingMessages — High-performance list displaying queued offline messages with retry countdowns and details.
 */
@Composable
fun PendingMessagesList(
    pendingMessages: List<PendingMessageUi>,
    onCancelMessageClick: ((PendingMessageUi) -> Unit)? = null,
    onForceRetryClick: ((PendingMessageUi) -> Unit)? = null,
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
                        imageVector = Icons.Default.PendingActions,
                        contentDescription = "Pending Messages",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pending Queued Messages",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${pendingMessages.size} Queued",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (pendingMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "No Pending Messages",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No pending messages in queue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = pendingMessages,
                        key = { it.id }
                    ) { msg ->
                        PendingMessageItem(
                            message = msg,
                            onCancelClick = if (onCancelMessageClick != null) { { onCancelMessageClick(msg) } } else null,
                            onRetryClick = if (onForceRetryClick != null) { { onForceRetryClick(msg) } } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingMessageItem(
    message: PendingMessageUi,
    onCancelClick: (() -> Unit)?,
    onRetryClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(MeshTheme.shapes.tiny)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (message.hasAttachment) Icons.Default.Attachment else Icons.AutoMirrored.Filled.Message,
                            contentDescription = "Attachment",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "To: ${message.recipientName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.tiny,
                    color = when (message.status) {
                        "RETRYING" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                        "FAILED" -> Color(0xFFF44336).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = message.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (message.status) {
                            "RETRYING" -> Color(0xFFFF9800)
                            "FAILED" -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = message.previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attempts: ${message.attemptCount} • Dest: ${message.recipientId.take(8)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (onCancelClick != null || onRetryClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRetryClick != null && message.status == "RETRYING") {
                        TextButton(
                            onClick = onRetryClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Retry Now", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (onCancelClick != null) {
                        TextButton(
                            onClick = onCancelClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * PendingMessages — Alias for PendingMessagesList for component naming consistency.
 */
@Composable
fun PendingMessages(
    pendingMessages: List<PendingMessageUi>,
    onCancelMessageClick: ((PendingMessageUi) -> Unit)? = null,
    onForceRetryClick: ((PendingMessageUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    PendingMessagesList(
        pendingMessages = pendingMessages,
        onCancelMessageClick = onCancelMessageClick,
        onForceRetryClick = onForceRetryClick,
        modifier = modifier
    )
}

