package com.meshlink.ui.designsystem.components.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.accessibility.meshMinTouchTarget
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors
import com.meshlink.ui.designsystem.theme.haptics.rememberMeshHaptics
import com.meshlink.ui.designsystem.theme.motion.meshPressScale

@Composable
fun MeshPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val colors = LocalMeshSemanticColors.current
    val haptics = rememberMeshHaptics()
    val bgColor = if (enabled) colors.primary else colors.disabled
    val textColor = if (enabled) Color.White else colors.textTertiary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .meshMinTouchTarget()
            .meshPressScale()
            .clip(shape)
            .background(bgColor)
            .clickable(enabled = enabled && !isLoading) {
                haptics.buttonPress()
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = text, style = MeshTheme.typography.labelLarge, color = textColor)
            }
        }
    }
}

// Alias for generic calls
@Composable
fun MeshButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) = MeshPrimaryButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled, isLoading = isLoading)

@Composable
fun MeshSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val colors = LocalMeshSemanticColors.current
    val haptics = rememberMeshHaptics()
    val bgColor = if (enabled) colors.elevatedSurface else colors.disabled
    val textColor = if (enabled) colors.textPrimary else colors.textTertiary

    Box(
        modifier = modifier
            .meshMinTouchTarget()
            .meshPressScale()
            .clip(shape)
            .background(bgColor)
            .clickable(enabled = enabled) {
                haptics.buttonPress()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MeshTheme.typography.labelLarge, color = textColor)
        }
    }
}

@Composable
fun MeshOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val colors = LocalMeshSemanticColors.current
    val haptics = rememberMeshHaptics()
    val borderColor = if (enabled) colors.primary else colors.border
    val textColor = if (enabled) colors.primary else colors.textTertiary

    Box(
        modifier = modifier
            .meshMinTouchTarget()
            .meshPressScale()
            .clip(shape)
            .border(1.5.dp, borderColor, shape)
            .clickable(enabled = enabled) {
                haptics.buttonPress()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MeshTheme.typography.labelLarge, color = textColor)
        }
    }
}

@Composable
fun MeshGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val colors = LocalMeshSemanticColors.current
    val haptics = rememberMeshHaptics()
    val textColor = if (enabled) colors.textPrimary else colors.textTertiary

    Box(
        modifier = modifier
            .meshMinTouchTarget()
            .meshPressScale()
            .clickable(enabled = enabled) {
                haptics.buttonPress()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(text = text, style = MeshTheme.typography.labelMedium, color = textColor)
        }
    }
}

@Composable
fun MeshDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val colors = LocalMeshSemanticColors.current
    val haptics = rememberMeshHaptics()
    val bgColor = if (enabled) colors.danger else colors.disabled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .meshMinTouchTarget()
            .meshPressScale()
            .clip(shape)
            .background(bgColor)
            .clickable(enabled = enabled) {
                haptics.buttonPress()
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MeshTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
fun MeshIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    tint: Color = LocalMeshSemanticColors.current.textPrimary
) {
    val colors = LocalMeshSemanticColors.current
    val haptics = rememberMeshHaptics()

    Box(
        modifier = modifier
            .size(size)
            .meshMinTouchTarget()
            .meshPressScale()
            .clip(CircleShape)
            .background(colors.elevatedSurface.copy(alpha = 0.6f))
            .border(1.dp, colors.border.copy(alpha = 0.4f), CircleShape)
            .clickable(enabled = enabled) {
                haptics.buttonPress()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun MeshFAB(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalMeshSemanticColors.current.primary
) {
    val haptics = rememberMeshHaptics()
    Box(
        modifier = modifier
            .size(56.dp)
            .meshMinTouchTarget()
            .meshPressScale()
            .clip(CircleShape)
            .background(accentColor)
            .clickable {
                haptics.buttonPress()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun MeshExtendedFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true
) {
    val colors = LocalMeshSemanticColors.current
    val haptics = rememberMeshHaptics()
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .meshPressScale()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.primary)
            .clickable {
                haptics.buttonPress()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            Row {
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = text, style = MeshTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@Composable
fun MeshFloatingActionCapsule(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshExtendedFAB(text = text, icon = icon, onClick = onClick, modifier = modifier, expanded = true)
}

@Composable
fun MeshLoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    MeshPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading
    )
}
