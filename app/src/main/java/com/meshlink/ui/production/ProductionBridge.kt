package com.meshlink.ui.production

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Presentation Bridge Adapters for Mesh-Link Phase 15.
 * Provides unified wrappers bridging legacy and Phase 1-14 UI screens to Phase 15 production UI helpers
 * without modifying any ViewModels, data models, or navigation logic.
 */

@Composable
fun MeshProductionScreenBridge(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isEmpty: Boolean = false,
    emptyCategory: MeshEmptyStateCategory = MeshEmptyStateCategory.CHATS,
    onRetry: (() -> Unit)? = null,
    onEmptyAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                MeshLoadingOverlay(message = "Loading Mesh Data...")
            }
            errorMessage != null -> {
                MeshErrorCard(
                    title = "Operation Failed",
                    message = errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isEmpty -> {
                MeshCategoryEmptyState(
                    category = emptyCategory,
                    onPrimaryAction = onEmptyAction
                )
            }
            else -> {
                content()
            }
        }
    }
}
