package com.meshlink.ui.designsystem.overlay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Unified Material 3 Toast & Snackbar Host for Mesh-Link 2026.
 * Hosts standardized semantic snackbar toasts across all screens.
 */
@Composable
fun MeshSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) { snackbarData ->
        MeshSnackbarItem(snackbarData)
    }
}

@Composable
fun MeshSnackbarItem(snackbarData: SnackbarData) {
    val actionLabel = snackbarData.visuals.actionLabel
    MeshSnackbar(
        message = snackbarData.visuals.message,
        actionLabel = actionLabel,
        onActionClick = if (actionLabel != null) { { snackbarData.performAction() } } else null,
        modifier = Modifier.fillMaxWidth()
    )
}

