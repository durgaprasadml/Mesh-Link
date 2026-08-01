package com.meshlink.ui.designsystem.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.navigation.MeshAdaptiveNavigationRail
import com.meshlink.ui.designsystem.navigation.MeshNavigationDock
import com.meshlink.ui.designsystem.overlay.MeshSnackbarHost
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Root Application Scaffold for Mesh-Link 2026.
 * Manages adaptive layout shell, floating navigation dock, navigation rail, global overlays, and window insets.
 */
@Composable
fun MeshScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    navItems: List<com.meshlink.ui.designsystem.navigation.MeshNavItem>? = null,
    snackbarHostState: SnackbarHostState? = null,
    statusBanner: (@Composable () -> Unit)? = null,
    searchOverlay: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isTopLevelScreen = currentRoute in listOf("home", "nearby", "sos", "settings")
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val showNavigationDock = isCompact && isTopLevelScreen
    val showNavigationRail = !isCompact && isTopLevelScreen

    ProvideMeshWindowInsetsManager {
        val insets = LocalMeshWindowInsetsManager.current

        Surface(
            modifier = modifier.fillMaxSize(),
            color = MeshTheme.colors.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showNavigationRail) {
                        MeshAdaptiveNavigationRail(
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                            items = navItems ?: com.meshlink.ui.designsystem.navigation.defaultMeshNavItems
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        if (statusBanner != null) {
                            statusBanner()
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            content()
                        }
                    }
                }

                // Floating Navigation Dock for Compact Screen Viewports
                if (showNavigationDock && !insets.isImeVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = insets.navigationBarHeight)
                    ) {
                        MeshNavigationDock(
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                            items = navItems ?: com.meshlink.ui.designsystem.navigation.defaultMeshNavItems
                        )
                    }
                }

                // FAB Placement
                if (floatingActionButton != null) {
                    val bottomFabPadding = when {
                        insets.isImeVisible -> insets.imeHeight + 16.dp
                        showNavigationDock -> 96.dp + insets.navigationBarHeight
                        else -> 24.dp + insets.navigationBarHeight
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                bottom = bottomFabPadding,
                                end = 20.dp
                            )
                    ) {
                        floatingActionButton()
                    }
                }

                // Global Snackbar Host
                if (snackbarHostState != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = insets.statusBarHeight + 8.dp)
                    ) {
                        MeshSnackbarHost(hostState = snackbarHostState)
                    }
                }

                // Global Search Overlay Host
                if (searchOverlay != null) {
                    searchOverlay()
                }
            }
        }
    }
}
