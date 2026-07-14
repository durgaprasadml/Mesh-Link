package com.meshlink.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class HapticManager(private val hapticFeedback: HapticFeedback) {

    fun performLightClick() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performHeavyClick() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performSuccess() {
        // Simulating success by a sequence could be complex in pure Compose Haptics,
        // so we use a heavy click as a fallback or standard feedback.
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performError() {
        // Fallback for error
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberHapticManager(): HapticManager {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        HapticManager(hapticFeedback)
    }
}
