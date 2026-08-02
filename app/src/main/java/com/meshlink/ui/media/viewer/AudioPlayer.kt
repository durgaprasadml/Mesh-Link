package com.meshlink.ui.media.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.animation.MediaAnimations
import com.meshlink.ui.media.models.MediaUi

/**
 * Modern audio player UI component with dynamic waveform.
 */
@Composable
fun AudioPlayer(
    media: MediaUi,
    onClose: () -> Unit,
    onShare: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var progress by remember { mutableStateOf(0f) }
    val durationMs = media.durationMs ?: 180_000L
    val waveformHeights = MediaAnimations.rememberWaveformHeights(barCount = 32, isPlaying = isPlaying)

    val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Audio Player",
                    tint = MeshTheme.colors.onBackground
                )
            }

            Text(
                text = "Audio Player",
                style = MeshTheme.customTypography.title,
                color = MeshTheme.colors.onBackground
            )

            if (onShare != null) {
                IconButton(onClick = { onShare(media) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Audio",
                        tint = MeshTheme.colors.onBackground
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Center Card
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Audio Artwork Box
            MeshCard(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = media.title,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Sender
            Text(
                text = media.title,
                style = MeshTheme.customTypography.headline,
                color = MeshTheme.colors.onBackground,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Audio Track • ${media.senderName}",
                style = MeshTheme.customTypography.body,
                color = MeshTheme.colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dynamic Waveform Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                waveformHeights.forEachIndexed { index, heightFactor ->
                    val isPast = (index.toFloat() / waveformHeights.size) <= progress
                    val barColor = if (isPast) MeshTheme.colors.primary else MeshTheme.colors.surfaceVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightFactor)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seek Slider
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(
                    thumbColor = MeshTheme.colors.primary,
                    activeTrackColor = MeshTheme.colors.primary,
                    inactiveTrackColor = MeshTheme.colors.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration((durationMs * progress).toLong()),
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
                Text(
                    text = formatDuration(durationMs),
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Speed selector chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MeshTheme.colors.surfaceVariant,
                    modifier = Modifier.clickable {
                        val nextIndex = (speeds.indexOf(currentSpeed) + 1) % speeds.size
                        currentSpeed = speeds[nextIndex]
                    }
                ) {
                    Text(
                        text = "${currentSpeed}x",
                        style = MeshTheme.customTypography.subtitle,
                        color = MeshTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Play / Pause main trigger button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MeshTheme.colors.primary)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Favorite Button
                IconButton(onClick = { /* Favorite toggle */ }) {
                    Icon(
                        imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (media.isFavorite) MeshTheme.colors.error else MeshTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
