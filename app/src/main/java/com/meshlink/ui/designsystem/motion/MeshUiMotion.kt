package com.meshlink.ui.designsystem.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Complete UI Motion Framework for Mesh-Link 2026.
 * High-performance, zero-recomposition animation wrappers using graphicsLayer and hardware accelerated Canvas draws.
 */

// ── Hero Animations ──

@Composable
fun MeshHeroContainer(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    content: @Composable () -> Unit
) {
    var scaleTarget by remember { mutableFloatStateOf(0.92f) }
    var alphaTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(active) {
        if (active) {
            scaleTarget = 1.0f
            alphaTarget = 1.0f
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = MeshMotionPresets.HeroTransform,
        label = "HeroScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = alphaTarget,
        animationSpec = MeshMotionPresets.HeroTransform,
        label = "HeroAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        }
    ) {
        content()
    }
}

fun Modifier.meshHeroAnimation(
    visible: Boolean = true,
    scaleFrom: Float = 0.9f
): Modifier = this.graphicsLayer {
    scaleX = if (visible) 1.0f else scaleFrom
    scaleY = if (visible) 1.0f else scaleFrom
    alpha = if (visible) 1.0f else 0.0f
}

// ── Pulse Engine ──

@Composable
fun MeshPulseEngine(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    pulseColor: Color = Color(0xFF00F59B),
    maxPulseScale: Float = 1.4f,
    content: @Composable () -> Unit
) {
    if (!active) {
        Box(modifier = modifier) { content() }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "MeshPulseEngineTransition")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MeshDurationPresets.Signal, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val maxRadius = (this.size.minDimension / 2f) * maxPulseScale
            val currentRadius = (this.size.minDimension / 2f) + (maxRadius - (this.size.minDimension / 2f)) * pulseProgress
            val currentAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.45f

            drawCircle(
                color = pulseColor.copy(alpha = currentAlpha),
                radius = currentRadius
            )
        }
        content()
    }
}

fun Modifier.meshPulse(
    color: Color = Color(0xFF00F59B),
    enabled: Boolean = true
): Modifier = if (!enabled) this else this.drawBehind {
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = size.minDimension * 0.6f
    )
}

// ── Glow Engine ──

@Composable
fun MeshGlowEngine(
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF00F59B),
    glowRadius: Dp = 20.dp,
    active: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MeshGlowEngineTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Box(
        modifier = modifier.meshGlowPulse(
            color = glowColor,
            radius = glowRadius,
            alpha = if (active) glowAlpha else 0.15f
        )
    ) {
        content()
    }
}

fun Modifier.meshGlowPulse(
    color: Color,
    radius: Dp = 16.dp,
    alpha: Float = 0.4f
): Modifier = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            radius.toPx(),
            0f,
            0f,
            shadowColor
        )
        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            paint = paint
        )
    }
}

// ── Breathing Animation ──

@Composable
fun MeshBreathingContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minScale: Float = 0.985f,
    maxScale: Float = 1.015f,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val transition = rememberInfiniteTransition(label = "BreathingTransition")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

fun Modifier.meshBreathing(
    enabled: Boolean = true,
    scaleAmount: Float = 0.02f
): Modifier = if (!enabled) this else this.graphicsLayer {
    val scale = 1.0f + (scaleAmount * 0.5f)
    scaleX = scale
    scaleY = scale
}

// ── Floating Animation ──

@Composable
fun MeshFloatingContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    floatDistance: Dp = 8.dp,
    durationMillis: Int = 2200,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val transition = rememberInfiniteTransition(label = "FloatingTransition")
    val floatOffset by transition.animateFloat(
        initialValue = -floatDistance.value,
        targetValue = floatDistance.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatOffsetY"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationY = floatOffset
        }
    ) {
        content()
    }
}

fun Modifier.meshFloating(
    offsetPx: Float = 12f
): Modifier = this.graphicsLayer {
    translationY = offsetPx
}

// ── Card Reveal ──

