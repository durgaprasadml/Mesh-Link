package com.meshlink.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.ResponsiveDashboardGrid
import com.meshlink.ui.components.chat.ChatRowItem
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress
import java.util.Calendar

enum class ConnectionState {
    CONNECTED, SEARCHING, NO_DEVICES
}

/** Returns a time-of-day greeting string. */
private fun greetingForHour(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
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

    // ── Mesh hero pulse animation ────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulseRing1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring1_alpha"
    )
    val pulseRing1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring1_scale"
    )
    val pulseRing2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(1800, 600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring2_alpha"
    )
    val pulseRing2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1800, 600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring2_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    MeshScreen(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isScrollingUp.value,
                enter = scaleIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = scaleOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToNearby,
                    modifier = Modifier
                        .scaleOnPress(0.93f)
                        .padding(end = MeshSpacing.FabEndPadding, bottom = MeshSpacing.FabBottomPadding),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(50),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
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
            // ── Header: Greeting + Avatar ──────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MeshSpacing.ScreenPadding,
                            end = MeshSpacing.ScreenPadding,
                            top = 20.dp,
                            bottom = 0.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = greetingForHour(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.userIdentity?.displayName ?: "Mesh Linker",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ConnectionStatusPill(state = connectionState)
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        )
                                    ),
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
                                size = 52.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Mesh Hero Card with animated pulse rings ──────────────────
                    MeshGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scaleOnPress(0.98f)
                            .clickable { onNavigateToNearby() },
                        cornerRadius = 24.dp,
                        glowColor = primaryColor,
                        glowRadius = 180f
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MESH NETWORK",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AnimatedContent(
                                        targetState = uiState.nearbyDevices.size,
                                        transitionSpec = {
                                            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                                        },
                                        label = "node_count"
                                    ) { count ->
                                        Text(
                                            text = "$count ${if (count == 1) "Node" else "Nodes"} Active",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "BLE & Wi-Fi Direct · multi-hop routing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Animated pulse icon container
                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                val center = Offset(size.width / 2, size.height / 2)
                                                val maxRadius = size.minDimension / 2

                                                // Outer ring
                                                drawCircle(
                                                    color = primaryColor.copy(alpha = pulseRing1Alpha),
                                                    radius = maxRadius * pulseRing1Scale,
                                                    center = center,
                                                    style = Stroke(width = 2.dp.toPx())
                                                )
                                                // Inner ring
                                                drawCircle(
                                                    color = primaryColor.copy(alpha = pulseRing2Alpha),
                                                    radius = maxRadius * pulseRing2Scale,
                                                    center = center,
                                                    style = Stroke(width = 1.5.dp.toPx())
                                                )
                                            }
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Wifi,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MeshSpacing.SM))

                    // ── Search Bar ──────────────────────────────────────────────
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearch = { isSearchActive = false },
                                expanded = isSearchActive,
                                onExpandedChange = { isSearchActive = it },
                                placeholder = {
                                    Text(
                                        "Search chats or nodes...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
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

            // ── Quick Actions Section ──────────────────────────────────────────
            if (!isSearchActive && searchQuery.isBlank()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(MeshSpacing.SectionGap))

                        // Section header: accent bar + title + count pill
                        Row(
                            modifier = Modifier.padding(horizontal = MeshSpacing.ScreenPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ResponsiveDashboardGrid(
                            items = listOf(
                                { cardModifier ->
                                    DashboardCard(
                                        icon = Icons.Default.Wifi,
                                        title = "Nearby",
                                        subtitle = "${uiState.nearbyDevices.size} discovered",
                                        onClick = onNavigateToNearby,
                                        modifier = cardModifier,
                                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        iconTintColor = MaterialTheme.colorScheme.primary,
                                        badgeCount = uiState.nearbyDevices.size,
                                        isActive = uiState.nearbyDevices.isNotEmpty()
                                    )
                                },
                                { cardModifier ->
                                    DashboardCard(
                                        icon = Icons.Default.Campaign,
                                        title = "Broadcast",
                                        subtitle = "Mesh-wide post",
                                        onClick = onNavigateToBroadcast,
                                        modifier = cardModifier,
                                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        iconTintColor = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                { cardModifier ->
                                    DashboardCard(
                                        icon = Icons.Default.Warning,
                                        title = "Emergency",
                                        subtitle = "SOS beacon",
                                        onClick = onNavigateToSos,
                                        modifier = cardModifier,
                                        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        iconTintColor = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // ── Recent Chats Section Header ───────────────────────────────────
            item {
                if (isSearchActive || searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Row(
                    modifier = Modifier.padding(
                        start = MeshSpacing.ScreenPadding,
                        end = MeshSpacing.ScreenPadding,
                        top = MeshSpacing.SectionGap,
                        bottom = MeshSpacing.SectionTitleBottomSpacing
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recent Chats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (filteredChats.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${filteredChats.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── Chat Items or Empty State ─────────────────────────────────────
            if (filteredChats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = if (searchQuery.isNotBlank()) "No results found" else "No recent chats",
                            description = if (searchQuery.isNotBlank()) "Try a different search term." else "Tap Connect to find nearby devices and start chatting offline.",
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
