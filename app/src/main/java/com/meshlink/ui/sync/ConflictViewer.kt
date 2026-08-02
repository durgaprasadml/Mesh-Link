package com.meshlink.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * ConflictViewer — Presentation-only inspector for duplicate messages, merge states, and sync conflicts.
 */
@Composable
fun ConflictViewerCard(
    conflicts: List<ConflictUi>,
    modifier: Modifier = Modifier
) {
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
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Conflicts",
                        tint = if (conflicts.isNotEmpty()) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sync Conflict & Merge Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.small,
                    color = if (conflicts.isEmpty()) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (conflicts.isEmpty()) "No Conflicts" else "${conflicts.size} Detected",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (conflicts.isEmpty()) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (conflicts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Clean",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Zero conflicts detected in current state",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = conflicts,
                        key = { it.id }
                    ) { item ->
                        ConflictRowItem(conflict = item)
                    }
                }
            }

            Surface(
                shape = MeshTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Read-Only Presentation View — Engine automatically handles conflict resolution using CRDT vectors.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ConflictRowItem(
    conflict: ConflictUi,
    modifier: Modifier = Modifier
) {
    val accentColor = when (conflict.conflictType) {
        "Duplicate Messages" -> Color(0xFF2196F3)
        "Pending Merge" -> Color(0xFFFF9800)
        "Sync Conflict" -> Color(0xFFF44336)
        "Resolved" -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conflict.conflictType,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor
                )
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(conflict.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Message ID: ${conflict.messageId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = conflict.details,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
