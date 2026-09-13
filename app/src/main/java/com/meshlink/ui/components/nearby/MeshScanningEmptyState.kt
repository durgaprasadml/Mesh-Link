package com.meshlink.ui.components.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun MeshScanningEmptyState(
    title: String = "Scanning for Mesh Peers",
    description: String = "Searching for nearby active Mesh Link nodes via BLE transport...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarEmptyPulse")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse2"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MeshTheme.spacing.large)
            .semantics { contentDescription = "$title. $description" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = size.minDimension / 2f
                
                // Outer Pulse Ring 1
                drawCircle(
                    color = primaryColor.copy(alpha = (1f - pulse1).coerceIn(0f, 0.6f)),
                    radius = maxRadius * pulse1,
                    style = Stroke(width = 3f)
                )

                // Outer Pulse Ring 2
                drawCircle(
                    color = primaryColor.copy(alpha = (1f - pulse2).coerceIn(0f, 0.6f)),
                    radius = maxRadius * pulse2,
                    style = Stroke(width = 3f)
                )

                // Core Hub
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = maxRadius * 0.35f
                )
            }

            TargetScanningIcon(
                modifier = Modifier.size(48.dp),
                tint = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MeshTheme.spacing.large)
        )
    }
}

/**
 * Modern 3D Animated Radar Scanner featuring:
 * - 3 concentric circular radar rings with depth shading
 * - Rotating bright blue scanning beam with a smooth continuous 360-degree sweep
 * - Soft glowing phosphor sweep trail
 * - 3D spherical lens shading and outer rim depth lighting
 * - Small glowing center emitter point with ambient halo
 * - Subtly pulsing mesh-node blips positioned across the radar rings that react to the sweep
 */
@Composable
fun TargetScanningIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar3DScannerTransition")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarPulsePhase"
    )


    val cyanAccent = remember(tint) {
        Color(
            red = (tint.red * 0.35f + 0f * 0.65f).coerceIn(0f, 1f),
            green = (tint.green * 0.45f + 0.92f * 0.55f).coerceIn(0f, 1f),
            blue = (tint.blue * 0.25f + 1f * 0.75f).coerceIn(0f, 1f)
        )
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Active 3D radar scanning for mesh nodes"
        }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = (size.minDimension / 2f) - 2.dp.toPx()
        if (maxR <= 0f) return@Canvas

        // 1. Subtle 3D Glass Radar Dish Backing (Convex depth)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.22f),
                    tint.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = maxR
            ),
            radius = maxR,
            center = center
        )

        // 2. Crosshair Grid Lines (Subtle tactical HUD reference)
        val gridColor = tint.copy(alpha = 0.16f)
        val gridStroke = 1.dp.toPx()
        drawLine(
            color = gridColor,
            start = Offset(center.x - maxR * 0.94f, center.y),
            end = Offset(center.x + maxR * 0.94f, center.y),
            strokeWidth = gridStroke
        )
        drawLine(
            color = gridColor,
            start = Offset(center.x, center.y - maxR * 0.94f),
            end = Offset(center.x, center.y + maxR * 0.94f),
            strokeWidth = gridStroke
        )

        // 3. Concentric Radar Rings with 3D Depth
        // Ring 1 (Inner)
        drawCircle(
            color = tint.copy(alpha = 0.32f),
            radius = maxR * 0.33f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Ring 2 (Middle)
        drawCircle(
            color = tint.copy(alpha = 0.48f),
            radius = maxR * 0.64f,
            center = center,
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Ring 3 (Outer Base Ring)
        drawCircle(
            color = tint.copy(alpha = 0.75f),
            radius = maxR * 0.94f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 3D Rim Lighting / Depth Bevel on Outer Ring (Top-Left Highlight, Bottom-Right Shadow)
        drawArc(
            brush = Brush.sweepGradient(
                0.00f to tint.copy(alpha = 0.95f),
                0.25f to cyanAccent.copy(alpha = 0.9f),
                0.50f to tint.copy(alpha = 0.3f),
                0.75f to tint.copy(alpha = 0.15f),
                1.00f to tint.copy(alpha = 0.95f),
                center = center
            ),
            startAngle = 135f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx()),
            topLeft = Offset(center.x - maxR * 0.94f, center.y - maxR * 0.94f),
            size = Size(maxR * 1.88f, maxR * 1.88f)
        )

        // 4. Rotating Radar Sweep Beam and Phosphor Trail
        rotate(degrees = sweepAngle, pivot = center) {
            // Rotating Sweep Trail (covers ~70 degrees behind the beam)
            val trailBrush = Brush.sweepGradient(
                0.00f to Color.Transparent,
                0.78f to Color.Transparent,
                0.86f to tint.copy(alpha = 0.04f),
                0.93f to tint.copy(alpha = 0.12f),
                0.98f to cyanAccent.copy(alpha = 0.28f),
                1.00f to tint.copy(alpha = 0.45f),
                center = center
            )
            drawCircle(
                brush = trailBrush,
                radius = maxR * 0.94f,
                center = center
            )

            // Scanning Beam Line (from center to outer ring edge at 0 degrees)
            // Soft outer glow of the beam
            drawLine(
                color = tint.copy(alpha = 0.45f),
                start = center,
                end = Offset(center.x + maxR * 0.94f, center.y),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Main bright blue/cyan beam
            drawLine(
                color = cyanAccent,
                start = center,
                end = Offset(center.x + maxR * 0.94f, center.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Crisp center glint
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = center,
                end = Offset(center.x + maxR * 0.94f, center.y),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }


        // 6. Glowing 3D Center Point / Emitter Hub
        val centerBaseR = 3.dp.toPx()
        // Center ambient pulse halo
        drawCircle(
            color = tint.copy(alpha = 0.25f + 0.15f * pulsePhase),
            radius = centerBaseR * (1.8f + 0.4f * pulsePhase),
            center = center
        )
        // Center hub ring
        drawCircle(
            color = tint,
            radius = centerBaseR,
            center = center
        )
        // Intense glowing center core
        drawCircle(
            color = Color.White,
            radius = centerBaseR * 0.5f,
            center = center
        )
    }
}

