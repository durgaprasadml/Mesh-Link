package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.TransferUi

/**
 * Material 3 Modal Bottom Sheet for detailed Transfer & Metadata Inspection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDetailSheet(
    transfer: TransferUi?,
    onDismiss: () -> Unit,
    onOpenFile: ((TransferUi) -> Unit)? = null,
    onShareFile: ((TransferUi) -> Unit)? = null,
    onRetryTransfer: ((TransferUi) -> Unit)? = null,
    onDeleteFile: ((TransferUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (transfer == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MeshTheme.colors.surface,
        contentColor = MeshTheme.colors.onSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header Title
            Text(
                text = "Transfer Details",
                style = MeshTheme.customTypography.title.copy(fontWeight = FontWeight.Bold),
                color = MeshTheme.colors.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Encryption Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MeshTheme.colors.primary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "End-to-End Mesh Encrypted (AES-256 GCM)",
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = MeshTheme.colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Grid
            DetailItemRow(icon = Icons.Default.Description, label = "File Name", value = transfer.fileName)
            DetailItemRow(icon = Icons.Default.Speed, label = "Transfer Speed", value = transfer.speedFormatted)
            DetailItemRow(icon = Icons.Default.Folder, label = "Transport", value = transfer.transportType)
            DetailItemRow(icon = Icons.Default.Folder, label = "Size", value = "${transfer.totalSizeBytes / 1024} KB")
            DetailItemRow(icon = Icons.Default.Folder, label = "Priority", value = transfer.priorityName)
            DetailItemRow(icon = Icons.Default.Folder, label = "CRC Errors", value = "${transfer.crcErrors}")

            Spacer(modifier = Modifier.height(24.dp))

            // Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onOpenFile?.invoke(transfer); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MeshTheme.colors.primary)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open")
                }

                OutlinedButton(
                    onClick = { onShareFile?.invoke(transfer); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                IconButton(
                    onClick = { onDeleteFile?.invoke(transfer); onDismiss() }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MeshTheme.colors.error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MeshTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MeshTheme.customTypography.subtitle,
            color = MeshTheme.colors.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.SemiBold),
            color = MeshTheme.colors.onSurface
        )
    }
}
