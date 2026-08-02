package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaType
import com.meshlink.ui.media.models.MediaUi

/**
 * Dedicated preview card component for PDF, DOC, ZIP, APK, TXT, and binary files.
 */
@Composable
fun FilePreview(
    media: MediaUi,
    onOpenClick: (MediaUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val fileIcon = getFilePreviewIcon(media.mediaType, media.title)
    val typeBadgeText = getFileBadgeText(media.mediaType, media.title)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MeshTheme.colors.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Large File Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MeshTheme.colors.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileIcon,
                    contentDescription = typeBadgeText,
                    tint = MeshTheme.colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File Name
            Text(
                text = media.title,
                style = MeshTheme.customTypography.title.copy(fontWeight = FontWeight.Bold),
                color = MeshTheme.colors.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Type Badge & Metadata
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MeshTheme.colors.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = typeBadgeText,
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                        color = MeshTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${media.sizeBytes / 1024} KB • Shared by ${media.senderName}",
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Open Action Button
            Button(
                onClick = { onOpenClick(media) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MeshTheme.colors.primary)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open File", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open File")
            }
        }
    }
}

private fun getFilePreviewIcon(type: MediaType, title: String): ImageVector {
    return when {
        type == MediaType.PDF || title.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        type == MediaType.APK || title.endsWith(".apk", ignoreCase = true) -> Icons.Default.Android
        type == MediaType.ZIP || title.endsWith(".zip", ignoreCase = true) -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }
}

private fun getFileBadgeText(type: MediaType, title: String): String {
    return when {
        type == MediaType.PDF || title.endsWith(".pdf", ignoreCase = true) -> "PDF DOCUMENT"
        type == MediaType.APK || title.endsWith(".apk", ignoreCase = true) -> "ANDROID PACKAGE"
        type == MediaType.ZIP || title.endsWith(".zip", ignoreCase = true) -> "ZIP ARCHIVE"
        else -> "DOCUMENT"
    }
}
