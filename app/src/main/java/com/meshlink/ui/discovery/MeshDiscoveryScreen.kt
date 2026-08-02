package com.meshlink.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.responsive.MeshAdaptiveLayout
import com.meshlink.ui.nearby.NearbyUiState
import com.meshlink.ui.nearby.SortOption
import com.meshlink.util.MeshIdNormalizer

/**
 * MeshDiscoveryScreen — Flagship presentation layout for Phase 5 Nearby Devices experience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshDiscoveryScreen(
    uiState: NearbyUiState,
    onBack: () -> Unit,
    onToggleScan: () -> Unit,
    onRefresh: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onDeviceConnect: (BleDevice, onConnected: () -> Unit) -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DiscoveryFilterCategory.ALL) }
    var connectingAddress by remember { mutableStateOf<String?>(null) }
    var selectedDeviceAddress by remember { mutableStateOf<String?>(null) }

    // Map BleDevice domain objects to presentation NearbyDeviceUiState
    val allDeviceUiStates = remember(uiState.devices) {
        uiState.devices.map { NearbyDeviceUiState.fromDomain(it) }
    }

    // Filter devices based on search query and category chips
    val filteredDeviceUiStates = remember(allDeviceUiStates, searchQuery, selectedCategory) {
        allDeviceUiStates.filter { dev ->
            val matchesQuery = searchQuery.isBlank() ||
                    dev.name.contains(searchQuery, ignoreCase = true) ||
                    dev.address.contains(searchQuery, ignoreCase = true) ||
                    dev.meshId.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                DiscoveryFilterCategory.ALL -> true
                DiscoveryFilterCategory.CONNECTED -> dev.isConnected
                DiscoveryFilterCategory.NEARBY -> !dev.isConnected && !dev.hasRelayCapability
                DiscoveryFilterCategory.RELAY -> dev.hasRelayCapability
                DiscoveryFilterCategory.BLE -> dev.transportUi == TransportTypeUi.BLE
                DiscoveryFilterCategory.WIFI_DIRECT -> dev.transportUi == TransportTypeUi.WIFI_DIRECT
            }

            matchesQuery && matchesCategory
        }
    }

    val selectedDeviceUi = remember(selectedDeviceAddress, allDeviceUiStates) {
        allDeviceUiStates.firstOrNull { it.address == selectedDeviceAddress }
    }

    val totalNearbyCount = allDeviceUiStates.size
    val connectedCount = allDeviceUiStates.count { it.isConnected }

    AnimatedErrorDialog(
        visible = uiState.errorMessage != null,
        title = "Discovery Error",
        message = uiState.errorMessage ?: "",
        onDismiss = onClearError,
        primaryButtonText = "Try Again",
        onPrimaryClick = {
            onClearError()
            onRefresh()
        }
    )

    MeshScreen(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NearbyTopBar(
                totalNearbyCount = totalNearbyCount,
                connectedCount = connectedCount,
                isScanning = uiState.isScanning,
                onBackClick = onBack,
                onRefreshClick = onRefresh,
                onSortClick = {
                    val nextSort = when (uiState.sortOption) {
                        SortOption.RSSI -> SortOption.NAME
                        SortOption.NAME -> SortOption.STATUS
                        SortOption.STATUS -> SortOption.RSSI
                    }
                    onSortOptionSelected(nextSort)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MeshAdaptiveLayout(
                landscape = {
                    // Split Pane Layout for Landscape / Tablet / Foldables
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Discovery Hero Canvas
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                        ) {
                            MeshDiscoveryHero(
                                devices = uiState.devices,
                                selectedAddress = selectedDeviceAddress,
                                isScanning = uiState.isScanning,
                                onNodeSelected = { dev ->
                                    selectedDeviceAddress = if (selectedDeviceAddress == dev.address) null else dev.address
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }

                        // Right Pane: Search, Filters & Device List
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            NearbySearch(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            DiscoveryFilters(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DeviceList(
                                devices = filteredDeviceUiStates,
                                connectingAddress = connectingAddress,
                                selectedAddress = selectedDeviceAddress,
                                searchQuery = searchQuery,
                                onDeviceClick = { dev ->
                                    selectedDeviceAddress = dev.address
                                },
                                onConnectClick = { dev ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    connectingAddress = dev.address
                                    onDeviceConnect(dev.device) {
                                        onNavigateToChat(
                                            dev.meshId.ifBlank { dev.address },
                                            dev.name.ifBlank { MeshIdNormalizer.canonicalize(dev.address) }
                                        )
                                        connectingAddress = null
                                    }
                                },
                                onRefresh = onRefresh,
                                listState = listState,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                defaultLayout = {
                    // Standard Single Column Phone Portrait Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // 1. Mesh Discovery Hero Radar (35-40% Height)
                        MeshDiscoveryHero(
                            devices = uiState.devices,
                            selectedAddress = selectedDeviceAddress,
                            isScanning = uiState.isScanning,
                            onNodeSelected = { dev ->
                                selectedDeviceAddress = if (selectedDeviceAddress == dev.address) null else dev.address
                            },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 2. Search Bar
                        NearbySearch(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        // 3. Category Filter Chips
                        DiscoveryFilters(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // 4. Discovered Device List
                        DeviceList(
                            devices = filteredDeviceUiStates,
                            connectingAddress = connectingAddress,
                            selectedAddress = selectedDeviceAddress,
                            searchQuery = searchQuery,
                            onDeviceClick = { dev ->
                                selectedDeviceAddress = dev.address
                            },
                            onConnectClick = { dev ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                connectingAddress = dev.address
                                onDeviceConnect(dev.device) {
                                    onNavigateToChat(
                                        dev.meshId.ifBlank { dev.address },
                                        dev.name.ifBlank { MeshIdNormalizer.canonicalize(dev.address) }
                                    )
                                    connectingAddress = null
                                }
                            },
                            onRefresh = onRefresh,
                            listState = listState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )

            // Device Detail Bottom Sheet Modal Inspector
            DeviceDetailSheet(
                deviceUi = selectedDeviceUi,
                onDismiss = { selectedDeviceAddress = null },
                onConnectChat = { dev ->
                    connectingAddress = dev.address
                    onDeviceConnect(dev.device) {
                        onNavigateToChat(
                            dev.meshId.ifBlank { dev.address },
                            dev.name.ifBlank { MeshIdNormalizer.canonicalize(dev.address) }
                        )
                        connectingAddress = null
                    }
                },
                isConnecting = connectingAddress == selectedDeviceAddress
            )
        }
    }
}
