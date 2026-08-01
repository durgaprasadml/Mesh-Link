package com.meshlink.ui.designsystem.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.glass.meshLightGlass
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors
import com.meshlink.ui.designsystem.theme.motion.meshPressScale

@Composable
fun MeshMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = LocalMeshSemanticColors.current.primary,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalMeshSemanticColors.current
    val clickableMod = if (onClick != null) modifier.meshPressScale().clickable { onClick() } else modifier

    Column(
        modifier = clickableMod
            .meshLightGlass(shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MeshTheme.typography.labelMedium,
                color = colors.textSecondary
            )
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = value,
            style = MeshTheme.typography.headlineMedium,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun MeshQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalMeshSemanticColors.current.primary
) {
    val colors = LocalMeshSemanticColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .meshPressScale()
            .meshLightGlass(shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MeshTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MeshTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun MeshInfoCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = LocalMeshSemanticColors.current.info
) {
    val colors = LocalMeshSemanticColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .meshLightGlass(shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MeshTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
    }
}

@Composable
fun MeshListItem(
    headline: String,
    modifier: Modifier = Modifier,
    subhead: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalMeshSemanticColors.current
    val clickableMod = if (onClick != null) modifier.meshPressScale().clickable { onClick() } else modifier

    Row(
        modifier = clickableMod
            .fillMaxWidth()
            .meshLightGlass(shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MeshTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            if (subhead != null) {
                Text(
                    text = subhead,
                    style = MeshTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}
