package com.meshlink.ui.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.meshlink.ui.designsystem.components.glass.meshDialogGlass
import com.meshlink.ui.designsystem.components.glass.meshNavigationGlass
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.accessibility.meshMinTouchTarget
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors
import com.meshlink.ui.designsystem.theme.motion.meshPressScale

@Composable
fun MeshNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .meshNavigationGlass(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RowScope.MeshNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    val contentColor = if (selected) colors.primary else colors.textSecondary
    val indicatorBg = if (selected) colors.primary.copy(alpha = 0.15f) else Color.Transparent

    Column(
        modifier = modifier
            .weight(1f)
            .meshMinTouchTarget()
            .meshPressScale()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(indicatorBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            style = MeshTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun MeshNavigationRail(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .meshNavigationGlass(shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun MeshNavigationDrawer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .meshNavigationGlass(shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .padding(20.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalMeshSemanticColors.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.elevatedSurface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
        content = content
    )
}

@Composable
fun MeshDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalMeshSemanticColors.current
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .meshDialogGlass()
                .padding(24.dp)
        ) {
            Text(text = title, style = MeshTheme.typography.titleLarge, color = colors.textPrimary)
            Box(modifier = Modifier.padding(vertical = 16.dp)) {
                content()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (dismissButton != null) {
                    dismissButton()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                confirmButton?.invoke()
            }
        }
    }
}

@Composable
fun MeshSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LocalMeshSemanticColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .meshNavigationGlass(shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MeshTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = actionLabel,
                style = MeshTheme.typography.labelLarge,
                color = colors.primary,
                modifier = Modifier
                    .clickable { onActionClick() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun MeshTooltip(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardSurface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MeshTheme.typography.bodySmall, color = colors.textPrimary)
    }
}

@Composable
fun MeshMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMeshSemanticColors.current
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(colors.cardSurface)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        content = content
    )
}
