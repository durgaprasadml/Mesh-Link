package com.meshlink.ui.media.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaUi

/**
 * Fullscreen pinch-to-zoom interactive image viewer.
 */
@Composable
fun ImageViewer(
    media: MediaUi,
    onClose: () -> Unit,
    onShare: ((MediaUi) -> Unit)? = null,
    onSave: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        // Zoomable / Pannable Image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            val maxOffsetX = (size.width * (scale - 1)) / 2
                            val maxOffsetY = (size.height * (scale - 1)) / 2
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.5f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = media.uriOrPath,
                contentDescription = media.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }

        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Viewer",
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
                    text = "Sent by ${media.senderName}",
                    style = MeshTheme.customTypography.caption,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Row {
                IconButton(onClick = { showDetailsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Media Details",
                        tint = Color.White
                    )
                }

                if (onShare != null) {
                    IconButton(onClick = { onShare(media) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Image",
                            tint = Color.White
                        )
                    }
                }

                if (onSave != null) {
                    IconButton(onClick = { onSave(media) }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Save Image",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Zoom Reset Floating Bar
        if (scale > 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                TextButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    }
                ) {
                    Text(
                        text = "Reset Zoom (${String.format("%.1fx", scale)})",
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.primary
                    )
                }
            }
        }
    }

    // Media Details Dialog
    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Image Details", style = MeshTheme.customTypography.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title: ${media.title}")
                    Text("MIME Type: ${media.mimeType}")
                    Text("Size: ${media.sizeBytes / 1024} KB")
                    if (media.width != null && media.height != null) {
                        Text("Dimensions: ${media.width} x ${media.height}")
                    }
                    Text("Sender: ${media.senderName}")
                    Text("Location/Path: ${media.uriOrPath}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("OK", color = MeshTheme.colors.primary)
                }
            }
        )
    }
}
