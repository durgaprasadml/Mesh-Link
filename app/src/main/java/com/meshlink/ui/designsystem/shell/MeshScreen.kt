package com.meshlink.ui.designsystem.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Universal Screen Container for Mesh-Link 2026.
 * Binds background theme, top bar container, inset propagation, loading slot, empty state slot, and accessibility parameters.
 * Eliminates nested scaffold and duplicate inset padding calculations across individual screen implementations.
 */
@Composable
fun MeshScreen(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    isEmpty: Boolean = false,
    loadingSlot: (@Composable () -> Unit)? = null,
    emptyStateSlot: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MeshTheme.colors.background,
    content: @Composable ColumnScope.() -> Unit
) {
    val insets = LocalMeshWindowInsetsManager.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.cutoutLeft,
                    end = insets.cutoutRight
                )
        ) {
            if (topBar != null) {
                topBar()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        if (loadingSlot != null) {
                            loadingSlot()
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MeshTheme.colors.primary)
                            }
                        }
                    }
                    isEmpty -> {
                        if (emptyStateSlot != null) {
                            emptyStateSlot()
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            content = content
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overload for screens utilizing Scaffold slots and PaddingValues propagation.
 */
@Composable
fun MeshScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MeshTheme.colors.background,
    contentWindowInsets: WindowInsets = WindowInsets(0.dp),
    content: @Composable (PaddingValues) -> Unit
) {
    val insets = LocalMeshWindowInsetsManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.cutoutLeft,
                    end = insets.cutoutRight
                )
        ) {
            content(paddingValues)
        }
    }
}

