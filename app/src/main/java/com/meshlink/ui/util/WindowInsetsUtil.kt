package com.meshlink.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable Window Insets Helper Modifiers for Mesh-Link 2026.
 * Standardizes status bar, navigation bar, keyboard (IME), safe content, display cutout,
 * system gesture, and safe drawing padding helpers across all Android form factors.
 */
object WindowInsetsUtil {
    fun Modifier.meshStatusBarsPadding(): Modifier = this.statusBarsPadding()
    fun Modifier.meshNavigationBarsPadding(): Modifier = this.navigationBarsPadding()
    fun Modifier.meshImePadding(): Modifier = this.imePadding()
    fun Modifier.meshDisplayCutoutPadding(): Modifier = this.displayCutoutPadding()
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
fun Modifier.meshDisplayCutoutPadding(): Modifier = this.displayCutoutPadding()
fun Modifier.meshSafeDrawingPadding(): Modifier = this.safeDrawingPadding()
fun Modifier.meshSafeContentPadding(): Modifier = this.safeContentPadding()
