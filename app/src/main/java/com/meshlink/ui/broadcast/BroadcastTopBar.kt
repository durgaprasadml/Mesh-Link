package com.meshlink.ui.broadcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.components.MeshTopAppBar
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastTopBar(
    stats: BroadcastStatisticsUi,
    isFilterActive: Boolean,
    onBack: () -> Unit,
    onToggleFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        MeshTopAppBar(
            title = "Broadcast Command",
            subtitle = "${stats.connectedPeerCount} Nearby Nodes Reachable",
            onBackClick = onBack,
            containerColor = MaterialTheme.colorScheme.surface,
            actions = {
                // Encryption indicator
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = MeshTheme.spacing.small)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Mesh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ENCRYPTED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Filter toggle button
                IconButton(
                    onClick = onToggleFilters,
                    modifier = Modifier.scaleOnPress(0.92f)
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Broadcasts",
                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isFilterActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        )

        // Sub-header metrics strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                label = "Total",
                count = stats.totalBroadcasts,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                label = "Delivered",
                count = stats.deliveredCount,
                color = Color(0xFF00F59B),
                modifier = Modifier.weight(1f)
            )
            if (stats.pendingCount > 0) {
                StatusChip(
                    label = "Pending",
                    count = stats.pendingCount,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
            if (stats.failedCount > 0) {
                StatusChip(
                    label = "Failed",
                    count = stats.failedCount,
                    color = Color(0xFFFF0055),
                    modifier = Modifier.weight(1f)
                )
            }
            if (stats.emergencyCount > 0) {
                StatusChip(
                    label = "SOS",
                    count = stats.emergencyCount,
                    color = Color(0xFFFF0055),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun StatusChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MeshTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
