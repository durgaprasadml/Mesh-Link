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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.launch

/**
 * Master Landing Experience Screen.
 * Plays the procedural 9-phase mesh network animation and seamlessly transitions to Home Screen.
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

    // Initialize node physics, connections, packets, and dust
    val nodes = remember(isWelcome) { NodePhysics.generateNodes(isWelcome) }
    val connectionAnimator = remember { MeshConnectionAnimator() }
    val ambientDust = remember { NodePhysics.generateAmbientDust() }
    val dataPackets = remember(nodes) { NodePhysics.generateDataPackets(nodes) }

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

        // Animate timeline progress 0.0f -> 1.0f
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

                // Physics update
                NodePhysics.updatePositions(
                    nodes = nodes,
                    width = 1080f, // Scaled dynamically by canvas size
                    height = 2200f,
                    timeMs = elapsedMs,
                    overallProgress = progress,
                    reduceMotion = false
                )

                // Connections & Packet update
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

    // Camera slow zoom effect (0.95 -> 1.04)
    val cameraScale = 0.95f + progress * 0.09f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimationConstants.DeepNavy)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                viewModel.onSkipClicked()
            }
    ) {
        // Multi-Layer Procedural Canvas
        MeshFormationCanvas(
            nodes = nodes,
            connectionAnimator = connectionAnimator,
            ambientDust = ambientDust,
            dataPackets = dataPackets,
            overallProgress = progress,
            timeMs = timeMs,
            isWelcomeMode = isWelcome,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = cameraScale
                    scaleY = cameraScale
                }
        )

        // Phase 8 Logo Emergence Overlay (Existing User / Logo reveal phase)
        AnimatedVisibility(
            visible = !isWelcome && progress in AnimationConstants.PHASE_6_PACKET_ROUTING_END..AnimationConstants.PHASE_9_TRANSITION_END,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AnimationConstants.ElectricBlue.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Mesh Link Logo",
                        tint = AnimationConstants.Cyan,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MESH LINK",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AnimationConstants.SoftWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )

                Text(
                    text = "Decentralized Mesh Network",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AnimationConstants.SoftWhiteTransparent,
                    letterSpacing = 1.sp
                )
            }
        }

        // Phase 7 Welcome Animation Overlay (First-Time User)
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
            visible = progress in 0.15f..0.85f,
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
