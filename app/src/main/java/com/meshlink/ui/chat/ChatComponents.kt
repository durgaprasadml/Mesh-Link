package com.meshlink.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.UserIdentity

/**
 * Reusable Chat UI components library for Mesh-Link Phase 4.
 */

@Composable
fun ChatAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isOnline: Boolean = true,
    isMeshConnected: Boolean = true
) {
    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase().ifBlank { "U" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45).sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Online & Mesh Status Dot
        val indicatorColor = if (isOnline) {
            if (isMeshConnected) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
        } else {
            MaterialTheme.colorScheme.outline
        }

        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(indicatorColor)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

@Composable
fun ChatDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = DividerDefaults.color.copy(alpha = 0.5f)
    )
}
