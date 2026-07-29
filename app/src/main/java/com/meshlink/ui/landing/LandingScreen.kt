package com.meshlink.ui.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Master Landing Experience Screen.
 * Plays the procedural 6-phase starlight constellation experience and seamlessly transitions to Home Screen.
 */
@Composable
fun LandingScreen(
    onAnimationComplete: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val interactionSource = remember { MutableInteractionSource() }

    val isWelcome = uiState.isWelcomeMode
    val totalDurationMs = if (isWelcome) {
        AnimationConstants.WELCOME_ANIMATION_DURATION_MS
    } else {
        AnimationConstants.STARTUP_ANIMATION_DURATION_MS
    }

    val progressAnimatable = remember { Animatable(0f) }
    var timeMs by remember { mutableLongStateOf(0L) }

    // Initialize starfield nodes, connections, and data packets
    val nodes = remember(isWelcome) { NodePhysics.generateNodes(isWelcome) }
    val connectionAnimator = remember { MeshConnectionAnimator() }
    val dataPackets = remember(nodes) {
        // Pre-allocate white data packets traveling along edges
        List(18) { id ->
            DataPacket(
                id = id,
                fromNodeId = (id * 7) % nodes.size,
                toNodeId = (id * 7 + 3) % nodes.size,
                speed = 0.5f + (id % 5) * 0.15f,
                delayProgress = 0.25f + (id / 18f) * 0.40f
            )
        }
    }

    LaunchedEffect(nodes) {
        connectionAnimator.buildConnections(nodes)
    }

    // Master Animation Clock Loop
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onAnimationComplete()
            return@LaunchedEffect
        }

        val startTime = withFrameNanos { it }

        val animationJob = launch {
            progressAnimatable.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = totalDurationMs.toInt(),
                    easing = LinearEasing
                )
            )
        }

        // Frame tick loop for physics and time
        while (progressAnimatable.value < 1.0f && !uiState.isCompleted) {
            withFrameNanos { frameTimeNanos ->
                val elapsedMs = (frameTimeNanos - startTime) / 1_000_000L
                timeMs = elapsedMs

                val progress = progressAnimatable.value
                val deltaSec = 0.016f

                // Update star physics
                NodePhysics.updatePositions(
                    nodes = nodes,
                    width = 1080f,
                    height = 2200f,
                    timeMs = elapsedMs,
                    overallProgress = progress,
                    reduceMotion = false
                )

                // Update connections & white energy packet trails
                connectionAnimator.update(
                    overallProgress = progress,
                    packets = dataPackets,
                    deltaSec = deltaSec
                )
            }
        }

        animationJob.join()
        onAnimationComplete()
    }

    val progress = progressAnimatable.value

    // Camera Timeline Zoom Dynamics:
    // 0.00 -> 0.20: Close up on Seed Star (Scale 2.00f -> 1.50f)
    // 0.20 -> 0.85: Cinematic zoom out (Scale 1.50f -> 1.00f) revealing full text constellation
    // 0.85 -> 1.00: Transition zoom towards user node (Scale 1.00f -> 1.15f) with soft fade dissolve
    val cameraScale = when {
        progress < 0.20f -> 2.00f - (progress / 0.20f) * 0.50f
        progress in 0.20f..0.85f -> 1.50f - ((progress - 0.20f) / 0.65f) * 0.50f
        else -> 1.00f + ((progress - 0.85f) / 0.15f) * 0.15f
    }

    val canvasAlpha = if (progress > 0.88f) {
        1.0f - ((progress - 0.88f) / 0.12f).coerceIn(0f, 1f)
    } else {
        1.0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimationConstants.DeepSpaceNavy)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                viewModel.onSkipClicked()
            }
    ) {
        // Multi-Layer Procedural Starlight Canvas
        MeshFormationCanvas(
            nodes = nodes,
            connectionAnimator = connectionAnimator,
            dataPackets = dataPackets,
            overallProgress = progress,
            timeMs = timeMs,
            isWelcomeMode = isWelcome,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = cameraScale
                    scaleY = cameraScale
                    alpha = canvasAlpha
                }
        )

        // First-Time User Welcome Card Overlay
        if (isWelcome) {
            WelcomeAnimation(
                displayName = uiState.userName,
                avatarUri = uiState.avatarUri,
                visible = true,
                progress = progress
            )
        }

        // Tap to skip hint
        AnimatedVisibility(
            visible = progress in 0.12f..0.85f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Tap anywhere to skip",
                style = MaterialTheme.typography.labelMedium,
                color = AnimationConstants.SoftWhiteTransparent.copy(alpha = 0.5f),
                letterSpacing = 0.8.sp
            )
        }
    }
}
