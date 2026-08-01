package com.meshlink.ui.designsystem.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Global Search Framework for Mesh-Link 2026.
 * Contains Search Overlay, Search Animation system, Suggestion Panel,
 * and Empty State component with unified glass aesthetics.
 */

@Composable
fun MeshSearchOverlay(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    suggestions: List<String> = emptyList(),
    recentSearches: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    onClearRecent: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    // Search Animation (Vertical slide enter/exit with spring motion)
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { -it }
        ) + fadeIn(),
        exit = slideOutVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            targetOffsetY = { -it }
        ) + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MeshTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Search Input Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MeshTheme.colors.textPrimary
                        )
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        placeholder = {
                            Text(
                                "Search mesh nodes, messages, channels...",
                                fontSize = 14.sp,
                                color = MeshTheme.colors.textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MeshTheme.colors.primary
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MeshTheme.colors.textSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = MeshTheme.shapes.pill,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MeshTheme.colors.surface,
                            unfocusedContainerColor = MeshTheme.colors.surface,
                            focusedBorderColor = MeshTheme.colors.primary,
                            unfocusedBorderColor = MeshTheme.colors.border
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MeshTheme.colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    content != null -> {
                        Box(modifier = Modifier.weight(1f)) {
                            content()
                        }
                    }

                    suggestions.isNotEmpty() || recentSearches.isNotEmpty() -> {
                        MeshSearchSuggestionPanel(
                            suggestions = suggestions,
                            recentSearches = recentSearches,
                            onSuggestionClick = onSuggestionClick,
                            onClearRecent = onClearRecent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    query.isNotEmpty() -> {
                        MeshSearchEmptyState(
                            query = query,
                            onClearQuery = { onQueryChange("") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    else -> {
                        MeshSearchEmptyState(
                            query = "",
                            onClearQuery = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Suggestion Panel component for displaying search recommendations and recent query history.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeshSearchSuggestionPanel(
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    recentSearches: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    onClearRecent: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT SEARCHES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MeshTheme.colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    if (onClearRecent != null) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            color = MeshTheme.colors.primary,
                            modifier = Modifier
                                .clip(MeshTheme.shapes.small)
                                .clickable { onClearRecent() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentSearches.forEach { search ->
                        Surface(
                            shape = MeshTheme.shapes.pill,
                            color = MeshTheme.colors.surface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MeshTheme.colors.border),
                            modifier = Modifier.clickable { onSuggestionClick(search) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MeshTheme.colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = search,
                                    fontSize = 13.sp,
                                    color = MeshTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Text(
                    text = "SUGGESTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }

            items(suggestions) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MeshTheme.shapes.small)
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = suggestion,
                        color = MeshTheme.colors.textPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Empty State component when search returns 0 results or initial search prompt.
 */
@Composable
fun MeshSearchEmptyState(
    query: String,
    modifier: Modifier = Modifier,
    onClearQuery: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MeshTheme.colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (query.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.Search,
                    contentDescription = null,
                    tint = MeshTheme.colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (query.isNotEmpty()) "No Results Found" else "Search Mesh Network",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MeshTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (query.isNotEmpty()) {
                    "We couldn't find any mesh nodes, messages, or channels matching \"$query\"."
                } else {
                    "Search active peers, encryption keys, broadcast channels, and message archives across the mesh."
                },
                fontSize = 14.sp,
                color = MeshTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (query.isNotEmpty() && onClearQuery != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onClearQuery,
                    shape = MeshTheme.shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MeshTheme.colors.primary,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = "Clear Search", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
