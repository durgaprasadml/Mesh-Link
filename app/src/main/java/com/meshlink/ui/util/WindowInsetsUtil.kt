package com.meshlink.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable Window Insets Helper Modifiers for Mesh-Link 2026.
 * Standardizes status bar, navigation bar, keyboard (IME), safe content, and safe drawing padding helpers.
 */
object WindowInsetsUtil {
    fun Modifier.meshStatusBarsPadding(): Modifier = this.statusBarsPadding()
    fun Modifier.meshNavigationBarsPadding(): Modifier = this.navigationBarsPadding()
    fun Modifier.meshImePadding(): Modifier = this.imePadding()
    fun Modifier.meshSafeDrawingPadding(): Modifier = this.safeDrawingPadding()
    fun Modifier.meshSafeContentPadding(): Modifier = this.safeContentPadding()

    @Composable
    fun Modifier.meshConsumeSafeDrawing(): Modifier {
        return this.consumeWindowInsets(WindowInsets.safeDrawing)
    }

    @Composable
    fun Modifier.meshConsumeInsets(insets: WindowInsets): Modifier {
        return this.consumeWindowInsets(insets)
    }
}

// Global Extension Functions for convenience
fun Modifier.meshStatusBarsPadding(): Modifier = this.statusBarsPadding()
fun Modifier.meshNavigationBarsPadding(): Modifier = this.navigationBarsPadding()
fun Modifier.meshImePadding(): Modifier = this.imePadding()
fun Modifier.meshSafeDrawingPadding(): Modifier = this.safeDrawingPadding()
fun Modifier.meshSafeContentPadding(): Modifier = this.safeContentPadding()


