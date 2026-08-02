package com.meshlink.ui.sos

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Public entry-point for the SOS Emergency feature in Mesh-Link.
 * Retains exact public contract and delegates 100% of UI presentation to [MeshEmergencyScreen].
 * All state management and business logic remain strictly inside [SosViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    onBack: () -> Unit,
    viewModel: SosViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MeshEmergencyScreen(
        state = state,
        onBack = onBack,
        onSendSos = { viewModel.sendSos() },
        onResetSos = { viewModel.resetSos() },
        onRefreshLocation = { viewModel.refreshLocation() },
        onToggleFlashlight = { viewModel.toggleFlashlight() },
        onToggleAlarm = { viewModel.toggleAlarm() },
        onDismissError = { viewModel.dismissError() }
    )
}
