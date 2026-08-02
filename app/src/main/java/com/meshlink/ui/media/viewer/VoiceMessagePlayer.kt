package com.meshlink.ui.media.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.animation.MediaAnimations
import com.meshlink.ui.media.models.MediaUi

/**
 * Premium voice message notes player card.
 */
@Composable
fun VoiceMessagePlayer(
    media: MediaUi,
    onPlayToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var progress by remember { mutableStateOf(0.3f) }
    val durationMs = media.durationMs ?: 15_000L
    val waveformHeights = MediaAnimations.rememberWaveformHeights(barCount = 20, isPlaying = isPlaying)

    val speeds = listOf(1.0f, 1.5f, 2.0f)

    MeshCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Circle Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MeshTheme.colors.primary)
                    .clickable {
                        isPlaying = !isPlaying
                        onPlayToggle?.invoke(isPlaying)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Voice Note" else "Play Voice Note",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Waveform & Info Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Waveform Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    waveformHeights.forEachIndexed { index, heightFactor ->
                        val isPast = (index.toFloat() / waveformHeights.size) <= progress
                        val barColor = if (isPast) MeshTheme.colors.primary else MeshTheme.colors.surfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(heightFactor)
                                .clip(RoundedCornerShape(1.dp))
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Note • ${media.senderName}",
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Medium),
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration((durationMs * progress).toLong()),
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Speed Selector Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MeshTheme.colors.surfaceVariant,
                modifier = Modifier.clickable {
                    val nextIndex = (speeds.indexOf(currentSpeed) + 1) % speeds.size
                    currentSpeed = speeds[nextIndex]
                }
            ) {
                Text(
                    text = "${currentSpeed}x",
                    style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = MeshTheme.colors.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
