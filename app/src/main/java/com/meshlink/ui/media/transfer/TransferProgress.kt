package com.meshlink.ui.media.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.animation.MediaAnimations
import com.meshlink.ui.media.models.TransferDirectionUi
import com.meshlink.ui.media.models.TransferStatus
import com.meshlink.ui.media.models.TransferUi

/**
 * Live transfer progress card displaying real-time mesh transfer metrics.
 */
@Composable
fun TransferProgressCard(
    transfer: TransferUi,
    onPauseClick: ((String) -> Unit)? = null,
    onResumeClick: ((String) -> Unit)? = null,
    onCancelClick: ((String) -> Unit)? = null,
    onRetryClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pulseAlpha = MediaAnimations.rememberPulseAlpha()
    val isUploading = transfer.direction == TransferDirectionUi.OUTGOING

    MeshCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Direction Arrow Indicator Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUploading) MeshTheme.colors.primary.copy(alpha = 0.2f)
                                else MeshTheme.colors.secondary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUploading) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isUploading) MeshTheme.colors.primary else MeshTheme.colors.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = transfer.fileName,
                            style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                            color = MeshTheme.colors.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "${if (isUploading) "Uploading" else "Downloading"} via ${transfer.transportType}",
                            style = MeshTheme.customTypography.caption,
                            color = MeshTheme.colors.onSurfaceVariant
                        )
                    }
                }

                // Transport / Priority Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MeshTheme.colors.surfaceVariant
                ) {
                    Text(
                        text = transfer.transportType,
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                        color = MeshTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Progress Bar
            LinearProgressIndicator(
                progress = { transfer.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MeshTheme.colors.primary,
                trackColor = MeshTheme.colors.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Speed & Telemetry Readouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${(transfer.progress * 100).toInt()}%",
                        style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.Bold),
                        color = MeshTheme.colors.primary
                    )
                    Text(
                        text = "•",
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                    Text(
                        text = transfer.speedFormatted,
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Medium),
                        color = MeshTheme.colors.onSurface
                    )
                }

                Text(
                    text = "ETA: ${transfer.etaFormatted}",
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }

            // Action Buttons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (transfer.status == TransferStatus.TRANSFERRING) {
                    IconButton(
                        onClick = { onPauseClick?.invoke(transfer.transferId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = MeshTheme.colors.onSurfaceVariant
                        )
                    }
                } else if (transfer.status == TransferStatus.PAUSED) {
                    IconButton(
                        onClick = { onResumeClick?.invoke(transfer.transferId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = MeshTheme.colors.primary
                        )
                    }
                }

                if (transfer.status == TransferStatus.FAILED && onRetryClick != null) {
                    IconButton(
                        onClick = { onRetryClick.invoke(transfer.transferId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MeshTheme.colors.primary
                        )
                    }
                }

                if (onCancelClick != null && transfer.status != TransferStatus.COMPLETED) {
                    IconButton(
                        onClick = { onCancelClick.invoke(transfer.transferId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Transfer",
                            tint = MeshTheme.colors.error
                        )
                    }
                }
            }
        }
    }
}
