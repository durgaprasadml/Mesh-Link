package com.meshlink.ui.sos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 180dp Large Emergency SOS Button.
 * Primary focal centerpiece of the Emergency SOS screen.
 */
@Composable
fun EmergencyButton(
    state: SosUiState,
    onActivate: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var countdownValue by remember { mutableIntStateOf(3) }
    val shakeOffset = remember { Animatable(0f) }

    // Breathing pulse for idle state
    val infiniteTransition = rememberInfiniteTransition(label = "sos_button_pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_breathing_scale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sos_button_scale"
    )

    // Trigger failure shake animation if status transitions to FAILED
    LaunchedEffect(state.status) {
        if (state.status == SosStatus.FAILED) {
            for (i in 0..3) {
                shakeOffset.animateTo(12f, tween(50))
                shakeOffset.animateTo(-12f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    // 3-second hold countdown effect
    LaunchedEffect(isPressed) {
        if (isPressed && state.status == SosStatus.SAFE) {
            countdownValue = 3
            progress.snapTo(0f)

            for (i in 3 downTo 1) {
                countdownValue = i
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                progress.animateTo(
                    targetValue = (4 - i) / 3f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            }

            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onActivate()
            isPressed = false
        } else {
            progress.animateTo(0f, animationSpec = tween(250))
            countdownValue = 3
        }
    }

    val buttonColor = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.errorContainer
        SosStatus.BROADCASTING -> MaterialTheme.colorScheme.errorContainer
        SosStatus.DELIVERED -> MaterialTheme.colorScheme.primaryContainer
        SosStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.onErrorContainer
        SosStatus.BROADCASTING -> MaterialTheme.colorScheme.onErrorContainer
        SosStatus.DELIVERED -> MaterialTheme.colorScheme.onPrimaryContainer
        SosStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = when (state.status) {
                    SosStatus.SAFE -> if (isPressed) "Holding to activate SOS. $countdownValue seconds remaining." else "Emergency SOS button. Press and hold for 3 seconds to alert emergency services and mesh peers."
                    SosStatus.BROADCASTING -> "Broadcasting SOS emergency alert"
                    SosStatus.DELIVERED -> "SOS alert delivered to responders"
                    SosStatus.FAILED -> "SOS alert failed. Tap to retry."
                }
                role = Role.Button
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Pulse Beacon Ring
            if (state.status == SosStatus.SAFE && !isPressed) {
                EmergencyBeaconPulse(size = 180.dp, color = MaterialTheme.colorScheme.error, enabled = true)
            } else if (state.status == SosStatus.BROADCASTING) {
                EmergencyBeaconPulse(size = 180.dp, color = MaterialTheme.colorScheme.error, enabled = true)
            }

            // Circular Hold Progress Ring
            if (isPressed && state.status == SosStatus.SAFE) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(210.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }

            // Main 180dp Interactive SOS Circle
            Surface(
                modifier = Modifier
                    .size(180.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .scale(if (state.status == SosStatus.SAFE && !isPressed) breathingScale * buttonScale else buttonScale)
                    .pointerInput(state.status) {
                        if (state.status == SosStatus.SAFE || state.status == SosStatus.FAILED) {
                            detectTapGestures(
                                onPress = {
                                    if (state.status == SosStatus.FAILED) {
                                        onActivate()
                                    } else {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    }
                                }
                            )
                        }
                    },
                shape = CircleShape,
                color = buttonColor,
                shadowElevation = 6.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isPressed && state.status == SosStatus.SAFE -> {
                            Text(
                                text = "$countdownValue",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 58.sp
                                ),
                                color = contentColor
                            )
                        }
                        state.status == SosStatus.BROADCASTING -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = contentColor,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SENDING",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                            }
                        }
                        state.status == SosStatus.DELIVERED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "SOS Delivered",
                                    modifier = Modifier.size(48.dp),
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "DELIVERED",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                            }
                        }
                        state.status == SosStatus.FAILED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry SOS",
                                    modifier = Modifier.size(44.dp),
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "RETRY",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                            }
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Emergency SOS Icon",
                                    modifier = Modifier.size(42.dp),
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SOS",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    ),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hold Instruction Label
        Text(
            text = when {
                isPressed -> "Keep holding to confirm SOS..."
                state.status == SosStatus.SAFE -> "Press and hold for 3 seconds"
                state.status == SosStatus.BROADCASTING -> "Broadcasting alert across Mesh network..."
                state.status == SosStatus.DELIVERED -> "Emergency alert delivered to responders"
                state.status == SosStatus.FAILED -> "Broadcast failed. Tap button to retry."
                else -> "Press and hold for 3 seconds"
            },
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
