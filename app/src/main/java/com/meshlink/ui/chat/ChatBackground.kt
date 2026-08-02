package com.meshlink.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.meshlink.messaging.presentation.ConnectionState
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Lightweight tactical grid and subtle ambient mesh backdrop.
 * Designed to maintain 60 FPS scrolling without recomposition overhead.
 */
@Composable
fun ChatBackground(
    modifier: Modifier = Modifier,
    connectionState: ConnectionState = ConnectionState.OFFLINE,
    content: @Composable () -> Unit
) {
    val backgroundColor = MeshTheme.colors.background
    val gridColor = if (connectionState == ConnectionState.DIRECT) {
        MeshTheme.colors.primary.copy(alpha = 0.03f)
    } else if (connectionState == ConnectionState.RELAY) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.03f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    }

    val ambientGlow = Brush.radialGradient(
        colors = listOf(
            when (connectionState) {
                ConnectionState.DIRECT -> MeshTheme.colors.primary.copy(alpha = 0.06f)
                ConnectionState.RELAY -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                ConnectionState.OFFLINE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
            },
            Color.Transparent
        ),
        radius = 1200f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .background(ambientGlow)
    ) {
        // Draw static tactical mesh grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 48.dp.toPx()
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                    pathEffect = dashPathEffect
                )
                x += gridSpacing
            }

            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = dashPathEffect
                )
                y += gridSpacing
            }
        }

        content()
    }
}
