package com.meshlink.ui.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.meshlink.ui.designsystem.theme.motion.MeshMotion

/**
 * Reusable Navigation Motion Framework for Mesh-Link 2026.
 * Standardizes Forward, Back, Shared Axis (X/Y/Z), Fade Through, Scale, Slide, and Container Transform transitions.
 */
@Immutable
object MeshNavigationMotion {

    // ── Forward Navigation ──
    val ForwardEnter: EnterTransition =
        slideInHorizontally(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullWidth -> (fullWidth * 0.12f).toInt() } +
                fadeIn(animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized))

    val ForwardExit: ExitTransition =
        fadeOut(animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate))

    // ── Back Navigation ──
    val BackEnter: EnterTransition =
        slideInHorizontally(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullWidth -> -(fullWidth * 0.12f).toInt() } +
                fadeIn(animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized))

    val BackExit: ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullWidth -> (fullWidth * 0.12f).toInt() } +
                fadeOut(animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate))

    // ── Shared Axis X (Horizontal) ──
    val SharedAxisXEnter: EnterTransition =
        slideInHorizontally(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullWidth -> (fullWidth * 0.3f).toInt() } +
                fadeIn(animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Decelerate))

    val SharedAxisXExit: ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullWidth -> -(fullWidth * 0.3f).toInt() } +
                fadeOut(animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate))

    // ── Shared Axis Y (Vertical) ──
    val SharedAxisYEnter: EnterTransition =
        slideInVertically(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullHeight -> (fullHeight * 0.3f).toInt() } +
                fadeIn(animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Decelerate))

    val SharedAxisYExit: ExitTransition =
        slideOutVertically(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullHeight -> -(fullHeight * 0.3f).toInt() } +
                fadeOut(animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate))

    // ── Shared Axis Z (Depth / Zoom) ──
    val SharedAxisZEnter: EnterTransition =
        scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) + fadeIn(animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Decelerate))

    val SharedAxisZExit: ExitTransition =
        scaleOut(
            targetScale = 1.1f,
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) + fadeOut(animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate))

    // ── Fade Through ──
    val FadeThroughEnter: EnterTransition =
        fadeIn(animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Decelerate)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Decelerate))

    val FadeThroughExit: ExitTransition =
        fadeOut(animationSpec = tween(MeshDurationPresets.Short2, easing = MeshEasingPresets.Accelerate))

    // ── Scale ──
    val ScaleEnter: EnterTransition =
        scaleIn(
            initialScale = 0.85f,
            animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Overshoot)
        ) + fadeIn(animationSpec = tween(MeshDurationPresets.Medium2))

    val ScaleExit: ExitTransition =
        scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(MeshDurationPresets.Short2, easing = MeshEasingPresets.Accelerate)
        ) + fadeOut(animationSpec = tween(MeshDurationPresets.Short2))

    // ── Slide ──
    val SlideInUp: EnterTransition =
        slideInVertically(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullHeight -> fullHeight } + fadeIn(animationSpec = tween(MeshDurationPresets.Long1))

    val SlideOutDown: ExitTransition =
        slideOutVertically(
            animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate)
        ) { fullHeight -> fullHeight } + fadeOut(animationSpec = tween(MeshDurationPresets.Medium2))

    val SlideInLeft: EnterTransition =
        slideInHorizontally(
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) { fullWidth -> -fullWidth } + fadeIn(animationSpec = tween(MeshDurationPresets.Long1))

    val SlideOutRight: ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate)
        ) { fullWidth -> fullWidth } + fadeOut(animationSpec = tween(MeshDurationPresets.Medium2))

    // ── Container Transform ──
    val ContainerTransformEnter: EnterTransition =
        scaleIn(
            initialScale = 0.75f,
            animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Emphasized)
        ) + fadeIn(animationSpec = tween(MeshDurationPresets.Long1, easing = MeshEasingPresets.Decelerate))

    val ContainerTransformExit: ExitTransition =
        scaleOut(
            targetScale = 0.75f,
            animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate)
        ) + fadeOut(animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Accelerate))
}
