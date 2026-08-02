package com.meshlink.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun AnalyticsCharts(
    hopDistribution: Map<Int, Int>,
    throughputSeries: ChartSeries = defaultSampleThroughputSeries(),
    modifier: Modifier = Modifier
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Charts & Activity Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Message throughput & connection stability over time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                selectedPointIndex?.let { idx ->
                    if (idx in throughputSeries.points.indices) {
                        val pt = throughputSeries.points[idx]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${pt.xLabel}: ${pt.value.toInt()} ${throughputSeries.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Canvas Line Chart
            val points = throughputSeries.points.ifEmpty { defaultSampleThroughputSeries().points }
            val animatedProgress = remember { Animatable(0f) }

            LaunchedEffect(points) {
                animatedProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1000)
                )
            }

            val lineColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            val secondaryLineColor = MaterialTheme.colorScheme.tertiary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val stepX = width / (points.size - 1).coerceAtLeast(1)
                            val index = (offset.x / stepX).toInt().coerceIn(0, points.size - 1)
                            selectedPointIndex = index
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val maxVal = (points.maxOfOrNull { it.value } ?: 100f).coerceAtLeast(10f)

                    // Draw grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = height * (i.toFloat() / gridLines)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Primary Chart Path
                    val path = Path()
                    val stepX = width / (points.size - 1).coerceAtLeast(1)

                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = height - (pt.value / maxVal * height * animatedProgress.value)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Points & Tooltip Line
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = height - (pt.value / maxVal * height * animatedProgress.value)
                        val isSelected = i == selectedPointIndex
                        val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()

                        drawCircle(
                            color = if (isSelected) lineColor else lineColor.copy(alpha = 0.8f),
                            radius = radius,
                            center = Offset(x, y)
                        )

                        if (isSelected) {
                            drawLine(
                                color = lineColor.copy(alpha = 0.5f),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }
            }

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { pt ->
                    Text(
                        text = pt.xLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun defaultSampleThroughputSeries(): ChartSeries {
    return ChartSeries(
        title = "Throughput",
        unit = "msg/s",
        points = listOf(
            ChartDataPoint("00:00", 12f),
            ChartDataPoint("04:00", 8f),
            ChartDataPoint("08:00", 45f),
            ChartDataPoint("12:00", 92f),
            ChartDataPoint("16:00", 64f),
            ChartDataPoint("20:00", 38f)
        )
    )
}
