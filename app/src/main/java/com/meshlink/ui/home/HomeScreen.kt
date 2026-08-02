package com.meshlink.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.messaging.presentation.ChatsListViewModel
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.search.MeshSearchOverlay
import com.meshlink.util.MeshIdNormalizer

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

    val filteredChats by remember(searchQuery, chatsState.chats) {
        derivedStateOf {
            if (searchQuery.isBlank()) chatsState.chats
            else chatsState.chats.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val lazyListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        MeshScreen(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToNearby,
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat"
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Section 1: Header
                item(key = "header") {
                    HomeHeroSection(
                        userIdentity = uiState.userIdentity,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }

                // Section 2: Search Bar
                item(key = "search") {
                    HomeSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onOpenOverlay = { isSearchOverlayVisible = true }
                    )
                }

                // Section 3: Quick Actions (Nearby, Broadcast, SOS)
                item(key = "quick_actions") {
                    QuickActionsSection(
                        onNavigateToNearby = onNavigateToNearby,
                        onNavigateToBroadcast = onNavigateToBroadcast,
                        onNavigateToSos = onNavigateToSos,
                        onNavigateToDiagnostics = onNavigateToDiagnostics,
                        onStartConversation = { isSearchOverlayVisible = true },
                        nearbyCount = uiState.nearbyDevices.size
                    )
                }

                // Section 4: Recent Chats (Occupies 70-80% height)
                if (filteredChats.isEmpty()) {
                    item(key = "empty_state") {
                        RecentChatsEmptyState(
                            searchQuery = searchQuery,
                            onStartChatting = onNavigateToNearby
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
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

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
}
