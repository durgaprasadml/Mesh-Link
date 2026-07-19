package com.meshlink.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.util.rememberHapticManager

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = {
        Icon(Icons.Default.ChevronRight, contentDescription = "Navigate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    },
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val hapticManager = rememberHapticManager()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button) {
                        hapticManager.performLightClick()
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (subtitle != null) "$title. $subtitle" else title
                if (onClick != null) role = Role.Button
            }
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Handled by outer semantics
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
            trailingContent()
        }
    }
}
