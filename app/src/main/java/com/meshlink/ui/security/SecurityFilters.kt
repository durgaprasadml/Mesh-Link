package com.meshlink.ui.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun SecurityFilterChipsRow(
    selectedCategory: SecurityFilterCategory,
    onCategorySelected: (SecurityFilterCategory) -> Unit,
    counts: Map<SecurityFilterCategory, Int> = emptyMap(),
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
        SecurityFilterCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            val count = counts[category]

            val chipContainerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                label = "FilterChipContainer"
            )

            val chipLabelColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "FilterChipLabel"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = if (count != null && count > 0) "${category.label} ($count)" else category.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = chipContainerColor,
                    labelColor = chipLabelColor,
                    selectedContainerColor = chipContainerColor,
                    selectedLabelColor = chipLabelColor
                ),
                border = null,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}
