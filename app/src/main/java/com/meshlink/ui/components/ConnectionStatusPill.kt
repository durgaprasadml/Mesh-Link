package com.meshlink.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.home.ConnectionState

@Composable
fun ConnectionStatusPill(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, dotColor, text) = when (state) {
        ConnectionState.CONNECTED -> Triple(MeshTheme.colors.success.copy(alpha = 0.15f), MeshTheme.colors.success, "Connected")
        ConnectionState.SEARCHING -> Triple(MeshTheme.colors.warning.copy(alpha = 0.15f), MeshTheme.colors.warning, "Searching")
        ConnectionState.NO_DEVICES -> Triple(MeshTheme.colors.error.copy(alpha = 0.15f), MeshTheme.colors.error, "Offline")
    }

    val animatedBackgroundColor by animateColorAsState(targetValue = backgroundColor, animationSpec = tween(300), label = "bg_color")
    val animatedDotColor by animateColorAsState(targetValue = dotColor, animationSpec = tween(300), label = "dot_color")

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(animatedBackgroundColor)
            .semantics(mergeDescendants = true) {
                contentDescription = "Connection Status: $text"
            }
            .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MeshTheme.spacing.mediumSmall)
                .clip(CircleShape)
                .background(animatedDotColor)
        )
        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
        Text(
            text = text,
            color = animatedDotColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
