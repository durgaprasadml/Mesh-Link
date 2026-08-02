package com.meshlink.ui.nearby

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.PermissionHandler
import com.meshlink.ui.discovery.MeshDiscoveryScreen

/**
 * NearbyDevicesScreen — Bridge composable connecting NearbyViewModel state & callbacks
 * to the flagship MeshDiscoveryScreen presentation UI layer with edge-to-edge window insets.
 */
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            MeshDiscoveryScreen(
                uiState = uiState,
                onBack = onBack,
                onToggleScan = {
                    viewModel.startDiscovery()
                },
                onRefresh = {
                    viewModel.startDiscovery()
                },
                onSortOptionSelected = { option ->
                    viewModel.setSortOption(option)
                },
                onDeviceConnect = { device, onConnected ->
                    viewModel.connectToDevice(device, onConnected)
                },
                onNavigateToChat = onNavigateToChat,
                onClearError = {
                    viewModel.setErrorMessage(null)
                }
            )
        }
    }
}
