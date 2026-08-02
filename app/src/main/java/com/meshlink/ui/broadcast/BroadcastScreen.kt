package com.meshlink.ui.broadcast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Public entry-point bridge screen for Broadcast & Group Communication.
 * Strictly collects ViewModel state and forwards interactions to [MeshBroadcastScreen].
 */
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val peerIdentities by viewModel.peerIdentities.collectAsStateWithLifecycle()

    MeshBroadcastScreen(
        messages = uiState.messages,
        peerIdentities = peerIdentities,
        onBack = onBack,
        onSendBroadcast = { messageText ->
            viewModel.sendBroadcast(messageText)
        }
    )
}
