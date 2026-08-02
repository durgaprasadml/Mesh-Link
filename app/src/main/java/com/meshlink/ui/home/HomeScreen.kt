package com.meshlink.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.domain.model.Chat
import com.meshlink.messaging.presentation.ChatsListViewModel
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.search.MeshSearchOverlay
import com.meshlink.util.MeshIdNormalizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Production-Ready Messaging-First Home Screen for Mesh-Link.
 * Displays Compact Header, M3 Search Bar, 3 Quick Action Cards, and Dominant Recent Conversations list (~75% screen height).
 * Supports Material 3 pull-to-refresh, adaptive two-pane viewports, and edge-to-edge insets.
 */
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onNavigateToDiagnostics: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val chatsViewModel: ChatsListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatsState by chatsViewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchOverlayVisible by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var selectedChatForTwoPane by remember { mutableStateOf<Chat?>(null) }

    val filteredChats by remember(searchQuery, chatsState.chats) {
        derivedStateOf {
            if (searchQuery.isBlank()) chatsState.chats
            else chatsState.chats.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val handleRefresh = {
        isRefreshing = true
        coroutineScope.launch {
            delay(800)
            isRefreshing = false
        }
    }

    HomeAdaptiveLayout(
        singlePaneContent = {
            HomeContent(
                uiState = uiState,
                filteredChats = filteredChats,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onOpenOverlay = { isSearchOverlayVisible = true },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToNearby = onNavigateToNearby,
                onNavigateToBroadcast = onNavigateToBroadcast,
                onNavigateToSos = onNavigateToSos,
                onNavigateToDiagnostics = onNavigateToDiagnostics,
                onNavigateToChat = onNavigateToChat,
                isRefreshing = isRefreshing,
                onRefresh = { handleRefresh() }
            )
        },
        masterPaneContent = {
            HomeContent(
                uiState = uiState,
                filteredChats = filteredChats,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onOpenOverlay = { isSearchOverlayVisible = true },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToNearby = onNavigateToNearby,
                onNavigateToBroadcast = onNavigateToBroadcast,
                onNavigateToSos = onNavigateToSos,
                onNavigateToDiagnostics = onNavigateToDiagnostics,
                onNavigateToChat = { chatId, name ->
                    val found = chatsState.chats.find { it.id == chatId }
                    if (found != null) {
                        selectedChatForTwoPane = found
                    }
                    onNavigateToChat(chatId, name)
                },
                isRefreshing = isRefreshing,
                onRefresh = { handleRefresh() }
            )
        },
        detailPaneContent = {
            val chat = selectedChatForTwoPane ?: chatsState.chats.firstOrNull()
            if (chat != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        ChatAvatar(
                            name = chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) },
                            avatarUri = chat.avatarUri,
                            size = 64.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chat.lastMessage ?: "No recent messages",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select a conversation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )

    // Full Screen Search Overlay
    MeshSearchOverlay(
        visible = isSearchOverlayVisible,
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        onClose = { isSearchOverlayVisible = false },
        recentSearches = listOf("Alpha Node", "Emergency Channel", "Broadcast Updates"),
        onSuggestionClick = { selected ->
            searchQuery = selected
            isSearchOverlayVisible = false
        }
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    filteredChats: List<Chat>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onOpenOverlay: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToDiagnostics: (() -> Unit)?,
    onNavigateToChat: (String, String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    MeshScreen(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNearby,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                ),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = 8.dp, bottom = 8.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Start new conversation",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        MeshPullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Section 1: Compact Header (~10% height)
                item(key = "header") {
                    HomeHeroSection(
                        userIdentity = uiState.userIdentity,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }

                // Section 2: Search Bar (~10% height)
                item(key = "search") {
                    HomeSearchBar(
                        query = searchQuery,
                        onQueryChange = onQueryChange,
                        onOpenOverlay = onOpenOverlay
                    )
                }

                // Section 3: Quick Actions (~15% height)
                item(key = "quick_actions") {
                    QuickActionsSection(
                        onNavigateToNearby = onNavigateToNearby,
                        onNavigateToBroadcast = onNavigateToBroadcast,
                        onNavigateToSos = onNavigateToSos,
                        onNavigateToDiagnostics = onNavigateToDiagnostics,
                        nearbyCount = uiState.nearbyDevices.size
                    )
                }

                // Section Header for Conversations
                item(key = "recent_chats_header") {
                    HomeSectionHeader(title = "RECENT CHATS")
                }

                // Section 4: Recent Chats (Dominant ~65% height)
                if (filteredChats.isEmpty()) {
                    item(key = "empty_state") {
                        RecentChatsEmptyState(
                            searchQuery = searchQuery,
                            onStartChatting = onNavigateToNearby,
                            modifier = Modifier.emptyStateFade()
                        )
                    }
                } else {
                    items(
                        items = filteredChats,
                        key = { chat -> chat.id }
                    ) { chat ->
                        RecentChatRow(
                            chat = chat,
                            onClick = {
                                val safeName = chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) }
                                onNavigateToChat(chat.id, safeName)
                            },
                            modifier = Modifier.chatRowInsertion()
                        )
                        ChatDivider()
                    }
                }
            }
        }
    }
}
