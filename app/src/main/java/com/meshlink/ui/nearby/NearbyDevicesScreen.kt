package com.meshlink.ui.nearby

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.PermissionHandler
import com.meshlink.ui.discovery.MeshDiscoveryScreen

/**
 * NearbyDevicesScreen — Bridge composable connecting NearbyViewModel state & callbacks
 * to the flagship MeshDiscoveryScreen presentation UI layer.
 *
 * Preserves exact public API signature, ViewModel contract, and navigation routes.
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
