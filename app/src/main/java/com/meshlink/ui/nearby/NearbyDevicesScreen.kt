package com.meshlink.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.components.PermissionHandler
import com.meshlink.ui.components.LoadingOverlay
import com.meshlink.ui.components.nearby.MeshDeviceCard
import com.meshlink.ui.components.nearby.MeshTopologyCanvas
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDevicesScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel()
) {
    PermissionHandler {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.startDiscovery()
        }

        var searchQuery by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }
        var connectingToAddress by remember { mutableStateOf<String?>(null) }

        val filteredDevices = remember(searchQuery, uiState.devices) {
            if (searchQuery.isBlank()) {
                uiState.devices
            } else {
                uiState.devices.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "Mesh Network",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Interactive Mesh Visualization Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    MeshTopologyCanvas(
                        devices = uiState.devices, // Always show all devices in the canvas, regardless of search filter
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Search and Filter Bar
                Box(modifier = Modifier.padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium)) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearch = { isSearchActive = false },
                                expanded = isSearchActive,
                                onExpandedChange = { isSearchActive = it },
                                placeholder = { Text("Search mesh peers") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                trailingIcon = {
                                    Row {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                        var showSortMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showSortMenu = true }) {
                                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                                            }
                                            DropdownMenu(
                                                expanded = showSortMenu,
                                                onDismissRequest = { showSortMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Sort by Signal (RSSI)") },
                                                    onClick = { 
                                                        viewModel.setSortOption(SortOption.RSSI)
                                                        showSortMenu = false 
                                                    },
                                                    trailingIcon = { if (uiState.sortOption == SortOption.RSSI) Icon(Icons.Default.Check, "") }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Sort by Name") },
                                                    onClick = { 
                                                        viewModel.setSortOption(SortOption.NAME)
                                                        showSortMenu = false 
                                                    },
                                                    trailingIcon = { if (uiState.sortOption == SortOption.NAME) Icon(Icons.Default.Check, "") }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Sort by Status") },
                                                    onClick = { 
                                                        viewModel.setSortOption(SortOption.STATUS)
                                                        showSortMenu = false 
                                                    },
                                                    trailingIcon = { if (uiState.sortOption == SortOption.STATUS) Icon(Icons.Default.Check, "") }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        },
                        expanded = isSearchActive,
                        onExpandedChange = { isSearchActive = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        // Realtime results handled below
                    }
                }

                // Error State, Empty State, or Device List
                if (uiState.errorMessage != null) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Discovery Error",
                        description = uiState.errorMessage!!,
                        modifier = Modifier.weight(0.65f)
                    )
                } else if (filteredDevices.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.BluetoothSearching,
                        title = if (searchQuery.isBlank()) "No nearby devices" else "No matching peers",
                        description = if (searchQuery.isBlank()) "Looking for active Mesh Link nodes via BLE and Wi-Fi Direct..." else "Adjust your search terms.",
                        modifier = Modifier.weight(0.65f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.65f)
                            .padding(horizontal = MeshTheme.spacing.mediumLarge),
                        verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                    ) {
                        items(filteredDevices, key = { it.address }, contentType = { "device_item" }) { device ->
                            MeshDeviceCard(
                                device = device, 
                                isConnecting = connectingToAddress == device.address,
                                onClick = {
                                    connectingToAddress = device.address
                                    viewModel.connectToDevice(device) {
                                        onNavigateToChat(
                                            device.meshId.ifBlank { device.address },
                                            device.name.ifBlank { device.address.takeLast(8) }
                                        )
                                        connectingToAddress = null
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge)) }
                    }
                }
            }
        }
    }
}
