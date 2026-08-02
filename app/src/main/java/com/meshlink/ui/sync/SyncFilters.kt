package com.meshlink.ui.sync

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

enum class SyncFilterCategory(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    DELIVERED("Delivered"),
    FAILED("Failed"),
    RETRY("Retry"),
    QUEUE("Queue"),
    STORAGE("Storage"),
    PEERS("Peers")
}

/**
 * SyncFilters — Material Filter Chips for categorizing synchronization and queue items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncFilters(
    selectedCategory: SyncFilterCategory,
    onCategorySelected: (SyncFilterCategory) -> Unit,
    counts: Map<SyncFilterCategory, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(MeshSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SyncFilterCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            val count = counts[category]

            val chipContainerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(200),
                label = "ChipColor"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = if (count != null && count > 0) "${category.label} ($count)" else category.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = chipContainerColor,
                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}
