package com.meshlink.ui.media.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaUi

/**
 * Modern video player UI component.
 */
@Composable
fun VideoPlayer(
    media: MediaUi,
    onClose: () -> Unit,
    onShare: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var progress by remember { mutableStateOf(0f) }
    val durationMs = media.durationMs ?: 60_000L

    val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Surface / Thumbnail Fallback
        AsyncImage(
            model = media.thumbnailBase64 ?: media.uriOrPath,
            contentDescription = media.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clickable { isPlaying = !isPlaying }
        )

        // Center Play / Pause Pulsing Overlay Trigger
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MeshTheme.colors.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        // Top App Bar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Player",
                    tint = Color.White
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = media.title,
                    style = MeshTheme.customTypography.subtitle,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "Video • ${media.senderName}",
                    style = MeshTheme.customTypography.caption,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            if (onShare != null) {
                IconButton(onClick = { onShare(media) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Video",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Controls Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            // Seek Bar Slider
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(
                    thumbColor = MeshTheme.colors.primary,
                    activeTrackColor = MeshTheme.colors.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Elapsed & Total Time Readout
                Text(
                    text = "${formatDuration((durationMs * progress).toLong())} / ${formatDuration(durationMs)}",
                    style = MeshTheme.customTypography.caption,
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mute / Unmute Toggle
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Speed Selector Dropdown Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clickable {
                            val nextIndex = (speeds.indexOf(currentSpeed) + 1) % speeds.size
                            currentSpeed = speeds[nextIndex]
                        }
                    ) {
                        Text(
                            text = "${currentSpeed}x",
                            style = MeshTheme.customTypography.caption,
                            color = MeshTheme.colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Subtitles Placeholder
                    IconButton(
                        onClick = { /* Subtitle action */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Subtitles",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
