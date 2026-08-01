package com.meshlink.ui.designsystem.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshListItem(
    headline: String,
    modifier: Modifier = Modifier,
    subhead: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column {
                Text(
                    text = headline,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MeshTheme.colors.textPrimary
                )
                if (subhead != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subhead,
                        fontSize = 12.sp,
                        color = MeshTheme.colors.textSecondary
                    )
                }
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}

@Composable
fun ChatBubbleComponent(
    messageText: String,
    timestamp: String,
    isFromMe: Boolean,
    modifier: Modifier = Modifier,
    deliveryStatus: String? = null
) {
    val bgColor = if (isFromMe) MeshTheme.colors.primary.copy(alpha = 0.2f) else MeshTheme.colors.surfaceVariant
    val borderColor = if (isFromMe) MeshTheme.colors.primary.copy(alpha = 0.5f) else MeshTheme.colors.border
    val align = if (isFromMe) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .clip(MeshTheme.shapes.medium)
                .background(bgColor)
                .border(0.5.dp, borderColor, MeshTheme.shapes.medium)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = messageText,
                    fontSize = 14.sp,
                    color = MeshTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timestamp,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MeshTheme.colors.textTertiary
                    )
                    if (deliveryStatus != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = deliveryStatus,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MeshTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineComponent(
    title: String,
    time: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(MeshTheme.shapes.circular)
                .background(MeshTheme.colors.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = time, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MeshTheme.colors.textTertiary)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 12.sp, color = MeshTheme.colors.textSecondary)
        }
    }
}

@Composable
fun MeshDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MeshTheme.colors.divider
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MeshTheme.colors.textSecondary,
            letterSpacing = 0.8.sp
        )
        action?.invoke()
    }
}
