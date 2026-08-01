package com.meshlink.ui.designsystem.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(MeshTheme.shapes.dialogs)
                .border(0.5.dp, MeshTheme.colors.border, MeshTheme.shapes.dialogs),
            color = MeshTheme.colors.surface,
            shadowElevation = MeshTheme.elevation.overlay
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
                if (confirmButton != null || dismissButton != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        confirmButton?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
fun MeshBottomSheet(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MeshTheme.shapes.bottomSheets)
            .border(0.5.dp, MeshTheme.colors.border, MeshTheme.shapes.bottomSheets),
        color = MeshTheme.colors.surface,
        shadowElevation = MeshTheme.elevation.overlay
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(MeshTheme.shapes.pill)
                    .background(MeshTheme.colors.border)
            )
            if (title != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textPrimary,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
