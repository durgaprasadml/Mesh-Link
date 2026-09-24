package com.meshlink.ui.nearby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.nearby.MeshDeviceCard
import com.meshlink.ui.components.nearby.MeshNetworkStatsBar
import com.meshlink.ui.components.nearby.MeshScanningEmptyState
import com.meshlink.ui.components.nearby.MeshTopologyCanvas
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.launch

/**
 * Modern, professional Nearby Mesh Network Screen.
 *
 * Visual hierarchy:
 * 1. Compact Material 3 TopAppBar with live discovery state.
 * 2. Restrained Mesh Discovery Canvas header (central "You" node, real peers, gentle sweep).
 * 3. Responsive 4-column live network statistics strip without clipping.
 * 4. Compact, streamlined search and sort bar.
 * 5. Real peer list or calm, purposeful empty discovery state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDevicesScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    var connectingToAddress by remember { mutableStateOf<String?>(null) }
    var selectedDeviceAddress by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

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
            viewModel.refreshDiscovery()
        }
    )

    val allPeers = uiState.allDiscoveredDevices.ifEmpty { uiState.devices }
    val totalPeers = allPeers.size
    val connectedPeers = remember(allPeers) { allPeers.count { it.isConnected } }
    val subtitleText = when {
        uiState.isScanning && totalPeers > 0 -> "$totalPeers nearby · $connectedPeers connected"
        uiState.isScanning -> "Discovering nearby peers"
        totalPeers > 0 -> "$totalPeers peers · Discovery paused"
        else -> "Discovery paused"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nearby Mesh Network",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.refreshDiscovery()
                        },
                        modifier = Modifier.semantics { contentDescription = "Refresh mesh discovery" }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            // 1. Apple-Inspired Discovery Visualization Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                MeshTopologyCanvas(
                    devices = allPeers,
                    selectedAddress = selectedDeviceAddress,
                    searchQuery = uiState.searchQuery,
                    isScanning = uiState.isScanning,
                    onNodeSelected = { device ->
                        selectedDeviceAddress = if (selectedDeviceAddress == device.address) null else device.address
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

            // 2. Sleek Compact Live Network Status Strip
            MeshNetworkStatsBar(
                devices = allPeers,
                isScanning = uiState.isScanning
            )

            // 3. Compact, Clean Search & Filter Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MeshTheme.spacing.mediumLarge,
                        vertical = MeshTheme.spacing.extraSmall
                    )
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search mesh peers by name…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search query",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort peers",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
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
                                trailingIcon = {
                                    if (uiState.sortOption == SortOption.RSSI) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NAME)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (uiState.sortOption == SortOption.NAME) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Status") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.STATUS)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (uiState.sortOption == SortOption.STATUS) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))

            // 4. Peer List or Purposeful Empty State
            if (uiState.devices.isEmpty()) {
                MeshScanningEmptyState(
                    title = if (uiState.searchQuery.isBlank()) "Looking for nearby Mesh-Link peers" else "No matching peers",
                    description = if (uiState.searchQuery.isBlank()) {
                        "Keep Mesh-Link open while discovery continues. Nearby devices running Mesh-Link will appear here automatically."
                    } else {
                        "No peers match \"${uiState.searchQuery}\". Try a different name."
                    },
                    isScanning = uiState.isScanning,
                    isSearchEmpty = uiState.searchQuery.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = MeshTheme.spacing.mediumLarge),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = if (uiState.searchQuery.isBlank()) {
                                "Nearby Devices (${uiState.devices.size})"
                            } else {
                                "Search Results (${uiState.devices.size})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(
                        items = uiState.devices,
                        key = { it.meshId.ifBlank { it.address } },
                        contentType = { "peer_item" }
                    ) { device ->
                        MeshDeviceCard(
                            device = device,
                            isConnecting = connectingToAddress == device.address,
                            isSelected = selectedDeviceAddress == device.address,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDeviceAddress = device.address
                                connectingToAddress = device.address
                                viewModel.connectToDevice(
                                    device = device,
                                    onSuccess = {
                                        connectingToAddress = null
                                        val targetId = device.meshId.ifBlank { device.address }
                                        val targetName = device.displayName ?: device.name.ifBlank { "Mesh Peer" }
                                        onNavigateToChat(targetId, targetName)
                                    },
                                    onError = {
                                        connectingToAddress = null
                                    }
                                )
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
                    }
                }
            }
        }
    }
}
