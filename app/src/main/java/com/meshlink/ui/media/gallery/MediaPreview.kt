package com.meshlink.ui.media.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.meshlink.ui.designsystem.components.buttons.MeshButton
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaUi

/**
 * Pre-send attachment preview modal sheet with caption input and compression options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewSheet(
    items: List<MediaUi>,
    onRemoveItem: (MediaUi) -> Unit,
    onSend: (caption: String, isCompressed: Boolean) -> Unit,
    onDismiss: () -> Unit,
    recipientName: String = "Mesh Network",
    modifier: Modifier = Modifier
) {
    var caption by remember { mutableStateOf("") }
    var isCompressed by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MeshTheme.colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Send Attachments (${items.size})",
                        style = MeshTheme.customTypography.title,
                        color = MeshTheme.colors.onSurface
                    )
                    Text(
                        text = "To: $recipientName",
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MeshTheme.colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Preview List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { media ->
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = media.thumbnailBase64 ?: media.uriOrPath,
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Remove Button Badge
                        IconButton(
                            onClick = { onRemoveItem(media) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compression Option Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mesh Intelligent Compression",
                        style = MeshTheme.customTypography.subtitle,
                        color = MeshTheme.colors.onSurface
                    )
                    Text(
                        text = if (isCompressed) "Optimized for fast offline hop delivery" else "Send original uncompressed file",
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isCompressed,
                    onCheckedChange = { isCompressed = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = MeshTheme.colors.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caption Text Input
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Add caption or instructions...", style = MeshTheme.customTypography.body) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MeshTheme.colors.primary,
                    unfocusedBorderColor = MeshTheme.colors.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Send Action Button
            MeshButton(
                text = "Send via Mesh",
                onClick = { onSend(caption, isCompressed) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.AutoMirrored.Filled.Send
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
