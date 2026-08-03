package com.meshlink.ui.nearby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.PermissionHandler
import com.meshlink.ui.components.nearby.MeshDeviceCard
import com.meshlink.ui.components.nearby.MeshNetworkStatsBar
import com.meshlink.ui.components.nearby.MeshScanningEmptyState
import com.meshlink.ui.components.nearby.MeshTopologyCanvas
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.launch

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

        var isSearchActive by remember { mutableStateOf(false) }
        var connectingToAddress by remember { mutableStateOf<String?>(null) }
        var selectedDeviceAddress by remember { mutableStateOf<String?>(null) }

        val haptic = LocalHapticFeedback.current
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        AnimatedErrorDialog(
            visible = uiState.errorMessage != null,
            title = "Discovery Error",
            message = uiState.errorMessage ?: "",
            onDismiss = { viewModel.setErrorMessage(null) },
            primaryButtonText = "Try Again",
            onPrimaryClick = {
                viewModel.setErrorMessage(null)
                viewModel.startDiscovery()
            }
        )

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = "Nearby Mesh Network",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isScanning) "Continuously discovering peers via BLE..." else "Mesh Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.semantics { contentDescription = "Navigate back" }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = null,
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
                // Interactive Topology Visualization Canvas Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    MeshTopologyCanvas(
                        devices = uiState.devices,
                        selectedAddress = selectedDeviceAddress,
                        onNodeSelected = { device ->
                            selectedDeviceAddress = if (selectedDeviceAddress == device.address) null else device.address
                            // Scroll list to selected device if present
                            val index = uiState.devices.indexOfFirst { it.address == device.address }
                            if (index >= 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Live Network Statistics Bar
                MeshNetworkStatsBar(
                    devices = uiState.devices,
                    isScanning = uiState.isScanning
                )

                // Search and Filter Bar
                Box(
                    modifier = Modifier.padding(
                        horizontal = MeshTheme.spacing.mediumLarge, 
                        vertical = MeshTheme.spacing.small
                    )
                ) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                                onSearch = { isSearchActive = false },
                                expanded = isSearchActive,
                                onExpandedChange = { isSearchActive = it },
                                placeholder = { Text("Search mesh peers by name...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                                trailingIcon = {
                                    Row {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                                            }
                                        }
                                        var showSortMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showSortMenu = true }) {
                                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort devices")
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
                        modifier = Modifier.fillMaxWidth()
                    ) { }
                }

                // Interactive Topological Canvas Map (Weight 0.38)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f)
                        .padding(horizontal = MeshTheme.spacing.mediumLarge)
                ) {
                    MeshTopologyCanvas(
                        devices = uiState.devices,
                        selectedAddress = selectedDeviceAddress,
                        onNodeSelected = { device ->
                            selectedDeviceAddress = device.address
                        }
                    )
                }

                // Device List or Empty State
                if (uiState.devices.isEmpty()) {
                    MeshScanningEmptyState(
                        title = if (uiState.searchQuery.isBlank()) "Scanning for Mesh Nodes" else "No matching peers",
                        description = if (uiState.searchQuery.isBlank()) "Looking for active Mesh Link devices over BLE..." else "Try searching with a different device name or MAC address.",
                        modifier = Modifier.weight(0.62f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.62f)
                            .padding(horizontal = MeshTheme.spacing.mediumLarge),
                        verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                    ) {
                        items(
                            items = uiState.devices, 
                            key = { it.address }, 
                            contentType = { "device_item" }
                        ) { device ->
                            MeshDeviceCard(
                                device = device, 
                                isConnecting = connectingToAddress == device.address,
                                isSelected = selectedDeviceAddress == device.address,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedDeviceAddress = device.address
                                    connectingToAddress = device.address
                                    viewModel.connectToDevice(device) {
                                        onNavigateToChat(
                                            device.meshId.ifBlank { device.address },
                                            device.name.ifBlank { com.meshlink.util.MeshIdNormalizer.canonicalize(device.address) }
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
