package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Data structures and state holders for the constellation animation particles & waves:
 * 1. Data Packets (White/silver light pulses traveling across active edges)
 * 2. Constellation Ripple Waves (Subtle energy ripples across formed text)
 */

class DataPacket(
    val id: Int,
    var fromNodeId: Int,
    var toNodeId: Int,
    var progress: Float = 0f,
    val speed: Float,
    val color: Color = AnimationConstants.StarlightWhite,
    val sizeDp: Float = 2.0f,
    val delayProgress: Float = 0.2f
) {
    var currentX = 0f
    var currentY = 0f
    var isActive = false

    fun reset() {
        progress = 0f
        isActive = false
    }
}

class ConstellationRippleWave(
    var centerX: Float = 0.5f,
    var centerY: Float = 0.45f,
    var radius: Float = 0f,
    var alpha: Float = 0f,
    var isActive: Boolean = false
) {
    fun trigger(x: Float, y: Float) {
        centerX = x
        centerY = y
        radius = 0f
        alpha = 0.8f
        isActive = true
    }

    fun update(deltaSec: Float) {
        if (!isActive) return
        radius += deltaSec * 0.4f
        alpha -= deltaSec * 0.5f
        if (alpha <= 0f || radius > 1.2f) {
            isActive = false
            alpha = 0f
        }
    }
}
