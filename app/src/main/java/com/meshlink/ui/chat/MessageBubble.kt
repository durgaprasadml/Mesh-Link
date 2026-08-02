package com.meshlink.ui.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.ui.components.chat.LocationMessageCard
import com.meshlink.ui.components.chat.SosEmergencyCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium Tactical Message Bubble composable featuring adaptive corners, glass surfaces,
 * delivery indicators, voice waveforms, media attachments, and gesture selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    position: BubblePosition = BubblePosition.SINGLE,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    currentlyPlaying: String? = null,
    playbackProgress: Float = 0f,
    transferProgress: Float? = null,
    onToggleSelection: () -> Unit = {},
    onPlayVoice: (String) -> Unit = {},
    onStopPlayback: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onLocationClick: (Double, Double) -> Unit = { _, _ -> },
    onRetryMedia: (String) -> Unit = {},
    onSwipeToReply: (Message) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isMe = message.isFromMe

    // Align Outbound right, Inbound left
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart

    // Specialized renderers for SOS & Location cards
    if (message.messageType == MessageType.SOS) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = alignment
        ) {
            SosEmergencyCard(
                message = message,
                onLocationClick = onLocationClick
            )
        }
        return
    }

    if (message.messageType == MessageType.LOCATION) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = alignment
        ) {
            LocationMessageCard(
                message = message,
                onLocationClick = onLocationClick
            )
        }
        return
    }

    // Dynamic Adaptive Corner Shapes
    val cornerRadius = 18.dp
    val tightRadius = 4.dp
    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = cornerRadius,
            bottomStart = cornerRadius,
            topEnd = if (position == BubblePosition.FIRST || position == BubblePosition.SINGLE) cornerRadius else tightRadius,
            bottomEnd = if (position == BubblePosition.LAST || position == BubblePosition.SINGLE) cornerRadius else tightRadius
        )
    } else {
        RoundedCornerShape(
            topEnd = cornerRadius,
            bottomEnd = cornerRadius,
            topStart = if (position == BubblePosition.FIRST || position == BubblePosition.SINGLE) cornerRadius else tightRadius,
            bottomStart = if (position == BubblePosition.LAST || position == BubblePosition.SINGLE) cornerRadius else tightRadius
        )
    }

    // Color theme
    val baseBgColor = if (isMe) {
        MeshTheme.colors.primary.copy(alpha = 0.20f)
    } else {
        MeshTheme.colors.surfaceVariant.copy(alpha = 0.85f)
    }
    val selectedBgColor = MeshTheme.colors.secondary.copy(alpha = 0.35f)

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) selectedBgColor else baseBgColor,
        label = "bubbleBgColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode && !isMe) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(checkedColor = MeshTheme.colors.primary)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Surface(
            shape = bubbleShape,
            color = animatedBgColor,
            tonalElevation = if (isMe) 2.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .border(
                    width = 1.dp,
                    color = if (isMe) MeshTheme.colors.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = bubbleShape
                )
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelection()
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSelection()
                    }
                )
                .animateContentSize()
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 6.dp
                )
            ) {
                // Relayed indicator badge
                if (message.status == DeliveryStatus.RELAYED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFB703).copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "RELAYED VIA MESH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFFFB703)
                            )
                        }
                    }
                }

                // Message Body by Type
                when (message.messageType) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    MessageType.IMAGE -> {
                        ImageAttachmentView(
                            message = message,
                            transferProgress = transferProgress,
                            onImageClick = onImageClick,
                            onRetryMedia = onRetryMedia
                        )
                    }

                    MessageType.VOICE -> {
                        VoiceAttachmentView(
                            message = message,
                            currentlyPlaying = currentlyPlaying,
                            playbackProgress = playbackProgress,
                            onPlayVoice = onPlayVoice,
                            onStopPlayback = onStopPlayback
                        )
                    }

                    else -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp & Delivery status footer
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    val formattedTime = remember(message.timestamp) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        DeliveryStatusTicks(status = message.status)
                    }
                }
            }
        }

        if (isSelectionMode && isMe) {
            Spacer(modifier = Modifier.width(4.dp))
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(checkedColor = MeshTheme.colors.primary)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageAttachmentView(
    message: Message,
    transferProgress: Float?,
    onImageClick: (String) -> Unit,
    onRetryMedia: (String) -> Unit
) {
    val imageFile = remember(message.mediaPath) {
        message.mediaPath?.let { File(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MeshTheme.colors.surfaceVariant)
            .combinedClickable(onClick = { onImageClick(message.messageId) }),
        contentAlignment = Alignment.Center
    ) {
        if (imageFile != null && imageFile.exists()) {
            AsyncImage(
                model = imageFile,
                contentDescription = "Image attachment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (!message.thumbnailBase64.isNullOrBlank()) {
            val bitmap = remember(message.thumbnailBase64) {
                try {
                    val bytes = Base64.decode(message.thumbnailBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Thumbnail preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (transferProgress != null && transferProgress < 1.0f) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { transferProgress },
                    color = MeshTheme.colors.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (message.status == DeliveryStatus.FAILED || message.status == DeliveryStatus.PERMANENT_FAILURE) {
            IconButton(
                onClick = { onRetryMedia(message.messageId) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry download",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun VoiceAttachmentView(
    message: Message,
    currentlyPlaying: String?,
    playbackProgress: Float,
    onPlayVoice: (String) -> Unit,
    onStopPlayback: () -> Unit
) {
    val isPlaying = currentlyPlaying == message.messageId

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MeshTheme.colors.primary)
                .clickable {
                    if (isPlaying) onStopPlayback() else onPlayVoice(message.messageId)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop voice note" else "Play voice note",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { if (isPlaying) playbackProgress else 0f },
                color = MeshTheme.colors.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            val durationMs = message.mediaDurationMs ?: 0L
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / 1000) / 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeliveryStatusTicks(status: DeliveryStatus) {
    val (icon, tint) = when (status) {
        DeliveryStatus.PENDING, DeliveryStatus.QUEUED -> Pair(
            Icons.Default.HourglassEmpty,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        DeliveryStatus.SENDING, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ROUTE -> Pair(
            Icons.Default.Check,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        DeliveryStatus.SENT, DeliveryStatus.WAITING_FOR_ACK -> Pair(
            Icons.Default.Check,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
        )
        DeliveryStatus.DELIVERED -> Pair(
            Icons.Default.DoneAll,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
        )
        DeliveryStatus.SEEN -> Pair(
            Icons.Default.DoneAll,
            MeshTheme.colors.primary // Neon blue/green read checkmark
        )
        DeliveryStatus.RELAYED -> Pair(
            Icons.Default.CheckCircle,
            Color(0xFFFFB703) // Amber mesh relay checkmark
        )
        DeliveryStatus.FAILED, DeliveryStatus.PERMANENT_FAILURE, DeliveryStatus.EXPIRED, DeliveryStatus.CANCELLED -> Pair(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = tint,
        modifier = Modifier.size(14.dp)
    )
}
