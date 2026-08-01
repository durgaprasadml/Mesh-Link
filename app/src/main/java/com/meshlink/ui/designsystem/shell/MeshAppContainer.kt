package com.meshlink.ui.designsystem.shell

import androidx.compose.runtime.Composable
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Root Application Container for Mesh-Link 2026.
 * Encloses the entire application shell with design system tokens and window inset providers.
 */

@Composable
fun MeshAppContainer(
    content: @Composable () -> Unit
) {
    MeshTheme {
        ProvideMeshWindowInsetsManager {
            content()
        }
    }
}

