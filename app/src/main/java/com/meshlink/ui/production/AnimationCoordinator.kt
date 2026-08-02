package com.meshlink.ui.production

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Centralized Animation Timing Coordinator for Mesh-Link Phase 15.
 * Standardizes durations, spring parameters, easings, and shared transition timing curves.
 */

@Immutable
object MeshAnimationCoordinator {

    // Standardized Duration Tokens (in ms)
    const val DURATION_INSTANT = 0
    const val DURATION_FAST = 150
    const val DURATION_MEDIUM = 300
    const val DURATION_SLOW = 500
    const val DURATION_SHIMMER = 1200
    const val DURATION_RADAR_PULSE = 2000

    // Standardized Easings
    val StandardEasing: Easing = FastOutSlowInEasing
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Linear: Easing = LinearEasing

    // Standardized Spring Specifications
    fun <T> bouncySpring(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> smoothSpring(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> fastSpring(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // Standardized Tweens
    fun <T> fastTween(): AnimationSpec<T> = tween(
        durationMillis = DURATION_FAST,
        easing = StandardEasing
    )

    fun <T> mediumTween(): AnimationSpec<T> = tween(
        durationMillis = DURATION_MEDIUM,
        easing = EmphasizedEasing
    )
}
