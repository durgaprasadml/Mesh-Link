package com.meshlink.ui.broadcast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.UserAvatar
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress
import com.meshlink.util.MeshIdNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastDetailSheet(
    uiState: BroadcastMessageUiState,
    onCopyText: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = uiState.message
    val priority = uiState.priority

    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
    }

    val senderName = remember(message.senderId, uiState.senderIdentity) {
        uiState.senderIdentity?.displayName?.ifBlank { null }
            ?: MeshIdNormalizer.canonicalize(message.senderId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = MeshTheme.elevation.level3
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshTheme.spacing.medium)
                .padding(bottom = MeshTheme.spacing.giant)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Broadcast Details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                    Text(
                        text = "Broadcast Inspection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Sender Information Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MeshTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(MeshTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val identity = uiState.senderIdentity ?: UserIdentity.create(message.senderId, senderName)
                    UserAvatar(identity = identity, size = 44.dp)
                    Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                    Column {
                        Text(
                            text = if (uiState.isMe) "You (Local Device)" else senderName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Node ID: ${message.senderId}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Message Payload Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MeshTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.padding(MeshTheme.spacing.medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(priority.containerColor)
                        ) {
                            Text(
                                text = priority.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(priority.badgeColor),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        DeliveryProgress(status = message.status)
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Metadata Grid
            DetailRow(label = "Message ID", value = message.messageId)
            DetailRow(label = "Chat Target ID", value = message.chatId)
            DetailRow(label = "Timestamp", value = formattedTime)
            DetailRow(label = "Security Protocol", value = "AES-256 GCM Encrypted")
            if (uiState.hasLocation) {
                DetailRow(label = "GPS Coordinates", value = "${message.latitude}, ${message.longitude}")
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)
            ) {
                OutlinedButton(
                    onClick = { onCopyText(message.text) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .scaleOnPress(0.98f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Text")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
