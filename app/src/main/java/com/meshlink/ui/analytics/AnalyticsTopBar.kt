package com.meshlink.ui.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTopBar(
    title: String = "Mesh Analytics",
    subtitle: String = "Network Insights",
    meshStatus: String = "ONLINE",
    activeConnectionsCount: Int = 0,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onSearchToggle: (() -> Unit)? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val statusColor = when (meshStatus.uppercase()) {
        "ONLINE", "HEALTHY", "OPERATIONAL" -> MeshTheme.colors.success
        "DEGRADED", "MODERATE" -> MeshTheme.colors.warning
        else -> MeshTheme.colors.error
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = MeshTheme.elevation.level2
    ) {
        Column {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = isSearchActive,
                        label = "top_bar_search_anim"
                    ) { searching ->
                        if (searching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    onSearchQueryChange?.invoke(it)
                                },
                                placeholder = { Text("Search events, devices & stats...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = MeshTheme.spacing.small),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        onSearchQueryChange?.invoke("")
                                        isSearchActive = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close search")
                                    }
                                }
                            )
                        } else {
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = "$subtitle • $activeConnectionsCount Peers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = {
                            if (onSearchToggle != null) {
                                onSearchToggle()
                            } else {
                                isSearchActive = true
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search analytics")
                        }
                    }
                    IconButton(onClick = onRefreshClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh telemetry")
                    }
                    IconButton(onClick = onExportClick) {
                        Icon(Icons.Default.Share, contentDescription = "Export report")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Network") },
                                onClick = {
                                    showMenu = false
                                    onRefreshClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Summary") },
                                onClick = {
                                    showMenu = false
                                    onExportClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

