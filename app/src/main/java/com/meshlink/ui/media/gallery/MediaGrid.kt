package com.meshlink.ui.media.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaType
import com.meshlink.ui.media.models.MediaUi

/**
 * Responsive LazyVerticalGrid with stable item keys and type badges.
 */
@Composable
fun MediaGrid(
    items: List<MediaUi>,
    onItemClick: (MediaUi) -> Unit,
    selectedIds: Set<String> = emptySet(),
    onItemToggleSelect: ((MediaUi) -> Unit)? = null,
    isSelectionMode: Boolean = false,
    columnsCount: Int = 3,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(
            items = items,
            key = { item -> item.id }
        ) { item ->
            MediaGridThumbnailCard(
                media = item,
                isSelected = selectedIds.contains(item.id),
                isSelectionMode = isSelectionMode,
                onClick = {
                    if (isSelectionMode && onItemToggleSelect != null) {
                        onItemToggleSelect(item)
                    } else {
                        onItemClick(item)
                    }
                }
            )
        }
    }
}

@Composable
fun MediaGridThumbnailCard(
    media: MediaUi,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshCard(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (media.mediaType) {
                MediaType.IMAGE, MediaType.VIDEO -> {
                    AsyncImage(
                        model = media.thumbnailBase64 ?: media.uriOrPath,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MediaType.AUDIO, MediaType.VOICE_NOTE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MeshTheme.colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (media.mediaType == MediaType.VOICE_NOTE) Icons.Default.Mic else Icons.Default.MusicNote,
                            contentDescription = media.title,
                            tint = MeshTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MeshTheme.colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = media.title,
                            tint = MeshTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Top-Right Checkbox Overlay (Selection Mode)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MeshTheme.colors.primary else Color.Black.copy(alpha = 0.4f))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom Type / Duration Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (media.mediaType) {
                        MediaType.VIDEO -> Icons.Default.PlayArrow
                        MediaType.VOICE_NOTE -> Icons.Default.Mic
                        MediaType.AUDIO -> Icons.Default.MusicNote
                        MediaType.DOCUMENT, MediaType.PDF, MediaType.APK, MediaType.ZIP -> Icons.Default.AttachFile
                        else -> Icons.Default.Image
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )

                    if (media.durationMs != null) {
                        Text(
                            text = formatMs(media.durationMs),
                            style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = media.title.takeLast(6),
                            style = MeshTheme.customTypography.caption,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", min, sec)
}
