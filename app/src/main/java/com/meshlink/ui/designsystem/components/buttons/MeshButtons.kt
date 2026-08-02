package com.meshlink.ui.designsystem.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.MeshColorTokens

enum class MeshButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    GHOST,
    EMERGENCY
}

@Composable
fun MeshButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MeshButtonVariant = MeshButtonVariant.PRIMARY,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val colors = MeshTheme.colors
    val shapes = MeshTheme.shapes

    val backgroundColor = when (variant) {
        MeshButtonVariant.PRIMARY -> if (enabled) colors.primary else colors.disabled
        MeshButtonVariant.SECONDARY -> if (enabled) colors.surfaceVariant else colors.disabled
        MeshButtonVariant.OUTLINED, MeshButtonVariant.GHOST -> Color.Transparent
        MeshButtonVariant.EMERGENCY -> if (enabled) colors.emergency else colors.disabled
    }

    val contentColor = when (variant) {
        MeshButtonVariant.PRIMARY, MeshButtonVariant.EMERGENCY -> Color.Black
        MeshButtonVariant.SECONDARY -> colors.textPrimary
        MeshButtonVariant.OUTLINED, MeshButtonVariant.GHOST -> colors.primary
    }

    val border = if (variant == MeshButtonVariant.OUTLINED && enabled) {
        BorderStroke(1.dp, colors.border)
    } else null

    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled && !isLoading,
        shape = shapes.buttons,
        color = backgroundColor,
        contentColor = contentColor,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MeshTheme.customTypography.button,
                color = contentColor
            )
            if (trailingIcon != null && !isLoading) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    MeshButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = MeshButtonVariant.PRIMARY,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        isLoading = isLoading
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    MeshButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = MeshButtonVariant.SECONDARY,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        isLoading = isLoading
    )
}

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    MeshButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = MeshButtonVariant.OUTLINED,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        isLoading = isLoading
    )
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    MeshButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = MeshButtonVariant.GHOST,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled
    )
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    MeshButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = MeshButtonVariant.EMERGENCY,
        leadingIcon = leadingIcon,
        enabled = enabled,
        isLoading = isLoading
    )
}

@Composable
fun SuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val colors = MeshTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled && !isLoading,
        shape = MeshTheme.shapes.buttons,
        color = if (enabled) colors.success else colors.disabled,
        contentColor = Color.Black
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MeshTheme.customTypography.button,
                color = Color.Black
            )
        }
    }
}

@Composable
fun EmergencyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    MeshButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = MeshButtonVariant.EMERGENCY,
        enabled = enabled
    )
}

@Composable
fun MeshIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MeshTheme.colors.primary,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(MeshTheme.shapes.circular)
            .background(MeshTheme.colors.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else MeshTheme.colors.disabled,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun MeshSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MeshTheme.shapes.medium)
            .background(MeshTheme.colors.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(MeshTheme.shapes.small)
                    .background(if (isSelected) MeshTheme.colors.primary else Color.Transparent)
                    .clickable { onItemSelected(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else MeshTheme.colors.textSecondary
                )
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
private fun MeshButtonPreviewLight() {
    MeshTheme(themeMode = "LIGHT") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeshButton(text = "Primary", onClick = {})
            MeshButton(text = "Emergency", onClick = {}, variant = MeshButtonVariant.EMERGENCY)
        }
    }
}

@Preview(name = "Dark Mode")
@Composable
private fun MeshButtonPreviewDark() {
    MeshTheme(themeMode = "DARK") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeshButton(text = "Primary", onClick = {})
            MeshButton(text = "Outlined", onClick = {}, variant = MeshButtonVariant.OUTLINED)
        }
    }
}
