package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Data structures and state holders for the 3-tier particle systems:
 * 1. Ambient Dust Particles (Background atmosphere)
 * 2. Discovery Sparks (Emitted upon link formation)
 * 3. Data Packets (Traveling along network edges)
 */

class AmbientDustParticle(
    val xRatio: Float,
    val yRatio: Float,
    val sizeDp: Float,
    val speedX: Float,
    val speedY: Float,
    val baseAlpha: Float
) {
    var currentX = 0f
    var currentY = 0f
    var currentAlpha = baseAlpha
}

class DiscoverySpark(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f, // 1.0 -> 0.0
    var color: Color = AnimationConstants.Cyan,
    var size: Float = 3f,
    var active: Boolean = false
) {
    fun spawn(startX: Float, startY: Float, sparkColor: Color) {
        x = startX
        y = startY
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val speed = 1.5f + Random.nextFloat() * 3.5f
        vx = kotlin.math.cos(angle.toDouble()).toFloat() * speed
        vy = kotlin.math.sin(angle.toDouble()).toFloat() * speed
        life = 1.0f
        color = sparkColor
        size = 2f + Random.nextFloat() * 4f
        active = true
    }

    fun update(delta: Float) {
        if (!active) return
        x += vx
        y += vy
        vx *= 0.95f
        vy *= 0.95f
        life -= delta * 2.2f
        if (life <= 0f) {
            active = false
        }
    }
}

class DataPacket(
    val id: Int,
    var fromNodeId: Int,
    var toNodeId: Int,
    var progress: Float = 0f,
    val speed: Float,
    val color: Color,
    val sizeDp: Float,
    val delayProgress: Float
) {
    var currentX = 0f
    var currentY = 0f
    var isActive = false
    var isRelaying = false
    var relayPauseTimer = 0f

    fun reset() {
        progress = 0f
        isActive = false
        isRelaying = false
        relayPauseTimer = 0f
    }
}
