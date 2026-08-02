package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.TransferDirectionUi
import com.meshlink.ui.media.models.TransferStatus
import com.meshlink.ui.media.models.TransferUi

/**
 * Active Transfer Queue component displaying live uploading, downloading, waiting, and paused file transfers.
 */
@Composable
fun TransferQueue(
    transfers: List<TransferUi>,
    onPauseClick: ((String) -> Unit)? = null,
    onResumeClick: ((String) -> Unit)? = null,
    onCancelClick: ((String) -> Unit)? = null,
    onRetryClick: ((String) -> Unit)? = null,
    onItemClick: ((TransferUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (transfers.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active transfers in queue",
                style = MeshTheme.customTypography.subtitle,
                color = MeshTheme.colors.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(
            items = transfers,
            key = { it.transferId }
        ) { transfer ->
            TransferQueueRow(
                transfer = transfer,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onCancelClick = onCancelClick,
                onRetryClick = onRetryClick,
                onClick = { onItemClick?.invoke(transfer) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TransferQueueRow(
    transfer: TransferUi,
    onPauseClick: ((String) -> Unit)?,
    onResumeClick: ((String) -> Unit)?,
    onCancelClick: ((String) -> Unit)?,
    onRetryClick: ((String) -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Direction Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (transfer.direction == TransferDirectionUi.OUTGOING)
                                MeshTheme.colors.primary.copy(alpha = 0.15f)
                            else
                                MeshTheme.colors.secondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transfer.direction == TransferDirectionUi.OUTGOING)
                            Icons.Default.Upload
                        else
                            Icons.Default.Download,
                        contentDescription = transfer.direction.name,
                        tint = if (transfer.direction == TransferDirectionUi.OUTGOING)
                            MeshTheme.colors.primary
                        else
                            MeshTheme.colors.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transfer.fileName,
                        style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = MeshTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${(transfer.progress * 100).toInt()}%",
                            style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                            color = MeshTheme.colors.primary
                        )
                        Text(
                            text = " • ${transfer.speedFormatted}",
                            style = MeshTheme.customTypography.caption,
                            color = MeshTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = " • ETA ${transfer.etaFormatted}",
                            style = MeshTheme.customTypography.caption,
                            color = MeshTheme.colors.onSurfaceVariant
                        )
                    }
                }

                // Action Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (transfer.status) {
                        TransferStatus.TRANSFERRING, TransferStatus.PREPARING -> {
                            IconButton(
                                onClick = { onPauseClick?.invoke(transfer.transferId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = MeshTheme.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        TransferStatus.PAUSED -> {
                            IconButton(
                                onClick = { onResumeClick?.invoke(transfer.transferId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = MeshTheme.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        TransferStatus.FAILED -> {
                            IconButton(
                                onClick = { onRetryClick?.invoke(transfer.transferId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = MeshTheme.colors.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        else -> {}
                    }

                    IconButton(
                        onClick = { onCancelClick?.invoke(transfer.transferId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MeshTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Progress Bar
            LinearProgressIndicator(
                progress = { transfer.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when (transfer.status) {
                    TransferStatus.FAILED -> MeshTheme.colors.error
                    TransferStatus.PAUSED -> MeshTheme.colors.onSurfaceVariant
                    else -> MeshTheme.colors.primary
                },
                trackColor = MeshTheme.colors.surfaceVariant
            )
        }
    }
}
