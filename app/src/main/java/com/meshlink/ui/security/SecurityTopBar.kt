package com.meshlink.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityTopBar(
    title: String = "Security & Trust Center",
    subtitle: String = "Protected by Mesh Encryption",
    onBackClick: () -> Unit,
    isSearchActive: Boolean = false,
    onSearchToggle: () -> Unit = {},
    onRefreshClick: (() -> Unit)? = null,
    onGenerateReportClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate Back"
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchActive) "Close Search" else "Search Security Settings"
                )
            }

            if (onRefreshClick != null) {
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Diagnostics"
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Security Options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onGenerateReportClick != null) {
                        DropdownMenuItem(
                            text = { Text("Export Security Report") },
                            leadingIcon = {
                                Icon(Icons.Default.Assessment, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onGenerateReportClick()
                            }
                        )
                    }
                    if (onHelpClick != null) {
                        DropdownMenuItem(
                            text = { Text("Security & Privacy FAQ") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onHelpClick()
                            }
                        )
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        modifier = modifier
    )
}
