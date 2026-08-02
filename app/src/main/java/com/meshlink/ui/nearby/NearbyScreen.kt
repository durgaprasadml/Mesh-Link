package com.meshlink.ui.nearby

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * NearbyScreen bridge composable alias delegating to NearbyDevicesScreen.
 */
@Composable
fun NearbyScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel()
) {
    NearbyDevicesScreen(
        onBack = onBack,
        onNavigateToChat = onNavigateToChat,
        viewModel = viewModel
    )
}
