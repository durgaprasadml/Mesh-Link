package com.meshlink.ui.notifications

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun NotificationFilterChips(
    selectedCategory: NotificationCategory,
    onCategorySelected: (NotificationCategory) -> Unit,
    categories: List<NotificationCategoryUi>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            val catInfo = categories.find { it.category == category }
            val unreadCount = catInfo?.unreadCount ?: 0

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(category.label)
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            ) {
                                Text(unreadCount.toString())
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
