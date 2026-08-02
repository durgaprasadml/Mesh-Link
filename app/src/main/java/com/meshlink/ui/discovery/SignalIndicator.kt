package com.meshlink.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * SignalIndicator — Minimal Material 4-bar signal strength indicator.
 */
@Composable
fun SignalIndicator(
    signal: SignalStrength,
    modifier: Modifier = Modifier
) {
    val barCount = signal.barCount
    val barColor = when (signal.quality) {
        ConnectionQuality.EXCELLENT -> Color(0xFF00F59B) // Mint
        ConnectionQuality.GOOD -> Color(0xFF0284C7)      // Sky Blue
        ConnectionQuality.FAIR -> Color(0xFFFFB703)      // Amber
        ConnectionQuality.POOR -> Color(0xFFFF0055)      // Crimson
        ConnectionQuality.DISCONNECTED -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barHeights = listOf(6.dp, 9.dp, 12.dp, 15.dp)
        barHeights.forEachIndexed { index, height ->
            val isActive = index < barCount
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isActive) barColor
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
            )
        }
    }
}
