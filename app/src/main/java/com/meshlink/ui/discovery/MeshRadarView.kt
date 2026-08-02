package com.meshlink.ui.discovery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.nearby.ActivePacketEvent
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MeshRadarView(
    devices: List<BleDevice>,
    modifier: Modifier = Modifier,
    selectedAddress: String? = null,
    isScanning: Boolean = true,
    latestPacketEvent: ActivePacketEvent? = null,
    onNodeSelected: ((BleDevice) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val rotationDegrees by DiscoveryAnimations.rememberRadarRotation(isScanning)
    val pulseScale by DiscoveryAnimations.rememberNodePulseScale()
    val beamAlpha by DiscoveryAnimations.rememberBeamAlpha()
    val isReducedMotion = DiscoveryAnimations.isReducedMotion()

    val nodeOffsets = remember { mutableStateMapOf<String, Offset>() }

    val primaryColor = MeshTheme.colors.primary
    val connectedColor = MeshTheme.colors.connected
    val textPrimaryColor = MeshTheme.colors.textPrimary
    val textSecondaryColor = MeshTheme.colors.textSecondary

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(devices) {
                    detectTapGestures { tapOffset ->
                        var clickedDevice: BleDevice? = null
                        var minDistance = Float.MAX_VALUE
                        val tapRadiusPx = 36.dp.toPx()

                        nodeOffsets.forEach { (address, pos) ->
                            val dx = tapOffset.x - pos.x
                            val dy = tapOffset.y - pos.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist <= tapRadiusPx && dist < minDistance) {
                                minDistance = dist
                                clickedDevice = devices.firstOrNull { it.address == address }
                            }
                        }

                        clickedDevice?.let { device ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNodeSelected?.invoke(device)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (minOf(size.width, size.height) / 2f) * 0.88f

            // 1. Draw Concentric Range Rings (Near, Medium, Far)
            val ringCount = 3
            val ringDash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (i in 1..ringCount) {
                val radius = (maxRadius / ringCount) * i
                drawCircle(
                    color = primaryColor.copy(alpha = 0.12f * (4 - i)),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5f, pathEffect = if (i < ringCount) ringDash else null)
                )
            }

            // Crosshair Axes
            drawLine(
                color = primaryColor.copy(alpha = 0.08f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1f
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.08f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1f
            )

            // 2. Draw Sweeping Radar Line
            if (!isReducedMotion) {
                val angleRad = Math.toRadians(rotationDegrees.toDouble())
                val sweepEnd = Offset(
                    x = center.x + (maxRadius * cos(angleRad)).toFloat(),
                    y = center.y + (maxRadius * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = center,
                    end = sweepEnd,
                    strokeWidth = 2f
                )
            }

            // 3. Compute Node Placement dynamically based on RSSI and hash angle
            nodeOffsets.clear()
            devices.forEachIndexed { index, device ->
                val angleInRad = if (devices.size == 1) {
                    PI / 4
                } else {
                    val hash = (device.address.hashCode() and 0x7FFFFFFF)
                    (hash % 360) * (PI / 180)
                }

                // Normalized RSSI distance factor (0.25 to 0.85 of maxRadius)
                val clampedRssi = device.rssi.coerceIn(-95, -45)
                val normDist = 1f - ((clampedRssi + 95) / 50f)
                val radius = (maxRadius * (0.25f + normDist * 0.60f)).coerceIn(maxRadius * 0.2f, maxRadius * 0.9f)

                val nodePos = Offset(
                    x = center.x + (radius * cos(angleInRad)).toFloat(),
                    y = center.y + (radius * sin(angleInRad)).toFloat()
                )
                nodeOffsets[device.address] = nodePos

                // 4. Connection Link to Central Hub
                val isSelected = device.address == selectedAddress
                val linkColor = if (device.isConnected) connectedColor else primaryColor
                val linkAlpha = if (device.isConnected) beamAlpha else 0.25f

                drawLine(
                    color = linkColor.copy(alpha = linkAlpha),
                    start = center,
                    end = nodePos,
                    strokeWidth = if (isSelected) 3f else 1.5f,
                    pathEffect = if (!device.isConnected) ringDash else null
                )

                // 5. Draw Node Point and Pulse Ring
                val nodeRadius = if (isSelected) 10.dp.toPx() else 7.dp.toPx()
                val pulseRadius = nodeRadius * (if (isSelected) pulseScale * 1.3f else 1.2f)

                drawCircle(
                    color = linkColor.copy(alpha = if (isSelected) 0.35f else 0.15f),
                    radius = pulseRadius,
                    center = nodePos
                )
                drawCircle(
                    color = linkColor,
                    radius = nodeRadius,
                    center = nodePos
                )
                drawCircle(
                    color = Color.White,
                    radius = nodeRadius * 0.4f,
                    center = nodePos
                )

                // Selected Target Lock Circle
                if (isSelected) {
                    drawCircle(
                        color = primaryColor,
                        radius = nodeRadius * 2.2f,
                        center = nodePos,
                        style = Stroke(width = 2f, pathEffect = ringDash)
                    )
                }

                // Node Name Label
                val paint = android.graphics.Paint().apply {
                    color = (if (isSelected) textPrimaryColor else textSecondaryColor).toArgb()
                    textSize = density.run { 11.sp.toPx() }
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawContext.canvas.nativeCanvas.drawText(
                    device.name.take(12),
                    nodePos.x,
                    nodePos.y + nodeRadius + density.run { 14.dp.toPx() },
                    paint
                )
            }

            // 6. Draw Central Hub (Local Device)
            val hubRadius = 11.dp.toPx()
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f * pulseScale),
                radius = hubRadius * 1.6f,
                center = center
            )
            drawCircle(
                color = primaryColor,
                radius = hubRadius,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = hubRadius * 0.45f,
                center = center
            )

            val hubPaint = android.graphics.Paint().apply {
                color = textPrimaryColor.toArgb()
                textSize = density.run { 11.sp.toPx() }
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                "You (Hub)",
                center.x,
                center.y + hubRadius + density.run { 14.dp.toPx() },
                hubPaint
            )
        }
    }
}
