package com.meshlink.ui.media.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.TransferDirectionUi
import com.meshlink.ui.media.models.TransferStatus
import com.meshlink.ui.media.models.TransferUi

/**
 * Grouped transfer activity log view supporting status filter tabs and retry/remove.
 */
@Composable
fun TransferHistoryList(
    transfers: List<TransferUi>,
    onRetryClick: ((String) -> Unit)? = null,
    onCancelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Active, 2: Completed, 3: Failed
    val tabs = listOf("All", "Active", "Completed", "Failed")

    val filteredTransfers = remember(transfers, selectedTab) {
        when (selectedTab) {
            1 -> transfers.filter { it.status == TransferStatus.TRANSFERRING || it.status == TransferStatus.PREPARING || it.status == TransferStatus.PAUSED }
            2 -> transfers.filter { it.status == TransferStatus.COMPLETED }
            3 -> transfers.filter { it.status == TransferStatus.FAILED || it.status == TransferStatus.CANCELLED }
            else -> transfers
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MeshTheme.colors.surface,
            contentColor = MeshTheme.colors.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                )
            }
        }

        if (filteredTransfers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transfer history records found",
                    style = MeshTheme.customTypography.body,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredTransfers,
                    key = { item -> item.transferId }
                ) { transfer ->
                    TransferHistoryRowItem(
                        transfer = transfer,
                        onRetryClick = onRetryClick,
                        onCancelClick = onCancelClick
                    )
                }
            }
        }
    }
}

@Composable
fun TransferHistoryRowItem(
    transfer: TransferUi,
    onRetryClick: ((String) -> Unit)?,
    onCancelClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isUploading = transfer.direction == TransferDirectionUi.OUTGOING
    val statusColor = when (transfer.status) {
        TransferStatus.COMPLETED -> MeshTheme.colors.primary
        TransferStatus.FAILED -> MeshTheme.colors.error
        TransferStatus.PAUSED -> MeshTheme.colors.warning
        else -> MeshTheme.colors.secondary
    }

    MeshCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUploading) Icons.Default.FileUpload else Icons.Default.FileDownload,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transfer.fileName,
                    style = MeshTheme.customTypography.subtitle,
                    color = MeshTheme.colors.onSurface,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transfer.status.name,
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Text(
                        text = "•",
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                    Text(
                        text = "${transfer.transferredSizeBytes / 1024} KB / ${transfer.totalSizeBytes / 1024} KB",
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                }
            }

            if (transfer.status == TransferStatus.FAILED && onRetryClick != null) {
                IconButton(onClick = { onRetryClick(transfer.transferId) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Transfer",
                        tint = MeshTheme.colors.primary
                    )
                }
            }
        }
    }
}
