package com.meshlink.ui.discovery

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class DiscoveryFilterCategory {
    ALL,
    CONNECTED,
    NEARBY,
    RELAY,
    BLE,
    WIFI_DIRECT
}

/**
 * DiscoveryFilters — Horizontal scrollable Material 3 Filter Chips row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryFilters(
    selectedCategory: DiscoveryFilterCategory,
    onCategorySelected: (DiscoveryFilterCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val categories = listOf(
            DiscoveryFilterCategory.ALL to "All",
            DiscoveryFilterCategory.CONNECTED to "Connected",
            DiscoveryFilterCategory.NEARBY to "Nearby",
            DiscoveryFilterCategory.RELAY to "Relay",
            DiscoveryFilterCategory.BLE to "BLE",
            DiscoveryFilterCategory.WIFI_DIRECT to "Wi-Fi Direct"
        )

        categories.forEach { (category, label) ->
            val isSelected = selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(text = label) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                } else if (category == DiscoveryFilterCategory.BLE) {
                    {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null
                        )
                    }
                } else if (category == DiscoveryFilterCategory.WIFI_DIRECT) {
                    {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
