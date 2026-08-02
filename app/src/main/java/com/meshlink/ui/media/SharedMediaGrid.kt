package com.meshlink.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.animation.mediaShimmerBackground
import com.meshlink.ui.media.models.MediaType
import com.meshlink.ui.media.models.MediaUi

/**
 * Adaptive Google Photos / Pinterest style Media Grid for images, videos, and GIFs.
 */
@Composable
fun SharedMediaGrid(
    mediaList: List<MediaUi>,
    onItemClick: (MediaUi) -> Unit,
    selectedMediaIds: Set<String> = emptySet(),
    onItemLongClick: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (mediaList.isEmpty()) {
        NoMedia(modifier = modifier)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = mediaList,
            key = { it.id }
        ) { media ->
            SharedMediaGridTile(
                media = media,
                isSelected = selectedMediaIds.contains(media.id),
                onClick = { onItemClick(media) },
                onLongClick = { onItemLongClick?.invoke(media) }
            )
        }
    }
}

@Composable
private fun SharedMediaGridTile(
    media: MediaUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MeshTheme.colors.surfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Placeholder / Shimmer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .mediaShimmerBackground()
            )

            // Content Overlay Info / Type Badge
            if (media.mediaType == MediaType.VIDEO) {
                // Play overlay badge
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Duration tag at bottom right
                if (media.durationMs != null && media.durationMs > 0) {
                    val seconds = (media.durationMs / 1000) % 60
                    val minutes = (media.durationMs / 1000) / 60
                    val durationText = String.format("%d:%02d", minutes, seconds)

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = durationText,
                            style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Title/Sender caption pill at bottom left
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = media.title.take(12),
                    style = MeshTheme.customTypography.caption,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Selected Checkmark overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MeshTheme.colors.primary.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}
