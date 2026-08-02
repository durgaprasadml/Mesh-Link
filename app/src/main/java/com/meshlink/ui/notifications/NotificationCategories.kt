package com.meshlink.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun NotificationCategoriesGrid(
    categories: List<NotificationCategoryUi>,
    onCategoryClick: (NotificationCategory) -> Unit,
    onCategoryToggle: (NotificationCategory, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Notification Channels",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
        ) {
            items(categories) { categoryUi ->
                NotificationCategoryCard(
                    categoryUi = categoryUi,
                    onClick = { onCategoryClick(categoryUi.category) },
                    onToggle = { enabled -> onCategoryToggle(categoryUi.category, enabled) }
                )
            }
        }
    }
}

@Composable
fun NotificationCategoryCard(
    categoryUi: NotificationCategoryUi,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (categoryUi.category) {
        NotificationCategory.MESSAGES -> Icons.Default.Message
        NotificationCategory.BROADCASTS -> Icons.Default.Radio
        NotificationCategory.NEARBY_DEVICES -> Icons.Default.Person
        NotificationCategory.SOS -> Icons.Default.Warning
        NotificationCategory.TRANSFERS -> Icons.Default.Download
        NotificationCategory.ANALYTICS -> Icons.Default.Analytics
        NotificationCategory.SECURITY -> Icons.Default.Security
        NotificationCategory.SYSTEM -> Icons.Default.CellTower
        else -> Icons.Default.Message
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Switch(
                    checked = categoryUi.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.8f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryUi.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (categoryUi.unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(categoryUi.unreadCount.toString(), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

