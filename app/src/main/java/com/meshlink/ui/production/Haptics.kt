package com.meshlink.ui.production

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Reusable Haptic Feedback System for Mesh-Link Phase 15.
 * Centralizes haptic feedback helpers for Success, Warning, Error, Selection, Long Press, Send, and Receive.
 */

enum class MeshHapticPattern {
    SUCCESS,
    WARNING,
    ERROR,
    SELECTION,
    LONG_PRESS,
    SEND,
    RECEIVE
}

class MeshHaptics(
    private val hapticFeedback: HapticFeedback,
    private val context: Context
) {
    fun perform(pattern: MeshHapticPattern) {
        when (pattern) {
            MeshHapticPattern.SELECTION -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            MeshHapticPattern.LONG_PRESS -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            MeshHapticPattern.SUCCESS -> {
                vibratePattern(longArrayOf(0, 40, 60, 40), intArrayOf(0, 150, 0, 255))
            }
            MeshHapticPattern.WARNING -> {
                vibratePattern(longArrayOf(0, 80, 50, 80), intArrayOf(0, 200, 0, 200))
            }
            MeshHapticPattern.ERROR -> {
                vibratePattern(longArrayOf(0, 100, 40, 100, 40, 100), intArrayOf(0, 255, 0, 255, 0, 255))
            }
            MeshHapticPattern.SEND -> {
                vibratePattern(longArrayOf(0, 30), intArrayOf(0, 180))
            }
            MeshHapticPattern.RECEIVE -> {
                vibratePattern(longArrayOf(0, 50, 40, 50), intArrayOf(0, 220, 0, 220))
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                if (vibrator?.hasVibrator() == true) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                }
            }
        } catch (_: Throwable) {
            // Safe fallback if vibration hardware permission or feature is absent
        }
    }
}

@Composable
fun rememberMeshHaptics(): MeshHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current.applicationContext
    return remember(hapticFeedback, context) {
        MeshHaptics(hapticFeedback = hapticFeedback, context = context)
    }
}
