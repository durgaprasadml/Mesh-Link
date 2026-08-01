package com.meshlink.ui.designsystem.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.meshlink.ui.designsystem.motion.MeshSignalPulse
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Unified Dialog Framework for Mesh-Link 2026.
 * Contains MeshDialog base container, Alert Dialog, Loading Dialog,
 * and Progress Dialog with shared glass styling, spring motion, and elevation.
 */

@Composable
fun MeshDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(MeshTheme.shapes.large)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MeshTheme.colors.glassBorder,
                                MeshTheme.colors.border.copy(alpha = 0.2f)
                            )
                        ),
                        shape = MeshTheme.shapes.large
                    ),
                color = MeshTheme.colors.surface,
                tonalElevation = MeshTheme.elevation.hero,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * Alert Dialog for critical alerts and confirmations.
 */
@Composable
fun MeshAlertDialog(
    visible: Boolean,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    dismissText: String? = "Cancel",
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    if (!visible) return

    MeshDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tintColor = if (isDestructive) MeshTheme.colors.error else MeshTheme.colors.primary

            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MeshTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = MeshTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (dismissText != null) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MeshTheme.shapes.pill
                    ) {
                        Text(text = dismissText, color = MeshTheme.colors.textPrimary)
                    }
                }

                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MeshTheme.shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) MeshTheme.colors.error else MeshTheme.colors.primary,
                        contentColor = if (isDestructive) Color.White else Color.Black
                    )
                ) {
                    Text(text = confirmText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Loading Dialog for blocking asynchronous operations.
 */
@Composable
fun MeshLoadingDialog(
    visible: Boolean,
    message: String = "Loading...",
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {}
) {
    if (!visible) return

    MeshDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MeshSignalPulse(
                active = true,
                color = MeshTheme.colors.primary,
                modifier = Modifier.size(64.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MeshTheme.colors.primary,
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = message,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MeshTheme.colors.textPrimary
            )
        }
    }
}

/**
 * Progress Dialog for tracking percentage-based background operations.
 */
@Composable
fun MeshProgressDialog(
    visible: Boolean,
    title: String = "Processing...",
    progress: Float,
    modifier: Modifier = Modifier,
    stepDescription: String? = null,
    onCancel: (() -> Unit)? = null
) {
    if (!visible) return

    val progressPercent = (progress.coerceIn(0f, 1f) * 100).toInt()

    MeshDialog(
        onDismissRequest = { onCancel?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onCancel != null,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MeshTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$progressPercent%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MeshTheme.colors.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MeshTheme.shapes.pill),
                color = MeshTheme.colors.primary,
                trackColor = MeshTheme.colors.primary.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )

            if (stepDescription != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stepDescription,
                    fontSize = 13.sp,
                    color = MeshTheme.colors.textSecondary
                )
            }

            if (onCancel != null) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MeshTheme.shapes.pill
                ) {
                    Text(text = "Cancel", color = MeshTheme.colors.textPrimary)
                }
            }
        }
    }
}
