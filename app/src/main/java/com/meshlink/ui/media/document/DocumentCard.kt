package com.meshlink.ui.media.document

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Rich document card preview for PDF, DOCX, XLSX, ZIP, APK, and binary files.
 */
@Composable
fun DocumentCard(
    media: MediaUi,
    onOpenClick: (MediaUi) -> Unit,
    onDownloadClick: ((MediaUi) -> Unit)? = null,
    onShareClick: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (docIcon, docColor) = getDocumentIconAndColor(media.mimeType, media.title)
    val formattedSize = formatFileSize(media.sizeBytes)
    val formattedDate = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(media.timestampMs))
    val extBadge = media.title.substringAfterLast('.', "").uppercase(Locale.getDefault()).take(5)

    MeshCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onOpenClick(media) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(docColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = docIcon,
                    contentDescription = media.title,
                    tint = docColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Document Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = media.title,
                        style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = MeshTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (extBadge.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = docColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = extBadge,
                                style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                                color = docColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedSize,
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                    Text(
                        text = "•",
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
                        text = "•",
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

            // Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDownloadClick != null && media.transferStatusName != "COMPLETED") {
                    IconButton(
                        onClick = { onDownloadClick(media) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Download ${media.title}",
                            tint = MeshTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (onShareClick != null) {
                    IconButton(
                        onClick = { onShareClick(media) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share ${media.title}",
                            tint = MeshTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getDocumentIconAndColor(mimeType: String, fileName: String): Pair<ImageVector, Color> {
    val lowerName = fileName.lowercase(Locale.getDefault())
    return when {
        mimeType.contains("pdf") || lowerName.endsWith(".pdf") ->
            Pair(Icons.Default.PictureAsPdf, Color(0xFFFF5252))
        mimeType.contains("word") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc") ->
            Pair(Icons.Default.Description, Color(0xFF29B6F6))
        mimeType.contains("excel") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") ->
            Pair(Icons.Default.TableChart, Color(0xFF66BB6A))
        mimeType.contains("zip") || mimeType.contains("compressed") || lowerName.endsWith(".zip") || lowerName.endsWith(".tar") || lowerName.endsWith(".gz") ->
            Pair(Icons.Default.FolderZip, Color(0xFFFFA726))
        mimeType.contains("android.package-archive") || lowerName.endsWith(".apk") ->
            Pair(Icons.Default.Android, Color(0xFF00F59B))
        else ->
            Pair(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFFAB47BC))
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size >= 1_073_741_824 -> String.format("%.2f GB", size / 1_073_741_824f)
        size >= 1_048_576 -> String.format("%.1f MB", size / 1_048_576f)
        size >= 1024 -> String.format("%.1f KB", size / 1024f)
        else -> "$size B"
    }
}
