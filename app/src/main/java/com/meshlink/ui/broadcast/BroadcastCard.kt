package com.meshlink.ui.broadcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BroadcastCard(
    uiState: BroadcastMessageUiState,
    onSelectBroadcast: (BroadcastMessageUiState) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val message = uiState.message
    val isMe = uiState.isMe
    val priority = uiState.priority
    var showMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("HH:mm · MMM dd", Locale.getDefault()).format(Date(message.timestamp))
    }

    val senderName = remember(message.senderId, uiState.senderIdentity) {
        uiState.senderIdentity?.displayName?.ifBlank { null }
            ?: MeshIdNormalizer.canonicalize(message.senderId)
    }

    val cardBorderColor = if (priority.isEmergency) Color(priority.badgeColor)
    else if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val cardContainerColor = if (priority.isEmergency) Color(priority.containerColor).copy(alpha = 0.15f)
    else if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .scaleOnPress(0.98f)
            .border(
                width = if (priority.isEmergency) 1.5.dp else 1.dp,
                color = cardBorderColor,
                shape = MeshTheme.shapes.medium
            )
            .combinedClickable(
                onClick = { onSelectBroadcast(uiState) },
                onLongClick = { showMenu = true }
            )
            .semantics {
                contentDescription = "Broadcast from $senderName: ${message.text}"
            },
        shape = MeshTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.medium)
        ) {
            // Header Row: Avatar, Sender, Priority Badge, Options Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMe) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "ME",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    val identity = uiState.senderIdentity ?: UserIdentity.create(message.senderId, senderName)
                    UserAvatar(identity = identity, size = 32.dp)
                }

                Spacer(modifier = Modifier.width(MeshTheme.spacing.small))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMe) "You (Broadcast Sender)" else senderName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Priority Badge
                Surface(
                    shape = CircleShape,
                    color = Color(priority.containerColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(priority.badgeColor))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (priority.isEmergency) {
                            EmergencyBeaconPulse(size = 6.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = priority.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(priority.badgeColor)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                showMenu = false
                                onSelectBroadcast(uiState)
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Text") },
                            onClick = {
                                showMenu = false
                                onCopyText(message.text)
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

            // Body Text
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Location Payload Preview if present
            if (uiState.hasLocation) {
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Surface(
                    shape = MeshTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Payload",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GPS Coordinates Attached (${message.latitude}, ${message.longitude})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))

            // Footer Row: Delivery State Pill & Scope Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Broadcast ID: ${message.messageId.take(8)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                DeliveryProgress(status = message.status)
            }
        }
    }
}
