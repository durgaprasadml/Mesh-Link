package com.meshlink.ui.designsystem.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = BorderStroke(0.5.dp, MeshTheme.colors.border),
    containerColor: Color = MeshTheme.colors.cardSurface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = MeshTheme.shapes.cards,
        color = containerColor,
        border = border,
        shadowElevation = MeshTheme.elevation.raised
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun MeshOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke = BorderStroke(1.dp, MeshTheme.colors.outline),
    containerColor: Color = MeshTheme.colors.surface,
    content: @Composable () -> Unit
) {
    MeshCard(
        modifier = modifier,
        onClick = onClick,
        border = border,
        containerColor = containerColor,
        content = content
    )
}

@Composable
fun MeshStatusCard(
    statusTitle: String,
    statusSubtitle: String,
    statusColor: Color = MeshTheme.colors.primary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null
) {
    MeshOutlinedCard(
        modifier = modifier,
        onClick = onClick,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        containerColor = statusColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MeshTheme.shapes.small)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = statusTitle,
                    style = MeshTheme.customTypography.title,
                    color = MeshTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusSubtitle,
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun MeshInfoCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    MeshCard(
        modifier = modifier,
        containerColor = MeshTheme.colors.surfaceVariant,
        border = BorderStroke(0.5.dp, MeshTheme.colors.border)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MeshTheme.colors.primary,
                    modifier = Modifier
                        .padding(top = 2.dp, end = 12.dp)
                        .size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MeshTheme.customTypography.subtitle,
                    color = MeshTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MeshTheme.customTypography.body,
                    color = MeshTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun MeshGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = MeshTheme.shapes.cards,
        color = MeshTheme.colors.glassSurface,
        border = BorderStroke(1.dp, MeshTheme.colors.glassBorder),
        shadowElevation = MeshTheme.elevation.flat
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    statusBadge: @Composable (() -> Unit)? = null,
    actionButton: @Composable (() -> Unit)? = null
) {
    MeshCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MeshTheme.colors.surfaceVariant,
        border = BorderStroke(1.dp, MeshTheme.colors.primary.copy(alpha = 0.3f))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textPrimary
                )
                statusBadge?.invoke()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MeshTheme.colors.textSecondary
            )
            if (actionButton != null) {
                Spacer(modifier = Modifier.height(16.dp))
                actionButton()
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    MeshCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MeshTheme.colors.textTertiary,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MeshTheme.colors.primary
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        color = MeshTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    MeshCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MeshTheme.shapes.small)
                    .background(MeshTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MeshTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MeshTheme.colors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MeshTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MediaCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    QuickActionTile(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun VoiceCard(
    duration: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🎙 VOICE NOTE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = duration, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MeshTheme.colors.textSecondary)
        }
    }
}

@Composable
fun FileCard(
    fileName: String,
    fileSize: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshCard(modifier = modifier, onClick = onClick) {
        Column {
            Text(text = fileName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = fileSize, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MeshTheme.colors.textTertiary)
        }
    }
}

@Composable
fun ImageCard(
    caption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshCard(modifier = modifier, onClick = onClick) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(MeshTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = caption ?: "IMAGE PAYLOAD", fontSize = 12.sp, color = MeshTheme.colors.textSecondary)
        }
    }
}

@Preview
@Composable
private fun MeshCardsPreview() {
    MeshTheme(themeMode = "DARK") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroCard(title = "Mesh Telemetry Active", subtitle = "Connected to 14 active nodes via Bluetooth LE & Wi-Fi Direct.")
            MetricCard(label = "Signal RSSI", value = "-68", unit = "dBm")
        }
    }
}
