package com.meshlink.ui.designsystem.theme.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion System Specifications & Material Motion Transitions for Mesh-Link.
 * Standardizes durations, spring specs, and Material 3 transitions (Fade Through, Shared Axis, Container Transform).
 * Respects Android System Reduced Motion preferences.
 */
@Immutable
object MeshMotion {
    const val DURATION_SHORT_1 = 100
    const val DURATION_SHORT_2 = 150
    const val DURATION_MEDIUM_1 = 200
    const val DURATION_MEDIUM_2 = 250
    const val DURATION_LONG_1 = 300
    const val DURATION_LONG_2 = 400
    const val DURATION_RADAR_SCAN = 2000
    const val DURATION_SIGNAL_PULSE = 1200
    const val DURATION_EMERGENCY_BEACON = 800

    val EmphasizedEasing: Easing = FastOutSlowInEasing
    val DecelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val AccelerateEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val Linear: Easing = LinearEasing

    val ResponsiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val TactileButtonSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val ScreenEnter = tween<Float>(DURATION_MEDIUM_2, easing = DecelerateEasing)
    val ScreenExit = tween<Float>(DURATION_MEDIUM_1, easing = AccelerateEasing)
    val SharedAxis = tween<Float>(DURATION_LONG_1, easing = EmphasizedEasing)
    val HeroMotion = tween<Float>(DURATION_LONG_2, easing = EmphasizedEasing)
    val ContainerTransform = tween<Float>(DURATION_LONG_1, easing = EmphasizedEasing)
    val FabMotion = tween<Float>(DURATION_MEDIUM_2, easing = EmphasizedEasing)
    val CardPressMotion = tween<Float>(DURATION_SHORT_1, easing = DecelerateEasing)
    val ListStagger = tween<Float>(DURATION_MEDIUM_1, easing = DecelerateEasing)
    val ButtonPress = tween<Float>(DURATION_SHORT_1, easing = DecelerateEasing)
    val LoadingSpin = tween<Float>(1000, easing = LinearEasing)
    val SuccessPulse = tween<Float>(DURATION_MEDIUM_2, easing = DecelerateEasing)
    val ErrorShake = tween<Float>(DURATION_SHORT_2, easing = DecelerateEasing)
    val BroadcastPulse = tween<Float>(DURATION_SIGNAL_PULSE, easing = DecelerateEasing)
    val RadarScan = tween<Float>(DURATION_RADAR_SCAN, easing = LinearEasing)
    val NodeDiscovery = tween<Float>(DURATION_MEDIUM_2, easing = DecelerateEasing)
    val SignalPulse = tween<Float>(DURATION_SIGNAL_PULSE, easing = LinearEasing)
    val SosAlertPulse = tween<Float>(DURATION_EMERGENCY_BEACON, easing = DecelerateEasing)
    val EmergencyPulse = tween<Float>(DURATION_EMERGENCY_BEACON, easing = DecelerateEasing)
    val SearchExpansion = tween<Float>(DURATION_LONG_1, easing = EmphasizedEasing)
    val BottomSheetMotion = tween<Float>(DURATION_LONG_1, easing = DecelerateEasing)
    val DialogMotion = tween<Float>(DURATION_MEDIUM_2, easing = DecelerateEasing)
    val NavigationMotion = tween<Float>(DURATION_MEDIUM_2, easing = EmphasizedEasing)

    val Fast = tween<Float>(DURATION_SHORT_2, easing = DecelerateEasing)
    val Medium = tween<Float>(DURATION_MEDIUM_2, easing = EmphasizedEasing)
    val Slow = tween<Float>(DURATION_LONG_1, easing = EmphasizedEasing)
    val FadeThrough = tween<Float>(DURATION_MEDIUM_1, easing = DecelerateEasing)

    // ── Material 3 Transition Helpers ──

    fun fadeThroughEnter(isReducedMotion: Boolean = false): EnterTransition {
        return if (isReducedMotion) fadeIn(animationSpec = tween(0))
        else fadeIn(animationSpec = tween(DURATION_MEDIUM_2, easing = DecelerateEasing)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(DURATION_MEDIUM_2, easing = DecelerateEasing))
    }

    fun fadeThroughExit(isReducedMotion: Boolean = false): ExitTransition {
        return if (isReducedMotion) fadeOut(animationSpec = tween(0))
        else fadeOut(animationSpec = tween(DURATION_SHORT_2, easing = AccelerateEasing))
    }

    fun sharedAxisXEnter(forward: Boolean = true, isReducedMotion: Boolean = false): EnterTransition {
        return if (isReducedMotion) fadeIn(animationSpec = tween(0))
        else slideInHorizontally(
            initialOffsetX = { fullWidth -> if (forward) fullWidth / 3 else -fullWidth / 3 },
            animationSpec = tween(DURATION_LONG_1, easing = EmphasizedEasing)
        ) + fadeIn(animationSpec = tween(DURATION_LONG_1, easing = Linear))
    }

    fun sharedAxisXExit(forward: Boolean = true, isReducedMotion: Boolean = false): ExitTransition {
        return if (isReducedMotion) fadeOut(animationSpec = tween(0))
        else slideOutHorizontally(
            targetOffsetX = { fullWidth -> if (forward) -fullWidth / 3 else fullWidth / 3 },
            animationSpec = tween(DURATION_LONG_1, easing = EmphasizedEasing)
        ) + fadeOut(animationSpec = tween(DURATION_LONG_1, easing = Linear))
    }

    fun sharedAxisYEnter(forward: Boolean = true, isReducedMotion: Boolean = false): EnterTransition {
        return if (isReducedMotion) fadeIn(animationSpec = tween(0))
        else slideInVertically(
            initialOffsetY = { fullHeight -> if (forward) fullHeight / 3 else -fullHeight / 3 },
            animationSpec = tween(DURATION_LONG_1, easing = EmphasizedEasing)
        ) + fadeIn(animationSpec = tween(DURATION_LONG_1, easing = Linear))
    }

    fun sharedAxisYExit(forward: Boolean = true, isReducedMotion: Boolean = false): ExitTransition {
        return if (isReducedMotion) fadeOut(animationSpec = tween(0))
        else slideOutVertically(
            targetOffsetY = { fullHeight -> if (forward) -fullHeight / 3 else fullHeight / 3 },
            animationSpec = tween(DURATION_LONG_1, easing = EmphasizedEasing)
        ) + fadeOut(animationSpec = tween(DURATION_LONG_1, easing = Linear))
    }

    fun sharedAxisZEnter(forward: Boolean = true, isReducedMotion: Boolean = false): EnterTransition {
        return if (isReducedMotion) fadeIn(animationSpec = tween(0))
        else scaleIn(
            initialScale = if (forward) 0.8f else 1.1f,
            animationSpec = tween(DURATION_LONG_1, easing = EmphasizedEasing)
        ) + fadeIn(animationSpec = tween(DURATION_LONG_1, easing = Linear))
    }

    fun sharedAxisZExit(forward: Boolean = true, isReducedMotion: Boolean = false): ExitTransition {
        return if (isReducedMotion) fadeOut(animationSpec = tween(0))
        else scaleOut(
            targetScale = if (forward) 1.1f else 0.8f,
            animationSpec = tween(DURATION_LONG_1, easing = EmphasizedEasing)
        ) + fadeOut(animationSpec = tween(DURATION_LONG_1, easing = Linear))
    }
}

val LocalMeshMotion = staticCompositionLocalOf { MeshMotion }