@Composable
fun MeshCardReveal(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0.94f,
        animationSpec = MeshMotionPresets.CardPress,
        label = "CardRevealScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0.0f,
        animationSpec = tween(MeshDurationPresets.Medium2, easing = MeshEasingPresets.Decelerate),
        label = "CardRevealAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}

fun Modifier.meshCardReveal(
    visible: Boolean = true,
    delayMs: Int = 0
): Modifier = this.graphicsLayer {
    alpha = if (visible) 1.0f else 0.0f
    scaleX = if (visible) 1.0f else 0.95f
    scaleY = if (visible) 1.0f else 0.95f
}

// ── Stagger Animation ──

@Composable
fun MeshStaggeredColumn(
    modifier: Modifier = Modifier,
    itemCount: Int,
    baseDelayMs: Int = 30,
    content: @Composable ColumnScope.(staggerModifier: (index: Int) -> Modifier) -> Unit
) {
    Column(modifier = modifier) {
        val staggerModifier: (index: Int) -> Modifier = { index: Int ->
            Modifier.meshStaggeredItem(index = index, baseDelayMs = baseDelayMs)
        }
        content(staggerModifier)
    }
}

@Composable
fun MeshStaggeredRow(
    modifier: Modifier = Modifier,
    itemCount: Int,
    baseDelayMs: Int = 30,
    content: @Composable RowScope.(staggerModifier: (index: Int) -> Modifier) -> Unit
) {
    Row(modifier = modifier) {
        val staggerModifier: (index: Int) -> Modifier = { index: Int ->
            Modifier.meshStaggeredItem(index = index, baseDelayMs = baseDelayMs)
        }
        content(staggerModifier)
    }
}



fun Modifier.meshStaggeredItem(
    index: Int,
    baseDelayMs: Int = 35
): Modifier = composed {
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(index) {
        kotlinx.coroutines.delay((index * baseDelayMs).toLong())
        revealed = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(MeshDurationPresets.Medium1, easing = MeshEasingPresets.Decelerate),
        label = "StaggerAlpha_$index"
    )

    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "StaggerScale_$index"
    )

    val translateY by animateFloatAsState(
        targetValue = if (revealed) 0f else 16f,
        animationSpec = tween(MeshDurationPresets.Medium1, easing = MeshEasingPresets.Decelerate),
        label = "StaggerTranslation_$index"
    )

    this.graphicsLayer {
        this.alpha = alpha
        scaleX = scale
        scaleY = scale
        translationY = translateY
    }
}

// ── Visibility Animation ──

enum class MeshVisibilityPreset {
    FadeThrough,
    Scale,
    SlideUp,
    ContainerTransform
}

@Composable
fun MeshVisibilityAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    preset: MeshVisibilityPreset = MeshVisibilityPreset.FadeThrough,
    content: @Composable () -> Unit
) {
    val enter: EnterTransition = when (preset) {
        MeshVisibilityPreset.FadeThrough -> MeshNavigationMotion.FadeThroughEnter
        MeshVisibilityPreset.Scale -> MeshNavigationMotion.ScaleEnter
        MeshVisibilityPreset.SlideUp -> MeshNavigationMotion.SlideInUp
        MeshVisibilityPreset.ContainerTransform -> MeshNavigationMotion.ContainerTransformEnter
    }

    val exit: ExitTransition = when (preset) {
        MeshVisibilityPreset.FadeThrough -> MeshNavigationMotion.FadeThroughExit
        MeshVisibilityPreset.Scale -> MeshNavigationMotion.ScaleExit
        MeshVisibilityPreset.SlideUp -> MeshNavigationMotion.SlideOutDown
        MeshVisibilityPreset.ContainerTransform -> MeshNavigationMotion.ContainerTransformExit
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit
    ) {
        content()
    }
}

// ── FAB Animation ──

@Immutable
object MeshFabAnimation {
    val EntranceEnter = MeshNavigationMotion.ScaleEnter
    val EntranceExit = MeshNavigationMotion.ScaleExit

    @Composable
    fun rememberFabPressScale(isPressed: Boolean): Float {
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1.0f,
            animationSpec = MeshMotionPresets.FabMotion,
            label = "FabPressScaleState"
        )
        return scale
    }
}

fun Modifier.meshFabPress(isPressed: Boolean): Modifier = this.graphicsLayer {
    val scale = if (isPressed) 0.92f else 1.0f
    scaleX = scale
    scaleY = scale
}
