package com.meshlink.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.messaging.presentation.ChatsListViewModel
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.fab.MeshFloatingActionButton
import com.meshlink.ui.designsystem.fab.rememberMeshFabScrollConnection
import com.meshlink.ui.designsystem.search.MeshSearchOverlay
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

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

    var searchQuery by remember { mutableStateOf("") }
    var isSearchOverlayVisible by remember { mutableStateOf(false) }
    var isFabVisible by remember { mutableStateOf(true) }

    val lazyListState = rememberLazyListState()
    val fabScrollConnection = rememberMeshFabScrollConnection { visible ->
        isFabVisible = visible
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshScreen(
            containerColor = MeshTheme.colors.background,
            floatingActionButton = {
                MeshFloatingActionButton(
                    onClick = onNavigateToNearby,
                    icon = Icons.Default.Wifi,
                    contentDescription = "Scan Mesh Network",
                    label = "Scan Mesh",
                    expanded = true,
                    visible = isFabVisible,
                    containerColor = MeshTheme.colors.primary,
                    contentColor = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.padding(
                        end = MeshSpacing.FabEndPadding,
                        bottom = MeshSpacing.FabBottomPadding
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(fabScrollConnection)
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Section 1: Hero Command Header
                item(key = "hero_section") {
                    HomeHeroSection(
                        userIdentity = uiState.userIdentity,
                        onNavigateToSettings = onNavigateToSettings,
                        modifier = Modifier.homeSectionStagger(0)
                    )
                }

                // Section 2: Mesh Overview & Telemetry Control Card
                item(key = "overview_section") {
                    MeshOverviewSection(
                        nearbyDevices = uiState.nearbyDevices,
                        onNavigateToNearby = onNavigateToNearby,
                        modifier = Modifier.homeSectionStagger(1)
                    )
                }

                // Section 3: Floating Tactical Search Bar
                item(key = "search_section") {
                    HomeSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onOpenOverlay = { isSearchOverlayVisible = true },
                        modifier = Modifier.homeSectionStagger(2)
                    )
                }

                // Section 4: Tactical Quick Actions Grid
                item(key = "quick_actions_section") {
                    QuickActionsSection(
                        onNavigateToNearby = onNavigateToNearby,
                        onNavigateToBroadcast = onNavigateToBroadcast,
                        onNavigateToSos = onNavigateToSos,
                        onNavigateToDiagnostics = onNavigateToDiagnostics,
                        onStartConversation = { isSearchOverlayVisible = true },
                        nearbyCount = uiState.nearbyDevices.size,
                        modifier = Modifier.homeSectionStagger(3)
                    )
                }

                // Section 5: Active Communications Preview List
                item(key = "communications_section") {
                    CommunicationsSection(
                        chats = chatsState.chats,
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToNearby = onNavigateToNearby,
                        searchQuery = searchQuery,
                        modifier = Modifier.homeSectionStagger(4)
                    )
                }

                // Section 6: Live Mesh Event Activity Stream
                item(key = "activity_section") {
                    ActivityTimelineSection(
                        nearbyDevices = uiState.nearbyDevices,
                        modifier = Modifier.homeSectionStagger(5)
                    )
                }

                // Section 7: Smart Network Recommendations
                item(key = "recommendations_section") {
                    RecommendationsSection(
                        nearbyDevices = uiState.nearbyDevices,
                        unreadCount = uiState.unreadChatsCount,
                        onNavigateToNearby = onNavigateToNearby,
                        onNavigateToBroadcast = onNavigateToBroadcast,
                        modifier = Modifier.homeSectionStagger(6)
                    )
                }
            }
        }

        // Full Screen Tactical Search Overlay
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
