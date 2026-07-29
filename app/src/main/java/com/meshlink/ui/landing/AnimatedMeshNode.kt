package com.meshlink.ui.landing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Mutable state object representing a single node in the animated mesh network.
 * Pre-allocated to avoid garbage collection pressure during frame rendering.
 */
class AnimatedMeshNode(
    val id: Int,
    val xRatio: Float,
    val yRatio: Float,
    val targetXRatio: Float = xRatio,
    val targetYRatio: Float = yRatio,
    val radiusDp: Float,
    val glowColor: Color,
    val pulsePhase: Float,
    val pulseSpeed: Float,
    val floatNoiseOffsetX: Float,
    val floatNoiseOffsetY: Float,
    val appearDelay: Float,
    val haloRingCount: Int = 2,
    val isUserNode: Boolean = false
) {
    // Computed screen pixel coordinates (updated during layout/physics tick)
    var currentX by mutableFloatStateOf(0f)
    var currentY by mutableFloatStateOf(0f)

    // Dynamic animation parameters
    var alpha by mutableFloatStateOf(0f)
    var scale by mutableFloatStateOf(1f)
    var breathingOffset by mutableFloatStateOf(0f)
    var radarWaveProgress by mutableFloatStateOf(0f)
    var isRadarActive by mutableStateOf(false)
    var brightness by mutableFloatStateOf(1f)

    fun reset() {
        alpha = 0f
        scale = 1f
        breathingOffset = 0f
        radarWaveProgress = 0f
        isRadarActive = false
        brightness = 1f
    }
}
