package com.meshlink.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.domain.model.Chat
import com.meshlink.messaging.presentation.ChatsListViewModel
import com.meshlink.ui.components.ConnectionStatusPill
import com.meshlink.ui.components.DashboardCard
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConnectionState {
    CONNECTED, SEARCHING, NO_DEVICES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val chatsViewModel: ChatsListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatsState by chatsViewModel.uiState.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val connectionState = when {
        uiState.nearbyDevices.isNotEmpty() -> ConnectionState.CONNECTED
        else -> ConnectionState.SEARCHING
    }
    
    val filteredChats = remember(searchQuery, chatsState.chats) {
        if (searchQuery.isBlank()) {
            chatsState.chats
        } else {
            chatsState.chats.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNearby,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConnectionStatusPill(state = connectionState)
                    
                    // Profile Avatar (Click to navigate to Settings)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onNavigateToSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = uiState.user?.name?.firstOrNull()?.uppercase() ?: "U"
                        Text(
                            text = initial,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                
                // SearchBar
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { isSearchActive = false },
                            expanded = isSearchActive,
                            onExpandedChange = { isSearchActive = it },
                            placeholder = { Text("Search chats or devices") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    },
                    expanded = isSearchActive,
                    onExpandedChange = { isSearchActive = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    // Search results could go here if active, but for now we filter the main list
                }
            }

            AnimatedVisibility(
                visible = !isSearchActive && searchQuery.isBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium)
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = MeshTheme.spacing.mediumLarge),
                        horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                    ) {
                        item(contentType = "dashboard_card") {
                            DashboardCard(
                                icon = Icons.Default.Wifi,
                                title = "Nearby Devices",
                                subtitle = "${uiState.nearbyDevices.size} available",
                                onClick = onNavigateToNearby,
                                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTintColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        item(contentType = "dashboard_card") {
                            DashboardCard(
                                icon = Icons.Default.Campaign,
                                title = "Broadcasts",
                                subtitle = "Send to all",
                                onClick = onNavigateToBroadcast,
                                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTintColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        item(contentType = "dashboard_card") {
                            DashboardCard(
                                icon = Icons.Default.Warning,
                                title = "SOS",
                                subtitle = "Emergency",
                                onClick = onNavigateToSos,
                                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                                iconTintColor = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
                }
            }

            Text(
                text = "Recent Chats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium)
            )

            AnimatedContent<Boolean>(
                targetState = filteredChats.isEmpty(),
                label = "chat_list_empty_state_transition"
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyState(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = if (searchQuery.isNotBlank()) "No results found" else "No recent chats",
                        description = if (searchQuery.isNotBlank()) "Try a different search term." else "Tap the + button to find nearby devices and start chatting.",
                        primaryButtonText = if (searchQuery.isBlank()) "Find Nearby Devices" else null,
                        onPrimaryButtonClick = if (searchQuery.isBlank()) onNavigateToNearby else null
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredChats, key = { it.id }, contentType = { "chat_item" }) { chat ->
                            ChatItem(
                                chat = chat,
                                onClick = {
                                    val safeName = chat.name.ifBlank { chat.id.takeLast(8) }
                                    onNavigateToChat(chat.id, safeName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val displayInitial = chat.name.firstOrNull()?.toString()?.uppercase() ?: "?"
            Text(
                text = displayInitial,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
        
        // Message Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.name.ifBlank { chat.id.takeLast(8) },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            Text(
                text = chat.lastMessage ?: "No messages yet",
                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
        
        // Timestamp & Status
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatTime(chat.lastMessageAt),
                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(MeshTheme.spacing.extraLarge)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    if (timeInMillis == 0L) return ""
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}
