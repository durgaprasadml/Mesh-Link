package com.meshlink.ui.broadcast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastFiltersSheet(
    filterState: BroadcastFilterState,
    onUpdateFilter: (BroadcastFilterState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember(filterState.searchQuery) { mutableStateOf(filterState.searchQuery) }
    var selectedPriority by remember(filterState.selectedPriority) { mutableStateOf(filterState.selectedPriority) }
    var selectedDeliveryState by remember(filterState.selectedDeliveryState) { mutableStateOf(filterState.selectedDeliveryState) }
    var emergencyOnly by remember(filterState.emergencyOnly) { mutableStateOf(filterState.emergencyOnly) }
    var filterMeOnly by remember(filterState.filterMeOnly) { mutableStateOf(filterState.filterMeOnly) }
    var filterPeersOnly by remember(filterState.filterPeersOnly) { mutableStateOf(filterState.filterPeersOnly) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = MeshTheme.elevation.level3
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshTheme.spacing.medium)
                .padding(bottom = MeshTheme.spacing.giant)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                    Text(
                        text = "Broadcast Filters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = {
                        query = ""
                        selectedPriority = null
                        selectedDeliveryState = null
                        emergencyOnly = false
                        filterMeOnly = false
                        filterPeersOnly = false
                        onUpdateFilter(BroadcastFilterState())
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset All")
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))

            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onUpdateFilter(
                        filterState.copy(searchQuery = it)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by keyword or sender...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = {
                            query = ""
                            onUpdateFilter(filterState.copy(searchQuery = ""))
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                } else null,
                shape = MeshTheme.shapes.medium,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Priority Section
            Text(
                text = "Priority Level",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.extraSmall)
            ) {
                FilterChip(
                    selected = selectedPriority == null && !emergencyOnly,
                    onClick = {
                        selectedPriority = null
                        emergencyOnly = false
                        onUpdateFilter(filterState.copy(selectedPriority = null, emergencyOnly = false))
                    },
                    label = { Text("All") }
                )
                BroadcastPriority.values().forEach { priority ->
                    val isSelected = if (priority.isEmergency) emergencyOnly else selectedPriority == priority
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (priority.isEmergency) {
                                emergencyOnly = !emergencyOnly
                                selectedPriority = null
                                onUpdateFilter(filterState.copy(emergencyOnly = emergencyOnly, selectedPriority = null))
                            } else {
                                selectedPriority = if (isSelected) null else priority
                                emergencyOnly = false
                                onUpdateFilter(filterState.copy(selectedPriority = selectedPriority, emergencyOnly = false))
                            }
                        },
                        label = { Text(priority.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(priority.containerColor),
                            selectedLabelColor = Color(priority.badgeColor)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Delivery Status Section
            Text(
                text = "Delivery Status",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.extraSmall)
            ) {
                FilterChip(
                    selected = selectedDeliveryState == null,
                    onClick = {
                        selectedDeliveryState = null
                        onUpdateFilter(filterState.copy(selectedDeliveryState = null))
                    },
                    label = { Text("All Statuses") }
                )
                listOf(
                    BroadcastDeliveryState.DELIVERED,
                    BroadcastDeliveryState.PENDING,
                    BroadcastDeliveryState.FAILED
                ).forEach { state ->
                    val isSelected = selectedDeliveryState == state
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDeliveryState = if (isSelected) null else state
                            onUpdateFilter(filterState.copy(selectedDeliveryState = selectedDeliveryState))
                        },
                        label = { Text(state.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Sender Filter Section
            Text(
                text = "Sender",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.extraSmall)
            ) {
                FilterChip(
                    selected = !filterMeOnly && !filterPeersOnly,
                    onClick = {
                        filterMeOnly = false
                        filterPeersOnly = false
                        onUpdateFilter(filterState.copy(filterMeOnly = false, filterPeersOnly = false))
                    },
                    label = { Text("Everyone") }
                )
                FilterChip(
                    selected = filterMeOnly,
                    onClick = {
                        filterMeOnly = !filterMeOnly
                        filterPeersOnly = false
                        onUpdateFilter(filterState.copy(filterMeOnly = filterMeOnly, filterPeersOnly = false))
                    },
                    label = { Text("Sent by Me") }
                )
                FilterChip(
                    selected = filterPeersOnly,
                    onClick = {
                        filterPeersOnly = !filterPeersOnly
                        filterMeOnly = false
                        onUpdateFilter(filterState.copy(filterPeersOnly = filterPeersOnly, filterMeOnly = false))
                    },
                    label = { Text("Received from Peers") }
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scaleOnPress(0.98f),
                shape = MeshTheme.shapes.medium
            ) {
                Text("Apply & Close Filters")
            }
        }
    }
}
