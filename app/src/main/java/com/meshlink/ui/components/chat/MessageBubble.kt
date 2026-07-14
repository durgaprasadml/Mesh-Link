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
import coil.compose.AsyncImage
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.io.File
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

    val baseBubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else baseBubbleColor,
        label = "bubbleColor"
    )

    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
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
            append("Status: ${message.status.name.lowercase()}. ")
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
                onLongClick = { onToggleSelection() },
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 300.dp, min = 80.dp)
                .animateContentSize()
        ) {
            when (message.messageType) {
                MessageType.IMAGE -> {
                    val mediaPath = message.mediaPath
                    if (mediaPath != null && File(mediaPath).exists()) {
                        AsyncImage(
                            model = File(mediaPath),
                            contentDescription = "View full image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(mediaPath) },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (message.isFromMe) "Sending image..." else "Receiving image...", style = MaterialTheme.typography.labelSmall)
                                if (message.status == DeliveryStatus.FAILED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IconButton(
                                        onClick = { onRetryMedia(message.messageId) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.error, CircleShape).size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Retry image transfer", tint = Color.White)
                                    }
                                } else if (transferProgress != null && transferProgress >= 0f) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { transferProgress },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                MessageType.VOICE -> {
                    val mediaPath = message.mediaPath
                    val fileExists = mediaPath != null && File(mediaPath).exists()
                    val isThisPlaying = currentlyPlaying == mediaPath
                    val durationText = message.mediaDurationMs?.let { "${it / 1000}s" } ?: ""

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (fileExists && mediaPath != null) {
                                    if (isThisPlaying) onStopPlayback() else onPlayVoice(mediaPath)
                                }
                            },
                            enabled = fileExists && !isSelectionMode,
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isThisPlaying) "Stop voice note" else "Play voice note",
                                tint = if (fileExists) textColor else textColor.copy(alpha = 0.3f)
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            if (message.status == DeliveryStatus.FAILED) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onRetryMedia(message.messageId) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Failed. Tap to retry.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            } else if (transferProgress != null && transferProgress >= 0f && message.status == DeliveryStatus.PENDING) {
                                Text(
                                    text = if (message.isFromMe) "Sending..." else "Receiving...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { transferProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = textColor.copy(alpha = 0.3f)
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { if (isThisPlaying) playbackProgress else 0f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = textColor.copy(alpha = 0.8f),
                                    trackColor = textColor.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
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
                    Column(
                        modifier = Modifier
                            .clickable(enabled = !isSelectionMode) {
                                val lat = message.latitude
                                val lng = message.longitude
                                if (lat != null && lng != null) {
                                    onLocationClick(lat, lng)
                                }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Location Shared", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (message.latitude != null && message.longitude != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha=0.3f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("Lat: ${String.format(Locale.US, "%.6f", message.latitude)}", color = textColor, style = MaterialTheme.typography.bodySmall)
                                    Text("Lng: ${String.format(Locale.US, "%.6f", message.longitude)}", color = textColor, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (message.batteryPercent != null && message.batteryPercent >= 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("🔋 ${message.batteryPercent}% Battery", color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                MessageType.SOS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .padding(12.dp)
                    ) {
                        Text("🚨 SOS EMERGENCY", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (message.latitude != null && message.longitude != null) {
                            Text("📍 Lat: ${String.format(Locale.US, "%.6f", message.latitude)}", color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.bodySmall)
                            Text("📍 Lng: ${String.format(Locale.US, "%.6f", message.longitude)}", color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.bodySmall)
                        }
                        if (message.batteryPercent != null && message.batteryPercent >= 0) {
                            Text("🔋 Battery: ${message.batteryPercent}%", color = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                MessageType.TEXT -> {
                    Text(
                        text = message.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // Timestamp + status row
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val statusIcon = when (message.status) {
                        DeliveryStatus.PENDING -> Icons.Default.AccessTime
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
                        modifier = Modifier.size(14.dp),
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
