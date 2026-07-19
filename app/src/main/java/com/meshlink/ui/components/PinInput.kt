package com.meshlink.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.launch

@Composable
fun PinInput(
    pin: String,
    onPinChange: (String) -> Unit,
    pinLength: Int = 4,
    isError: Boolean = false,
    onMaxPinEntered: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Shake animation state
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isError) {
        if (isError) {
            // Vibrate on error
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
            
            // Shake animation
            coroutineScope.launch {
                offsetX.animateTo(20f, animationSpec = tween(50, easing = LinearEasing))
                offsetX.animateTo(-20f, animationSpec = tween(50, easing = LinearEasing))
                offsetX.animateTo(20f, animationSpec = tween(50, easing = LinearEasing))
                offsetX.animateTo(-20f, animationSpec = tween(50, easing = LinearEasing))
                offsetX.animateTo(10f, animationSpec = tween(50, easing = LinearEasing))
                offsetX.animateTo(-10f, animationSpec = tween(50, easing = LinearEasing))
                offsetX.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // PIN Dots
        Row(
            modifier = Modifier
                .padding(vertical = MeshTheme.spacing.large)
                .graphicsLayer { translationX = offsetX.value },
            horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < pin.length
                val color by animateColorAsState(
                    targetValue = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else if (isFilled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    label = "dotColor"
                )
                
                val borderColor = if (isError) {
                    MaterialTheme.colorScheme.error
                } else if (isFilled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .size(MeshTheme.spacing.mediumLarge)
                        .clip(CircleShape)
                        .background(color)
                        .border(MeshTheme.spacing.extraSmall, borderColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

        // Numeric Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                ) {
                    rowKeys.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(72.dp).weight(1f))
                        } else {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                KeypadButton(
                                    key = key,
                                    onClick = {
                                        if (key == "DEL") {
                                            if (pin.isNotEmpty()) {
                                                onPinChange(pin.dropLast(1))
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    vibrator.vibrate(
                                                        VibrationEffect.createPredefined(
                                                            VibrationEffect.EFFECT_CLICK
                                                        )
                                                    )
                                                }
                                            }
                                        } else {
                                            if (pin.length < pinLength) {
                                                val newPin = pin + key
                                                onPinChange(newPin)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    vibrator.vibrate(
                                                        VibrationEffect.createPredefined(
                                                            VibrationEffect.EFFECT_CLICK
                                                        )
                                                    )
                                                }
                                                if (newPin.length == pinLength) {
                                                    onMaxPinEntered?.invoke(newPin)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(key: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (key == "DEL") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Text(
                text = key,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
