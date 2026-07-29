package com.meshlink.ui.components.chat

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil.compose.AsyncImage
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.io.File
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    currentlyPlaying: String?,
    playbackProgress: Float,
    transferProgress: Float?,
    onToggleSelection: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onImageClick: (String) -> Unit,
    onLocationClick: (Double, Double) -> Unit,
    onRetryMedia: (String) -> Unit
) {
    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val haptic = LocalHapticFeedback.current

    val isSos = message.messageType == MessageType.SOS
    val isLocation = message.messageType == MessageType.LOCATION
    val baseBubbleColor = when {
        isSos || isLocation -> Color.Transparent
        isMe -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else baseBubbleColor,
        label = "bubbleColor"
    )

    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isSos || isLocation) {
        RoundedCornerShape(20.dp)
    } else if (isMe) {
        RoundedCornerShape(topStart = MeshTheme.spacing.large, topEnd = MeshTheme.spacing.small, bottomStart = MeshTheme.spacing.large, bottomEnd = MeshTheme.spacing.large)
    } else {
        RoundedCornerShape(topStart = MeshTheme.spacing.small, topEnd = MeshTheme.spacing.large, bottomStart = MeshTheme.spacing.large, bottomEnd = MeshTheme.spacing.large)
    }

    val semanticDescription = buildString {
        append(if (isMe) "Sent message. " else "Received message. ")
        when (message.messageType) {
            MessageType.TEXT -> append(message.text)
            MessageType.IMAGE -> append("Photo.")
            MessageType.VOICE -> append("Voice note.")
            MessageType.LOCATION -> append("Location shared.")
            MessageType.SOS -> append("SOS Emergency alert.")

        }
        append(" at ${formatTime(message.timestamp)}. ")
        if (isMe) {
            val statusStr = when (message.status) {
                DeliveryStatus.QUEUED -> "sending"
                DeliveryStatus.SENT -> "sent"
                DeliveryStatus.DELIVERED, DeliveryStatus.RELAYED, DeliveryStatus.SEEN -> "delivered"
                DeliveryStatus.FAILED -> "failed"
            }
            append("Status: $statusStr. ")
        }
        if (isSelected) {
            append("Selected. ")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() },
                onLongClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleSelection() 
                },
                role = Role.Button
            )
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
                role = Role.Button
            },
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .then(
                    if (!isSos && !isLocation) {
                        Modifier.padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.mediumSmall)
                    } else Modifier
                )
                .widthIn(max = if (isSos || isLocation) 340.dp else 300.dp, min = if (isSos || isLocation) 280.dp else 80.dp)
                .animateContentSize()
        ) {
            when (message.messageType) {
                MessageType.IMAGE -> {
                    val mediaPath = message.mediaPath
                    val isComplete = message.status != DeliveryStatus.QUEUED && message.status != DeliveryStatus.FAILED
                    val hasFullFile = mediaPath != null && File(mediaPath).exists()
                    
                    if (isComplete && hasFullFile) {
                        AsyncImage(
                            model = File(mediaPath!!),
                            contentDescription = "View full image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 260.dp)
                                .clip(RoundedCornerShape(MeshTheme.spacing.medium))
                                .clickable { onImageClick(message.messageId) },
                            contentScale = ContentScale.Crop
                        )
                    } else if (!message.thumbnailBase64.isNullOrEmpty()) {
                        val imageBitmapState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                        androidx.compose.runtime.LaunchedEffect(message.thumbnailBase64) {
                            if (!message.thumbnailBase64.isNullOrEmpty()) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val bytes = Base64.decode(message.thumbnailBase64, Base64.DEFAULT)
                                        imageBitmapState.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        }
                        val imageBitmap = imageBitmapState.value
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(MeshTheme.spacing.medium))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "Image thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.5f
                                )
                            }
                            
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                                    if (message.status == DeliveryStatus.FAILED) {
                                        IconButton(
                                            onClick = { onRetryMedia(message.messageId) },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.error, CircleShape).size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry image transfer", tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))
                                        Text("Failed. Tap to retry.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    } else if (transferProgress != null && transferProgress >= 0f) {
                                        CircularProgressIndicator(
                                            progress = { transferProgress },
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(MeshTheme.spacing.medium))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (message.status == DeliveryStatus.FAILED) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { onRetryMedia(message.messageId) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.error, CircleShape).size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Retry image transfer", tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))
                                    Text("Failed. Tap to retry.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
                MessageType.VOICE -> {
                    val fileExists = remember(message.mediaPath) {
                        message.mediaPath != null && File(message.mediaPath).exists()
                    }
                    val isThisPlaying = currentlyPlaying == message.messageId
                    val durationMs = message.mediaDurationMs ?: 0L
                    val durationText = remember(durationMs) {
                        val seconds = (durationMs / 1000) % 60
                        val minutes = (durationMs / (1000 * 60)) % 60
                        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(MeshTheme.spacing.small)
                    ) {
                        IconButton(
                            onClick = {
                                if (isThisPlaying) {
                                    onStopPlayback()
                                } else if (fileExists && message.mediaPath != null) {
                                    onPlayVoice(message.mediaPath)
                                }
                            },
                            enabled = fileExists && !isSelectionMode,
                            modifier = Modifier
                                .background(textColor.copy(alpha = 0.15f), CircleShape)
                                .size(MeshTheme.spacing.huge)
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isThisPlaying) "Stop voice message" else "Play voice message",
                                tint = textColor
                            )
                        }

                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))

                        Column(modifier = Modifier.weight(1f)) {
                            if (message.status == DeliveryStatus.FAILED) {
                                Text(
                                    text = "Failed to download voice note",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Tap to retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.clickable { onRetryMedia(message.messageId) }
                                )
                            } else if (transferProgress != null && transferProgress < 1.0f) {
                                LinearProgressIndicator(
                                    progress = { transferProgress },
                                    modifier = Modifier.fillMaxWidth().height(MeshTheme.spacing.small).clip(RoundedCornerShape(MeshTheme.spacing.extraSmall)),
                                    color = textColor,
                                    trackColor = textColor.copy(alpha = 0.3f)
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { if (isThisPlaying) playbackProgress else 0f },
                                    modifier = Modifier.fillMaxWidth().height(MeshTheme.spacing.small).clip(RoundedCornerShape(MeshTheme.spacing.extraSmall)),
                                    color = textColor.copy(alpha = 0.8f),
                                    trackColor = textColor.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                                Text(
                                    text = if (fileExists) durationText else "File missing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                MessageType.LOCATION -> {
                    LocationMessageCard(
                        message = message,
                        onLocationClick = onLocationClick
                    )
                }
                MessageType.SOS -> {
                    SosEmergencyCard(
                        message = message,
                        onLocationClick = onLocationClick
                    )
                }

                MessageType.TEXT -> {
                    Column {
                        Text(
                            text = message.text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = MeshTheme.spacing.extraSmall)
                        )
                        if (message.status == DeliveryStatus.FAILED) {
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onRetryMedia(message.messageId) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry message", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(MeshTheme.spacing.mediumLarge))
                                Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                                Text("Failed. Tap to retry.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (!isSos) {
                val formattedTime = androidx.compose.runtime.remember(message.timestamp) {
                    formatTime(message.timestamp)
                }

                // Timestamp + status row
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = MeshTheme.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                        val statusIcon = when (message.status) {
                            DeliveryStatus.QUEUED -> Icons.Default.AccessTime
                            DeliveryStatus.SENT -> Icons.Default.Check
                            DeliveryStatus.RELAYED -> Icons.Default.DoneAll
                            DeliveryStatus.DELIVERED -> Icons.Default.DoneAll
                            DeliveryStatus.SEEN -> Icons.Default.DoneAll
                            DeliveryStatus.FAILED -> Icons.Default.ErrorOutline
                        }
                        val iconTint = when (message.status) {
                            DeliveryStatus.SEEN -> MaterialTheme.colorScheme.primary
                            DeliveryStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> textColor.copy(alpha = 0.7f)
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null, // Handled by outer semantics
                            modifier = Modifier.size(MeshTheme.spacing.medium),
                            tint = iconTint
                        )
                    }
                }
            } else if (isMe) {
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = MeshTheme.spacing.extraSmall, end = MeshTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusIcon = when (message.status) {
                        DeliveryStatus.QUEUED -> Icons.Default.AccessTime
                        DeliveryStatus.SENT -> Icons.Default.Check
                        DeliveryStatus.RELAYED -> Icons.Default.DoneAll
                        DeliveryStatus.DELIVERED -> Icons.Default.DoneAll
                        DeliveryStatus.SEEN -> Icons.Default.DoneAll
                        DeliveryStatus.FAILED -> Icons.Default.ErrorOutline
                    }
                    val iconTint = when (message.status) {
                        DeliveryStatus.SEEN -> MaterialTheme.colorScheme.primary
                        DeliveryStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.error
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Status: ${message.status}",
                        modifier = Modifier.size(MeshTheme.spacing.medium),
                        tint = iconTint
                    )
                }
            }
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}
