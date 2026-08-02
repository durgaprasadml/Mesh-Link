package com.meshlink.ui.discovery

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.responsive.MeshAdaptiveLayout
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.nearby.NearbyUiState
import com.meshlink.ui.nearby.SortOption
import com.meshlink.util.MeshIdNormalizer
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var connectingAddress by remember { mutableStateOf<String?>(null) }
    var selectedDeviceAddress by remember { mutableStateOf<String?>(null) }
    var isFilterPanelVisible by remember { mutableStateOf(false) }
    var filterConnectedOnly by remember { mutableStateOf(false) }
    var filterRelayOnly by remember { mutableStateOf(false) }

    // Map BleDevice to Presentation NearbyDeviceUiState
    val allDeviceUiStates = remember(uiState.devices) {
        uiState.devices.map { NearbyDeviceUiState.fromDomain(it) }
    }

    // Filter devices based on query and chip selections
    val filteredDeviceUiStates = remember(allDeviceUiStates, searchQuery, filterConnectedOnly, filterRelayOnly) {
        allDeviceUiStates.filter { dev ->
            val matchesQuery = searchQuery.isBlank() ||
                    dev.name.contains(searchQuery, ignoreCase = true) ||
                    dev.address.contains(searchQuery, ignoreCase = true)
            val matchesConnected = !filterConnectedOnly || dev.isConnected
            val matchesRelay = !filterRelayOnly || dev.hasRelayCapability
            matchesQuery && matchesConnected && matchesRelay
        }
    }

    val selectedDeviceUi = remember(selectedDeviceAddress, allDeviceUiStates) {
        allDeviceUiStates.firstOrNull { it.address == selectedDeviceAddress }
    }

    val totalNearbyCount = allDeviceUiStates.size
    val connectedCount = allDeviceUiStates.count { it.isConnected }
    val relayCount = allDeviceUiStates.count { it.hasRelayCapability }

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
            DiscoveryTopBar(
                totalNearbyCount = totalNearbyCount,
                connectedCount = connectedCount,
                relayCount = relayCount,
                isScanning = uiState.isScanning,
                onBackClick = onBack,
                onRefreshClick = onRefresh,
                onFilterClick = { isFilterPanelVisible = true }
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
                    // Split Pane Layout for Landscape / Tablet
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MeshSpacing.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(MeshSpacing.LG)
                    ) {
                        // Left Column: Radar View Canvas & Stats
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                        ) {
                            MeshGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                cornerRadius = MeshSpacing.CardCornerRadius,
                                glowColor = MaterialTheme.colorScheme.primary,
                                glowRadius = 240f
                            ) {
                                MeshRadarView(
                                    devices = uiState.devices,
                                    selectedAddress = selectedDeviceAddress,
                                    isScanning = uiState.isScanning,
                                    latestPacketEvent = uiState.latestPacketEvent,
                                    onNodeSelected = { dev ->
                                        selectedDeviceAddress = if (selectedDeviceAddress == dev.address) null else dev.address
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(MeshSpacing.MD))
                            NetworkStatistics(
                                devices = filteredDeviceUiStates,
                                packetCount = uiState.packetCount,
                                isScanning = uiState.isScanning
                            )
                        }

                        // Right Column: Device List
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
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
                                listState = listState,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                defaultLayout = {
                    // Standard Phone Portrait Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MeshSpacing.ScreenPadding, vertical = MeshSpacing.SM)
                    ) {
                        // Top Section: Mesh Tactical Radar View Card
                        MeshGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.40f)
                                .padding(bottom = MeshSpacing.MD),
                            cornerRadius = MeshSpacing.CardCornerRadius,
                            glowColor = MaterialTheme.colorScheme.primary,
                            glowRadius = 200f
                        ) {
                            MeshRadarView(
                                devices = uiState.devices,
                                selectedAddress = selectedDeviceAddress,
                                isScanning = uiState.isScanning,
                                latestPacketEvent = uiState.latestPacketEvent,
                                onNodeSelected = { dev ->
                                    selectedDeviceAddress = if (selectedDeviceAddress == dev.address) null else dev.address
                                    val index = filteredDeviceUiStates.indexOfFirst { it.address == dev.address }
                                    if (index >= 0) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(index)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Telemetry Statistics Dashboard Bar
                        NetworkStatistics(
                            devices = filteredDeviceUiStates,
                            packetCount = uiState.packetCount,
                            isScanning = uiState.isScanning,
                            modifier = Modifier.padding(bottom = MeshSpacing.MD)
                        )

                        // Main Discovered Device List
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
                            listState = listState,
                            modifier = Modifier.weight(0.60f)
                        )
                    }

                    // Floating Glass Control Action Bar
                    DiscoveryControls(
                        isScanning = uiState.isScanning,
                        onToggleScan = onToggleScan,
                        onRefresh = onRefresh,
                        onFilterClick = { isFilterPanelVisible = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = MeshSpacing.MD)
                            .padding(horizontal = MeshSpacing.ScreenPadding)
                    )
                }
            )

            // Bottom Sheet Filter Drawer
            FilterPanel(
                visible = isFilterPanelVisible,
                onDismiss = { isFilterPanelVisible = false },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                currentSort = uiState.sortOption,
                onSortChange = onSortOptionSelected,
                filterConnectedOnly = filterConnectedOnly,
                onToggleConnectedOnly = { filterConnectedOnly = it },
                filterRelayOnly = filterRelayOnly,
                onToggleRelayOnly = { filterRelayOnly = it }
            )

            // Modal Device Inspector Bottom Sheet
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
