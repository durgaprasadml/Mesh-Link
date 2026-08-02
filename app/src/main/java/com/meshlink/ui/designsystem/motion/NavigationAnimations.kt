package com.meshlink.ui.designsystem.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Material Motion Navigation Animations for Mesh-Link 2026.
 * Implements Fade Through, Shared Axis (X, Y, Z), Container Transform, and Reduced Motion support.
 */
object NavigationAnimations {

    // Motion Durations
    const val DurationFast = 150
    const val DurationMedium = 300
    const val DurationSlow = 500

    /**
     * Fade Through Transition for top-level navigation bar switching.
     */
    fun fadeThrough(duration: Int = DurationMedium): ContentTransform {
        return (fadeIn(animationSpec = tween(durationMillis = duration)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = duration)))
            .togetherWith(fadeOut(animationSpec = tween(durationMillis = duration)))
    }

    /**
     * Shared Axis X Transition for horizontal linear navigation flow.
     */
    fun sharedAxisX(forward: Boolean = true, duration: Int = DurationMedium): ContentTransform {
        val slideOffset = if (forward) 300 else -300
        return (slideInHorizontally(initialOffsetX = { slideOffset }, animationSpec = tween(durationMillis = duration)) +
                fadeIn(animationSpec = tween(durationMillis = duration)))
            .togetherWith(slideOutHorizontally(targetOffsetX = { -slideOffset }, animationSpec = tween(durationMillis = duration)) +
                    fadeOut(animationSpec = tween(durationMillis = duration)))
    }

    /**
     * Shared Axis Y Transition for vertical hierarchical flow.
     */
    fun sharedAxisY(forward: Boolean = true, duration: Int = DurationMedium): ContentTransform {
        val slideOffset = if (forward) 200 else -200
        return (slideInVertically(initialOffsetY = { slideOffset }, animationSpec = tween(durationMillis = duration)) +
                fadeIn(animationSpec = tween(durationMillis = duration)))
            .togetherWith(slideOutVertically(targetOffsetY = { -slideOffset }, animationSpec = tween(durationMillis = duration)) +
                    fadeOut(animationSpec = tween(durationMillis = duration)))
    }

    /**
     * Shared Axis Z Transition for parent-child depth navigation.
     */
    fun sharedAxisZ(forward: Boolean = true, duration: Int = DurationMedium): ContentTransform {
        val initialScale = if (forward) 0.8f else 1.1f
        val targetScale = if (forward) 1.1f else 0.8f
        return (scaleIn(initialScale = initialScale, animationSpec = tween(durationMillis = duration)) +
                fadeIn(animationSpec = tween(durationMillis = duration)))
            .togetherWith(scaleOut(targetScale = targetScale, animationSpec = tween(durationMillis = duration)) +
                    fadeOut(animationSpec = tween(durationMillis = duration)))
    }

    /**
     * Container Transform for card-to-detail or FAB expansion transitions.
     */
    fun containerTransform(duration: Int = DurationMedium): ContentTransform {
        return (scaleIn(initialScale = 0.85f, transformOrigin = TransformOrigin.Center, animationSpec = tween(durationMillis = duration)) +
                fadeIn(animationSpec = tween(durationMillis = duration)))
            .togetherWith(scaleOut(targetScale = 0.95f, transformOrigin = TransformOrigin.Center, animationSpec = tween(durationMillis = duration)) +
                    fadeOut(animationSpec = tween(durationMillis = duration)))
    }

    /**
     * Instant Transition for Reduced Motion accessibility preferences.
     */
    fun reducedMotion(): ContentTransform {
        return fadeIn(animationSpec = tween(0)).togetherWith(fadeOut(animationSpec = tween(0)))
    }
}
