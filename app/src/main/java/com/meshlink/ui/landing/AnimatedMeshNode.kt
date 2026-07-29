package com.meshlink.ui.landing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Pre-allocated star node object representing a star in the constellation universe.
 * Encapsulates independent twinkling parameters, wave discovery progress,
 * and procedural migration targets for constellation text formation.
 */
class AnimatedMeshNode(
    val id: Int,
    val startXRatio: Float,
    val startYRatio: Float,
    var targetXRatio: Float = startXRatio,
    var targetYRatio: Float = startYRatio,
    var radiusDp: Float = 2.5f,
    val glowColor: Color = AnimationConstants.StarlightSilver,
    val pulsePhase: Float = 0f,
    val pulseSpeed: Float = 1.0f,
    val twinklePhase: Float = 0f,
    val twinkleSpeed: Float = 1.0f,
    val baseBrightness: Float = 0.4f,
    val isMigrating: Boolean = false,
    val migrationOrder: Float = 0f,
    val isUserNode: Boolean = false
) {
    // Computed pixel coordinates on screen
    var currentX by mutableFloatStateOf(0f)
    var currentY by mutableFloatStateOf(0f)

    // Dynamic animation states
    var alpha by mutableFloatStateOf(0.3f)
    var currentBrightness by mutableFloatStateOf(baseBrightness)
    var isDiscovered by mutableStateOf(false)
    var discoveryProgress by mutableFloatStateOf(0f)
    var pulseIntensity by mutableFloatStateOf(0f)

    fun reset() {
        alpha = 0.3f
        currentBrightness = baseBrightness
        isDiscovered = false
        discoveryProgress = 0f
        pulseIntensity = 0f
    }
}
