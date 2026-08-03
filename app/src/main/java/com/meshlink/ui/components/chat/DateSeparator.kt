package com.meshlink.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import androidx.compose.runtime.remember
import com.meshlink.ui.util.DateTimeUtils

@Composable
fun DateSeparator(timestamp: Long, modifier: Modifier = Modifier) {
    val dateText = remember(timestamp) { DateTimeUtils.formatDateSeparator(timestamp) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MeshTheme.spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(MeshTheme.spacing.medium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.small)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
