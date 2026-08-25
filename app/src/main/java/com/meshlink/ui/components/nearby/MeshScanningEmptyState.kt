package com.meshlink.ui.components.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

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
 * Modern target/crosshair discovery radar icon featuring:
 * - Concentric circular target rings
 * - A small center dot
 * - A thin diagonal crosshair pointer extending toward the upper-right
 * - Clean rounded line strokes
 */
@Composable
fun TargetScanningIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Target scanning radar icon"
        }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = 2.5.dp.toPx()
        val ringStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val maxR = (size.minDimension / 2f) - strokeWidth

        // Outer Ring
        drawCircle(
            color = tint,
            radius = maxR * 0.90f,
            center = center,
            style = ringStroke
        )

        // Middle Concentric Ring
        drawCircle(
            color = tint,
            radius = maxR * 0.52f,
            center = center,
            style = ringStroke
        )

        // Small Center Dot/Circle
        drawCircle(
            color = tint,
            radius = maxR * 0.16f,
            center = center
        )

        // Diagonal crosshair / target pointer extending toward the upper-right (45 degrees)
        val cos45 = 0.70710678f
        val sin45 = 0.70710678f

        // Main upper-right target pointer extending past outer ring
        drawLine(
            color = tint,
            start = Offset(center.x + cos45 * (maxR * 0.20f), center.y - sin45 * (maxR * 0.20f)),
            end = Offset(center.x + cos45 * (maxR * 1.08f), center.y - sin45 * (maxR * 1.08f)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Symmetrical lower-left crosshair notch aligning the crosshair axis
        drawLine(
            color = tint,
            start = Offset(center.x - cos45 * (maxR * 0.20f), center.y + sin45 * (maxR * 0.20f)),
            end = Offset(center.x - cos45 * (maxR * 0.52f), center.y + sin45 * (maxR * 0.52f)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
