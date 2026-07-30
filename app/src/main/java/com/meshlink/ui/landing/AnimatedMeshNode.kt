package com.meshlink.ui.landing

import kotlin.math.cos
import kotlin.math.sin

/**
 * Data model for a single node (out of 6) in the Mesh Link logo.
 */
data class MeshLogoNode(
    val id: Int,
    val isCenter: Boolean,
    val discoveryStage: Int,      // Stage 1 (Center) -> Stage 6 (Final outer node)
    val angleDeg: Float = 0f,     // 0° for center; outer angles (-126°, -54°, 18°, 90°, 162°)
    val twinklePhase: Float = (id * 1.37f),
    val twinkleSpeed: Float = 0.8f + (id % 3) * 0.35f,
    val breathingPhase: Float = (id * 0.95f)
) {
    /**
     * Compute actual screen pixel coordinates based on viewport center and radius.
     */
    fun computePosition(
        centerX: Float,
        centerY: Float,
        logoRadius: Float,
        outPos: FloatArray
    ) {
        if (isCenter) {
            outPos[0] = centerX
            outPos[1] = centerY
        } else {
            val rad = Math.toRadians(angleDeg.toDouble())
            outPos[0] = centerX + (logoRadius * cos(rad)).toFloat()
            outPos[1] = centerY + (logoRadius * sin(rad)).toFloat()
        }
    }
}

/**
 * Data model for a single light beam connection (out of 10) in the Mesh Link logo.
 */
data class MeshLogoBeam(
    val id: Int,
    val fromNodeId: Int,
    val toNodeId: Int,
    val discoveryStage: Int // Stage (2..6) when this beam travels
)

