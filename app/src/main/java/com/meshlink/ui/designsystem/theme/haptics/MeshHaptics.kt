package com.meshlink.ui.designsystem.theme.haptics

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic Engine for Mesh Link 2026.
 * Provides distinct tactile sensations for interactive user actions.
 */
class MeshHaptics(private val view: android.view.View) {
    fun selection() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun buttonPress() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun toggle() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun success() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun warning() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun error() {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun sosTrigger() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
fun rememberMeshHaptics(): MeshHaptics {
    val view = LocalView.current
    return remember(view) { MeshHaptics(view) }
}
