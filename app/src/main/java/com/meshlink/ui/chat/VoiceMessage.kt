package com.meshlink.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.Message

/**
 * Material 3 Voice Message Card composable.
 * Features play/pause button, audio waveform visualizer bars, duration display, and speed selector toggle.
 */
@Composable
fun VoiceMessage(
    message: Message,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var speedMultiplier by remember { mutableStateOf("1.0x") }

    val playButtonBg by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        label = "playBtnBg"
    )
    val playButtonIconTint by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        label = "playBtnTint"
    )

    val waveformHeights = remember {
        listOf(12, 20, 8, 28, 16, 24, 10, 30, 18, 14, 22, 12, 26, 16, 8, 20, 14, 24, 10, 18)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier.widthIn(min = 220.dp, max = 280.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Play / Pause Button
            Surface(
                shape = CircleShape,
                color = playButtonBg,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { if (isPlaying) onStopClick() else onPlayClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause voice message" else "Play voice message",
                        tint = playButtonIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Waveform bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(28.dp)
                ) {
                    waveformHeights.forEachIndexed { idx, barHeight ->
                        val barProgress = idx.toFloat() / waveformHeights.size
                        val isPlayed = playbackProgress >= barProgress
                        val barColor = if (isPlayed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlaying) "0:${(playbackProgress * 30).toInt().toString().padStart(2, '0')}" else "0:30",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Speed toggle pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.clickable {
                            speedMultiplier = when (speedMultiplier) {
                                "1.0x" -> "1.5x"
                                "1.5x" -> "2.0x"
                                else -> "1.0x"
                            }
                        }
                    ) {
                        Text(
                            text = speedMultiplier,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
