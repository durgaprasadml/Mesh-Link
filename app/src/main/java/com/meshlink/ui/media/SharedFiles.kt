package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaType
import com.meshlink.ui.media.models.MediaUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Material Messaging-style document list component for documents, archives, APKs, and files.
 */
@Composable
fun SharedFiles(
    filesList: List<MediaUi>,
    onFileClick: (MediaUi) -> Unit,
    onOpenClick: (MediaUi) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (filesList.isEmpty()) {
        NoMedia(
            title = "No files shared",
            subtitle = "Documents, archives, and APKs shared over Mesh-Link will appear here.",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = filesList,
            key = { it.id }
        ) { media ->
            SharedFileRowItem(
                media = media,
                onClick = { onFileClick(media) },
                onOpenClick = { onOpenClick(media) }
            )
            HorizontalDivider(
                color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 72.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun SharedFileRowItem(
    media: MediaUi,
    onClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileIcon = getFileIcon(media.mediaType, media.mimeType, media.title)
    val formattedDate = rememberFormattedDate(media.timestampMs)
    val formattedSize = rememberFormattedSize(media.sizeBytes)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MeshTheme.colors.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fileIcon,
                contentDescription = media.mediaType.name,
                tint = MeshTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.title,
                style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                color = MeshTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedSize,
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
                Text(
                    text = " • ",
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
                Text(
                    text = media.senderName,
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " • ",
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
                Text(
                    text = formattedDate,
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onOpenClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open File",
                tint = MeshTheme.colors.primary
            )
        }
    }
}

private fun getFileIcon(type: MediaType, mime: String, title: String): ImageVector {
    return when {
        type == MediaType.PDF || title.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        type == MediaType.APK || title.endsWith(".apk", ignoreCase = true) -> Icons.Default.Android
        type == MediaType.ZIP || title.endsWith(".zip", ignoreCase = true) || title.endsWith(".tar", ignoreCase = true) -> Icons.Default.FolderZip
        type == MediaType.AUDIO || type == MediaType.VOICE_NOTE -> Icons.Default.AudioFile
        else -> Icons.Default.Description
    }
}

private fun rememberFormattedDate(timestampMs: Long): String {
    if (timestampMs <= 0) return "Recent"
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}

private fun rememberFormattedSize(sizeBytes: Long): String {
    return when {
        sizeBytes >= 1048576 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / 1048576f)
        sizeBytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", sizeBytes / 1024f)
        else -> "$sizeBytes B"
    }
}
