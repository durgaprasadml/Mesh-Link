package com.meshlink.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.messaging.presentation.ChatsListViewModel
import com.meshlink.ui.components.ConnectionStatusPill
import com.meshlink.ui.components.DashboardCard
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.ResponsiveDashboardGrid
import com.meshlink.ui.components.chat.ChatRowItem
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress

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
    val lazyListState = rememberLazyListState()

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

    val isScrollingUp = remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 || lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    MeshScreen(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isScrollingUp.value,
                enter = scaleIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = scaleOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onNavigateToNearby,
                    modifier = Modifier
                        .scaleOnPress(0.92f)
                        .padding(end = MeshSpacing.FabEndPadding, bottom = MeshSpacing.FabBottomPadding),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = MeshSpacing.ListBottomSpacing)
        ) {
            // Header Section: Modern Profile Header + Search Bar
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MeshSpacing.ScreenPadding, vertical = MeshSpacing.TopSafeArea),
                    color = Color.Transparent
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hello, ${uiState.userIdentity?.displayName ?: "Mesh User"}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ConnectionStatusPill(state = connectionState)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                                    .scaleOnPress(0.92f)
                                    .clickable(role = Role.Button, onClick = onNavigateToSettings)
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Button
                                        contentDescription = "Profile and Settings"
                                    }
                            ) {
                                com.meshlink.ui.components.UserAvatar(
                                    identity = uiState.userIdentity,
                                    size = 48.dp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(MeshSpacing.SearchBarSpacing))
                        
                        SearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    onSearch = { isSearchActive = false },
                                    expanded = isSearchActive,
                                    onExpandedChange = { isSearchActive = it },
                                    placeholder = { Text("Search chats or devices...") },
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
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(MeshSpacing.SearchBarCornerRadius)
                        ) {}
                    }
                }
            }

            // Dashboard Grid Section
            if (!isSearchActive && searchQuery.isBlank()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dashboard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = MeshSpacing.ScreenPadding)
                        )
                        Spacer(modifier = Modifier.height(MeshSpacing.SectionTitleBottomSpacing))
                        
                        ResponsiveDashboardGrid(
                            items = listOf(
                                { cardModifier ->
                                    DashboardCard(
                                        icon = Icons.Default.Wifi,
                                        title = "Nearby Devices",
                                        subtitle = "${uiState.nearbyDevices.size} available",
                                        onClick = onNavigateToNearby,
                                        modifier = cardModifier,
                                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        iconTintColor = MaterialTheme.colorScheme.primary
                                    )
                                },
                                { cardModifier ->
                                    DashboardCard(
                                        icon = Icons.Default.Campaign,
                                        title = "Broadcasts",
                                        subtitle = "Send to all",
                                        onClick = onNavigateToBroadcast,
                                        modifier = cardModifier,
                                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        iconTintColor = MaterialTheme.colorScheme.tertiary
                                    )
                                },
                                { cardModifier ->
                                    DashboardCard(
                                        icon = Icons.Default.Warning,
                                        title = "SOS",
                                        subtitle = "Emergency",
                                        onClick = onNavigateToSos,
                                        modifier = cardModifier,
                                        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        iconTintColor = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Recent Chats Section Header
            item {
                Text(
                    text = "Recent Chats",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        start = MeshSpacing.ScreenPadding,
                        end = MeshSpacing.ScreenPadding,
                        top = 8.dp,
                        bottom = MeshSpacing.SectionTitleBottomSpacing
                    )
                )
            }

            // Chat Items or Empty State
            if (filteredChats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MeshTheme.spacing.extraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = if (searchQuery.isNotBlank()) "No results found" else "No recent chats",
                            description = if (searchQuery.isNotBlank()) "Try a different search term." else "Tap the + button to find nearby devices and start chatting.",
                            primaryButtonText = if (searchQuery.isBlank()) "Find Nearby Devices" else null,
                            onPrimaryButtonClick = if (searchQuery.isBlank()) onNavigateToNearby else null
                        )
                    }
                }
            } else {
                items(
                    items = filteredChats,
                    key = { it.id },
                    contentType = { "chat_item" }
                ) { chat ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MeshSpacing.ScreenPadding, vertical = 4.dp)
                            .animateItem()
                    ) {
                        ChatRowItem(
                            chat = chat,
                            onClick = {
                                val safeName = chat.name.ifBlank { com.meshlink.util.MeshIdNormalizer.canonicalize(chat.id) }
                                onNavigateToChat(chat.id, safeName)
                            }
                        )
                    }
                }
            }
        }
    }
}
