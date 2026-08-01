package com.meshlink.ui.designsystem.shell

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Convenience layout modifier extensions using MeshWindowInsetsManager.
 */

@Composable
fun Modifier.meshWindowInsetsPadding(
    includeStatusBar: Boolean = true,
    includeNavigationBar: Boolean = true,
    includeIme: Boolean = true
): Modifier {
    val insets = LocalMeshWindowInsetsManager.current
    val topPadding = if (includeStatusBar) insets.statusBarHeight else 0.dp
    val bottomPadding = when {
        includeIme && insets.isImeVisible -> insets.imeHeight
        includeNavigationBar -> insets.navigationBarHeight
        else -> 0.dp
    }
    return this.padding(
        top = topPadding,
        bottom = bottomPadding,
        start = insets.cutoutLeft,
        end = insets.cutoutRight
    )
}

@Composable
fun Modifier.meshImePadding(): Modifier {
    val insets = LocalMeshWindowInsetsManager.current
    return if (insets.isImeVisible) {
        this.padding(bottom = insets.imeHeight)
    } else {
        this.imePadding()
    }
}

