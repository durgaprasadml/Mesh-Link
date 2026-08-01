package com.meshlink.ui.designsystem.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MeshTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    border: BorderStroke? = null,
    elevation: Dp = MeshTheme.elevation.level1,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val animatedScale by animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            animationSpec = tween(durationMillis = 150),
            label = "card_scale"
        )

        Card(
            onClick = onClick,
            modifier = modifier.scale(animatedScale),
            shape = shape,
            border = border,
            interactionSource = interactionSource,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            border = border,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            content = content
        )
    }
}

