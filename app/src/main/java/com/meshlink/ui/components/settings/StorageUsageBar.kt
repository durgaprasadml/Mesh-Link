package com.meshlink.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

data class StorageCategory(
    val name: String,
    val bytes: Long,
    val color: Color
)

@Composable
fun StorageUsageBar(
    categories: List<StorageCategory>,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val maxBytes = totalBytes.coerceAtLeast(1L)
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MeshTheme.spacing.extraLarge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val weight = (category.bytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
                if (weight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(category.color)
                    )
                }
            }
            // Fill remaining space if any
            val remainingWeight = 1f - categories.sumOf { (it.bytes.toFloat() / maxBytes.toFloat()).toDouble() }.toFloat()
            if (remainingWeight > 0f) {
                Spacer(modifier = Modifier.weight(remainingWeight))
            }
        }
        
        Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
        
        // Legend
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowCategories.forEach { category ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MeshTheme.spacing.medium)
                                .clip(CircleShape)
                                .background(category.color)
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = formatBytes(category.bytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}
