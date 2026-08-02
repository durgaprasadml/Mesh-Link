package com.meshlink.ui.media.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Real-time media search bar component.
 */
@Composable
fun MediaSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search by name, sender, or file extension...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = MeshTheme.customTypography.body,
                color = MeshTheme.colors.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MeshTheme.colors.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Search",
                        tint = MeshTheme.colors.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MeshTheme.colors.primary,
            unfocusedBorderColor = MeshTheme.colors.outline.copy(alpha = 0.3f),
            focusedContainerColor = MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MeshTheme.colors.surfaceVariant.copy(alpha = 0.3f)
        )
    )
}
