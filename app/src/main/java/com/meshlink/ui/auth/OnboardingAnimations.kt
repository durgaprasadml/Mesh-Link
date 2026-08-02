package com.meshlink.ui.auth

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

/**
 * CompositionLocal to detect system Reduced Motion settings for accessibility.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Provides system reduced motion settings to composable hierarchy.
 */
@Composable
fun ProvideReducedMotion(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        try {
            val animatorScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            animatorScale == 0.0f
        } catch (_: Exception) {
            false
        }
    }
    CompositionLocalProvider(LocalReducedMotion provides isReducedMotion) {
        content()
    }
}

/**
 * Central animation specifications for Mesh-Link Onboarding.
 */
object OnboardingAnimations {
    val EmphasizedEasing: Easing = FastOutSlowInEasing
    val DecelerateEasing: Easing = LinearOutSlowInEasing

    // Durations
    const val ShortDurationMs = 250
    const val MediumDurationMs = 400
    const val LongDurationMs = 600

    /**
     * Shared Axis Horizontal enter transition (Forward motion).
     */
    fun sharedAxisXForwardEnter(reducedMotion: Boolean = false): EnterTransition {
        if (reducedMotion) return fadeIn(tween(ShortDurationMs))
        return fadeIn(tween(MediumDurationMs, easing = EmphasizedEasing)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(MediumDurationMs, easing = EmphasizedEasing))
    }

    /**
     * Shared Axis Horizontal exit transition (Forward motion).
     */
    fun sharedAxisXForwardExit(reducedMotion: Boolean = false): ExitTransition {
        if (reducedMotion) return fadeOut(tween(ShortDurationMs))
        return fadeOut(tween(MediumDurationMs, easing = EmphasizedEasing)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(MediumDurationMs, easing = EmphasizedEasing))
    }

    /**
     * Shared Axis Horizontal enter transition (Backward motion).
     */
    fun sharedAxisXBackwardEnter(reducedMotion: Boolean = false): EnterTransition {
        if (reducedMotion) return fadeIn(tween(ShortDurationMs))
        return fadeIn(tween(MediumDurationMs, easing = EmphasizedEasing)) +
                scaleIn(initialScale = 1.08f, animationSpec = tween(MediumDurationMs, easing = EmphasizedEasing))
    }

    /**
     * Shared Axis Horizontal exit transition (Backward motion).
     */
    fun sharedAxisXBackwardExit(reducedMotion: Boolean = false): ExitTransition {
        if (reducedMotion) return fadeOut(tween(ShortDurationMs))
        return fadeOut(tween(MediumDurationMs, easing = EmphasizedEasing)) +
                scaleOut(targetScale = 1.08f, animationSpec = tween(MediumDurationMs, easing = EmphasizedEasing))
    }

    /**
     * Fade Through transition (used for screen changes with no directional relationship).
     */
    fun fadeThrough(reducedMotion: Boolean = false): ContentTransform {
        if (reducedMotion) {
            return fadeIn(tween(ShortDurationMs)) togetherWith fadeOut(tween(ShortDurationMs))
        }
        return (fadeIn(tween(MediumDurationMs, easing = EmphasizedEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(MediumDurationMs, easing = EmphasizedEasing))) togetherWith
                (fadeOut(tween(ShortDurationMs, easing = EmphasizedEasing)))
    }

    /**
     * Bouncy spring animation spec for avatar pop & checkmark success animations.
     */
    fun <T> bouncySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Gentle spring for cards & sheet previews.
     */
    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
