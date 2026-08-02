package com.meshlink.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * PeerAvailability — Grid/list visualizing peer connectivity status across the mesh network.
 */
@Composable
fun PeerAvailabilityCard(
    peers: List<PeerStatusUi>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<PeerAvailabilityStatus?>(null) }

    val filteredPeers = remember(peers, selectedFilter) {
        if (selectedFilter == null) peers else peers.filter { it.status == selectedFilter }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Peer Availability",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mesh Peer Availability",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${peers.count { it.status == PeerAvailabilityStatus.ONLINE }} Online",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All (${peers.size})", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = selectedFilter == PeerAvailabilityStatus.ONLINE,
                    onClick = { selectedFilter = PeerAvailabilityStatus.ONLINE },
                    label = { Text("Online", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = selectedFilter == PeerAvailabilityStatus.RELAY_AVAILABLE,
                    onClick = { selectedFilter = PeerAvailabilityStatus.RELAY_AVAILABLE },
                    label = { Text("Relays", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = selectedFilter == PeerAvailabilityStatus.OFFLINE,
                    onClick = { selectedFilter = PeerAvailabilityStatus.OFFLINE },
                    label = { Text("Offline", style = MaterialTheme.typography.labelSmall) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (filteredPeers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No peers matching filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filteredPeers,
                        key = { it.peerId }
                    ) { peer ->
                        PeerStatusRow(peer = peer)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerStatusRow(
    peer: PeerStatusUi,
    modifier: Modifier = Modifier
) {
    val statusColor = when (peer.status) {
        PeerAvailabilityStatus.ONLINE -> Color(0xFF4CAF50)
        PeerAvailabilityStatus.RELAY_AVAILABLE -> Color(0xFF9C27B0)
        PeerAvailabilityStatus.REACHABLE -> Color(0xFF2196F3)
        PeerAvailabilityStatus.RECENTLY_SEEN -> Color(0xFFFF9800)
        PeerAvailabilityStatus.OFFLINE -> Color(0xFF9E9E9E)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(MeshTheme.shapes.tiny)
                    .background(statusColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: ${peer.peerId.take(12)} • RSSI: ${peer.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (peer.isRelay) {
                Surface(
                    shape = MeshTheme.shapes.tiny,
                    color = Color(0xFF9C27B0).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Relay",
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RELAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9C27B0)
                        )
                    }
                }
            }

            Surface(
                shape = MeshTheme.shapes.tiny,
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = peer.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
