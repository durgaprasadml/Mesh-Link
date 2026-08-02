package com.meshlink.ui.sos

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val infiniteTransition = rememberInfiniteTransition(label = "sos_button_pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_breathing_scale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sos_button_scale"
    )

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
            progress.animateTo(0f, animationSpec = tween(300))
            countdownValue = 3
        }
    }

    val buttonColor = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.error
        SosStatus.BROADCASTING -> MaterialTheme.colorScheme.error
        SosStatus.DELIVERED -> MaterialTheme.colorScheme.primary
        SosStatus.FAILED -> MaterialTheme.colorScheme.error
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = when (state.status) {
                    SosStatus.SAFE -> if (isPressed) "Holding to activate. $countdownValue seconds remaining." else "Press and hold for 3 seconds to send emergency SOS."
                    SosStatus.BROADCASTING -> "Broadcasting SOS emergency message"
                    SosStatus.DELIVERED -> "SOS delivered successfully"
                    SosStatus.FAILED -> "SOS broadcast failed. Tap to retry."
                }
                role = Role.Button
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Subtle pulse aura ring when idle
            if (state.status == SosStatus.SAFE && !isPressed) {
                EmergencyBeaconPulse(size = 170.dp, color = MaterialTheme.colorScheme.error, enabled = true)
            } else if (state.status == SosStatus.BROADCASTING) {
                EmergencyBeaconPulse(size = 170.dp, color = MaterialTheme.colorScheme.error, enabled = true)
            }

            // Outer Hold Countdown Progress Ring
            if (isPressed && state.status == SosStatus.SAFE) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(200.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }

            // Main 170dp Interactive SOS Circle
            Surface(
                modifier = Modifier
                    .size(170.dp)
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
                                    fontSize = 56.sp
                                ),
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                        state.status == SosStatus.BROADCASTING -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onError,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SENDING",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                        state.status == SosStatus.DELIVERED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "SOS Delivered",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "DELIVERED",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        state.status == SosStatus.FAILED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry SOS",
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "RETRY",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Emergency SOS Icon",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SOS",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when {
                isPressed -> "Keep holding..."
                state.status == SosStatus.SAFE -> "Press and hold for 3 seconds"
                state.status == SosStatus.BROADCASTING -> "Broadcasting alert across Mesh network..."
                state.status == SosStatus.DELIVERED -> "Emergency alert delivered to responders"
                state.status == SosStatus.FAILED -> "Broadcast failed. Tap button to retry."
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = if (isPressed || state.status == SosStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

