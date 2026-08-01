package com.meshlink.ui.designsystem.theme.haptics

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic Engine for Mesh Link 2026.
 * Provides distinct tactile sensations for interactive user actions across navigation, messaging, and emergency alerts.
 */
class MeshHaptics(private val view: android.view.View) {

    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

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

    fun failure() {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun warning() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun error() {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun navigation() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun emergency() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun sosTrigger() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun broadcast() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun connection() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun messageSent() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun messageReceived() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun heavyClick() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun selectionClick() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun rememberMeshHaptics(): MeshHaptics {
    val view = LocalView.current
    return remember(view) { MeshHaptics(view) }
}
