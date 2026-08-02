package com.meshlink.ui.media

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * 48dp Material 3 Search Bar component for Media & Files.
 */
@Composable
fun MediaSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search files by name, type, or sender...",
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
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MeshTheme.colors.primary,
            unfocusedBorderColor = MeshTheme.colors.outline.copy(alpha = 0.3f),
            focusedContainerColor = MeshTheme.colors.surfaceVariant.copy(alpha = 0.4f),
            unfocusedContainerColor = MeshTheme.colors.surfaceVariant.copy(alpha = 0.2f)
        )
    )
}
