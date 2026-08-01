package com.meshlink.ui.designsystem.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Global Notification Framework for Mesh-Link 2026.
 * Contains Status Banner, Connection Banner, Emergency Banner,
 * Upload Indicator, Download Indicator, and Sync Indicator.
 */

@Composable
fun MeshStatusBanner(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    containerColor: Color = MeshTheme.colors.surface,
    contentColor: Color = MeshTheme.colors.textPrimary
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .border(width = 0.5.dp, color = MeshTheme.colors.border),
            color = containerColor,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MeshConnectionBanner(
    isConnected: Boolean,
    nodeCount: Int,
    modifier: Modifier = Modifier
) {
    MeshStatusBanner(
        visible = true,
        message = if (isConnected) "Mesh Connected • $nodeCount active peer nodes" else "Mesh Searching • Scanning BLE & Wi-Fi Direct",
        icon = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
        containerColor = if (isConnected) MeshTheme.colors.primary.copy(alpha = 0.12f) else MeshTheme.colors.warning.copy(alpha = 0.12f),
        contentColor = if (isConnected) MeshTheme.colors.primary else MeshTheme.colors.warning,
        modifier = modifier
    )
}

@Composable
fun MeshEmergencyBanner(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier
) {
    MeshStatusBanner(
        visible = visible,
        message = message,
        icon = Icons.Default.Warning,
        containerColor = MeshTheme.colors.emergency.copy(alpha = 0.2f),
        contentColor = MeshTheme.colors.emergency,
        modifier = modifier
    )
}

/**
 * Upload Indicator for file or payload transfer progress.
 */
@Composable
fun MeshUploadIndicator(
    isUploading: Boolean,
    fileName: String,
    progress: Float,
    modifier: Modifier = Modifier,
    uploadSpeed: String? = null,
    onCancel: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isUploading,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(MeshTheme.shapes.medium)
                .border(width = 0.5.dp, color = MeshTheme.colors.glassBorder),
            color = MeshTheme.colors.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MeshTheme.colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = MeshTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Uploading: $fileName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MeshTheme.colors.textPrimary
                            )
                            if (uploadSpeed != null) {
                                Text(
                                    text = uploadSpeed,
                                    fontSize = 11.sp,
                                    color = MeshTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeshTheme.colors.primary
                        )

                        if (onCancel != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MeshTheme.colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MeshTheme.shapes.pill),
                    color = MeshTheme.colors.primary,
                    trackColor = MeshTheme.colors.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Download Indicator for incoming peer transfer progress.
 */
@Composable
fun MeshDownloadIndicator(
    isDownloading: Boolean,
    fileName: String,
    progress: Float,
    modifier: Modifier = Modifier,
    downloadSpeed: String? = null,
    onCancel: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isDownloading,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(MeshTheme.shapes.medium)
                .border(width = 0.5.dp, color = MeshTheme.colors.glassBorder),
            color = MeshTheme.colors.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MeshTheme.colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MeshTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Downloading: $fileName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MeshTheme.colors.textPrimary
                            )
                            if (downloadSpeed != null) {
                                Text(
                                    text = downloadSpeed,
                                    fontSize = 11.sp,
                                    color = MeshTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeshTheme.colors.primary
                        )

                        if (onCancel != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MeshTheme.colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MeshTheme.shapes.pill),
                    color = MeshTheme.colors.primary,
                    trackColor = MeshTheme.colors.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun MeshSyncIndicator(
    isSyncing: Boolean,
    syncLabel: String = "Syncing mesh database...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSyncing,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = modifier
                .clip(MeshTheme.shapes.pill)
                .background(MeshTheme.colors.surface)
                .border(width = 0.5.dp, color = MeshTheme.colors.border, shape = MeshTheme.shapes.pill)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = MeshTheme.colors.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = syncLabel,
                color = MeshTheme.colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}
