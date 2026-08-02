package com.meshlink.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.nearby.SortOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    filterConnectedOnly: Boolean,
    onToggleConnectedOnly: (Boolean) -> Unit,
    filterRelayOnly: Boolean,
    onToggleRelayOnly: (Boolean) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = MeshTheme.elevation.level3
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshSpacing.ScreenPadding)
                .padding(bottom = MeshSpacing.XL)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter & Sort Mesh Nodes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    onSearchQueryChange("")
                    onSortChange(SortOption.RSSI)
                    onToggleConnectedOnly(false)
                    onToggleRelayOnly(false)
                }) {
                    Text("Reset All", color = MeshTheme.colors.primary)
                }
            }

            Spacer(modifier = Modifier.height(MeshSpacing.MD))

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search node name or MAC address...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MeshTheme.colors.primary,
                    unfocusedBorderColor = MeshTheme.colors.border
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(MeshSpacing.LG))

            // Sorting Options Section
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MeshSpacing.SM))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MeshSpacing.SM)
            ) {
                SortOption.entries.forEach { option ->
                    val isSelected = currentSort == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSortChange(option) },
                        label = {
                            Text(
                                text = when (option) {
                                    SortOption.RSSI -> "Signal (RSSI)"
                                    SortOption.NAME -> "Name"
                                    SortOption.STATUS -> "Connection Status"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeshTheme.colors.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MeshTheme.colors.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshSpacing.LG))

            // Filter Options Section
            Text(
                text = "Filter By Status & Capabilities",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MeshSpacing.SM))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MeshSpacing.SM)
            ) {
                FilterChip(
                    selected = filterConnectedOnly,
                    onClick = { onToggleConnectedOnly(!filterConnectedOnly) },
                    label = { Text("Connected Peers Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeshTheme.colors.connected.copy(alpha = 0.2f),
                        selectedLabelColor = MeshTheme.colors.connected
                    )
                )

                FilterChip(
                    selected = filterRelayOnly,
                    onClick = { onToggleRelayOnly(!filterRelayOnly) },
                    label = { Text("Relay Capable Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeshTheme.colors.warning.copy(alpha = 0.2f),
                        selectedLabelColor = MeshTheme.colors.warning
                    )
                )
            }
        }
    }
}
