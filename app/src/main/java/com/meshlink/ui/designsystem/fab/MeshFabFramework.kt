package com.meshlink.ui.designsystem.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.motion.meshGlow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.haptics.rememberMeshHaptics

/**
 * Reusable Floating Action Framework for Mesh-Link 2026.
 * Provides context-aware FABs, expandable FABs, spring feedback, and scroll-aware auto-collapse.
 */

@Composable
fun MeshFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    label: String? = null,
    visible: Boolean = true,
    containerColor: Color = MeshTheme.colors.primary,
    contentColor: Color = Color.Black,
    size: Dp = 56.dp
) {
    val haptics = rememberMeshHaptics()
    var isPressed by remember { mutableStateOf(false) }

    val fabScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "FabPressScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .scale(fabScale)
                .meshGlow(color = containerColor, radius = 12.dp, alpha = 0.35f)
                .clip(if (expanded && !label.isNullOrEmpty()) MeshTheme.shapes.pill else CircleShape)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = if (expanded && !label.isNullOrEmpty()) MeshTheme.shapes.pill else CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.buttonPress()
                    onClick()
                },
            color = containerColor,
            tonalElevation = MeshTheme.elevation.floating,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = size, minHeight = size)
                    .padding(horizontal = if (expanded && !label.isNullOrEmpty()) 20.dp else 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                if (expanded && !label.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Remembers a NestedScrollConnection that automatically collapses or hides a FAB during scroll.
 */
@Composable
fun rememberMeshFabScrollConnection(
    onScrollStateChange: (isVisible: Boolean) -> Unit
): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    onScrollStateChange(false)
                } else if (available.y > 12f) {
                    onScrollStateChange(true)
                }
                return Offset.Zero
            }
        }
    }
}
