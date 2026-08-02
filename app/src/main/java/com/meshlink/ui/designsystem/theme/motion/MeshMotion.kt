package com.meshlink.ui.designsystem.theme.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion System Specifications for Mesh-Link 2026 Original Design System.
 */
@Immutable
object MeshMotion {
    // ── Durations ──
    const val DURATION_SHORT_1 = 100
    const val DURATION_SHORT_2 = 150
    const val DURATION_MEDIUM_1 = 200
    const val DURATION_MEDIUM_2 = 250
    const val DURATION_LONG_1 = 300
    const val DURATION_LONG_2 = 400
    const val DURATION_RADAR_SCAN = 2000
    const val DURATION_SIGNAL_PULSE = 1200
    const val DURATION_EMERGENCY_BEACON = 800

    // ── Easing Curves ──
    val EmphasizedEasing: Easing = FastOutSlowInEasing
    val DecelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val AccelerateEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val Linear: Easing = LinearEasing

    // ── Springs ──
    val ResponsiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val TactileButtonSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // ── Reusable Specifications ──
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

    // Standard duration & fade aliases
    val Fast = tween<Float>(DURATION_SHORT_2, easing = DecelerateEasing)
    val Medium = tween<Float>(DURATION_MEDIUM_2, easing = EmphasizedEasing)
    val Slow = tween<Float>(DURATION_LONG_1, easing = EmphasizedEasing)
    val FadeThrough = tween<Float>(DURATION_MEDIUM_1, easing = DecelerateEasing)
}

val LocalMeshMotion = staticCompositionLocalOf { MeshMotion }
