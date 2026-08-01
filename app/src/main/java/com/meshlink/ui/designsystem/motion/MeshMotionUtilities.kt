package com.meshlink.ui.designsystem.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Reusable Duration Presets for Mesh-Link 2026.
 */
@Immutable
object MeshDurationPresets {
    const val Instant = 0
    const val Fast = 100
    const val Short1 = 100
    const val Short2 = 150
    const val Medium1 = 200
    const val Medium2 = 250
    const val Long1 = 300
    const val Long2 = 400
    const val Extended = 600
    const val Radar = 2000
    const val Signal = 1200
    const val Beacon = 800
}

/**
 * Reusable Easing Presets for Mesh-Link 2026.
 */
@Immutable
object MeshEasingPresets {
    val Emphasized: Easing = FastOutSlowInEasing
    val Decelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val Accelerate: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Linear: Easing = LinearEasing
    val Overshoot: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    val Dynamic: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val Anticipate: Easing = CubicBezierEasing(0.36f, 0.0f, 0.66f, -0.56f)
}

/**
 * Reusable Spring Presets for Mesh-Link 2026.
 */
@Immutable
object MeshSpringPresets {
    val Tactile: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val Responsive: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val Bouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val ExtraBouncy: SpringSpec<Float> = spring(
        dampingRatio = 0.45f,
        stiffness = Spring.StiffnessLow
    )

    val Soft: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val Snappy: SpringSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = 800f
    )

    val Heavy: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow
    )

    val Smooth: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val Rigid: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

}

/**
 * High-Level Motion Presets combining durations, easings, and spring dynamics.
 */
@Immutable
object MeshMotionPresets {
    @Stable
    val ScreenEnter: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Medium2,
        easing = MeshEasingPresets.Decelerate
    )

    @Stable
    val ScreenExit: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Medium1,
        easing = MeshEasingPresets.Accelerate
    )

    @Stable
    val DetailEnter: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Long1,
        easing = MeshEasingPresets.Emphasized
    )

    @Stable
    val DetailExit: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Medium2,
        easing = MeshEasingPresets.Accelerate
    )

    @Stable
    val ModalEnter: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Long1,
        easing = MeshEasingPresets.Decelerate
    )

    @Stable
    val ModalExit: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Medium2,
        easing = MeshEasingPresets.Accelerate
    )

    @Stable
    val SheetEnter: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Long1,
        easing = MeshEasingPresets.Decelerate
    )

    @Stable
    val SheetExit: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Medium2,
        easing = MeshEasingPresets.Accelerate
    )

    @Stable
    val SharedAxis: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Long1,
        easing = MeshEasingPresets.Emphasized
    )

    @Stable
    val HeroTransform: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Long2,
        easing = MeshEasingPresets.Emphasized
    )

    @Stable
    val ContainerTransform: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Long1,
        easing = MeshEasingPresets.Emphasized
    )

    @Stable
    val CardPress: SpringSpec<Float> = MeshSpringPresets.Tactile

    @Stable
    val ButtonPress: SpringSpec<Float> = MeshSpringPresets.Tactile

    @Stable
    val ListStagger: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Medium1,
        easing = MeshEasingPresets.Decelerate
    )

    @Stable
    val FabMotion: SpringSpec<Float> = MeshSpringPresets.Bouncy

    @Stable
    val Floating: TweenSpec<Float> = tween(
        durationMillis = 2000,
        easing = MeshEasingPresets.Emphasized
    )

    @Stable
    val Breathing: TweenSpec<Float> = tween(
        durationMillis = 1800,
        easing = MeshEasingPresets.Emphasized
    )

    @Stable
    val Pulse: TweenSpec<Float> = tween(
        durationMillis = MeshDurationPresets.Signal,
        easing = MeshEasingPresets.Emphasized
    )

    @Stable
    val GlowPulse: TweenSpec<Float> = tween(
        durationMillis = 1500,
        easing = MeshEasingPresets.Emphasized
    )
}
