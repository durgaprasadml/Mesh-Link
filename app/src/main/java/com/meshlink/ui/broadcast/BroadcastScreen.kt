package com.meshlink.ui.broadcast

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.components.UserAvatarImage
import com.meshlink.ui.components.chat.DateSeparator
import com.meshlink.ui.components.chat.DeliveryUiState
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    var messageText by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    val maxChars = 500
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom only when user is already at the bottom
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isAtBottom = visibleItems.isEmpty() ||
                (visibleItems.last().index >= listState.layoutInfo.totalItemsCount - 2)

            if (isAtBottom) {
                listState.animateScrollToItem(uiState.messages.size)
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Mesh-Link Broadcast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Broadcast messages are transmitted across all reachable nearby Mesh-Link peers using Bluetooth Low Energy mesh flooding. No internet, Wi-Fi, or cellular network is required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Broadcast",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.extraSmall)
                        ) {
                            if (uiState.nearbyDevicesCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "${uiState.nearbyDevicesCount} nearby ${if (uiState.nearbyDevicesCount == 1) "peer" else "peers"} active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "All nearby Mesh-Link devices",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Broadcast channel information",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = MeshTheme.elevation.level1,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small)
                ) {
                    // Subtle Info Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MeshTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = MeshTheme.spacing.mediumSmall, vertical = MeshTheme.spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                        Text(
                            text = "Messages are delivered to all nearby Mesh-Link peers",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

                    // Input & Send Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { if (it.length <= maxChars) messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Broadcast message input field" },
                            placeholder = {
                                Text(
                                    text = "Type a broadcast message…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = MeshTheme.shapes.extraLarge,
                            maxLines = 5,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))

                        val canSend = messageText.isNotBlank() && !uiState.isSending
                        IconButton(
                            onClick = {
                                val msg = messageText.trim()
                                if (msg.isNotBlank() && !uiState.isSending) {
                                    viewModel.sendBroadcast(msg)
                                    messageText = ""
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                        ) {
                            if (uiState.isSending) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send broadcast",
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (messageText.length > 400) {
                        Text(
                            text = "${messageText.length}/$maxChars",
                            color = if (messageText.length >= maxChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = MeshTheme.spacing.extraSmall, end = MeshTheme.spacing.small)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent<Boolean>(
            targetState = uiState.messages.isEmpty(),
            label = "broadcast_list_transition"
        ) { isEmpty ->
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Campaign,
                        title = "No broadcasts yet",
                        description = "Broadcast messages are sent directly to all nearby Mesh-Link peers in range."
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        horizontal = MeshTheme.spacing.medium,
                        vertical = MeshTheme.spacing.mediumSmall
                    ),
                    reverseLayout = false
                ) {
                    item(key = "broadcast_channel_intro") {
                        BroadcastChannelIntro()
                    }

                    itemsIndexed(
                        items = uiState.messages,
                        key = { _, item -> item.message.messageId },
                        contentType = { _, _ -> "broadcast_message" }
                    ) { index, uiMsg ->
                        val msg = uiMsg.message
                        val prevMsg = if (index > 0) uiState.messages[index - 1].message else null
                        val nextMsg = if (index < uiState.messages.size - 1) uiState.messages[index + 1].message else null

                        val showDateSeparator = DateTimeUtils.shouldShowDateSeparator(
                            currentTimestamp = msg.timestamp,
                            previousTimestamp = prevMsg?.timestamp
                        )

                        if (showDateSeparator) {
                            DateSeparator(timestamp = msg.timestamp)
                        }

                        val isFirstInGroup = prevMsg == null ||
                            prevMsg.senderId != msg.senderId ||
                            prevMsg.isFromMe != msg.isFromMe ||
                            (msg.timestamp - prevMsg.timestamp > 120_000L) ||
                            showDateSeparator

                        val isLastInGroup = nextMsg == null ||
                            nextMsg.senderId != msg.senderId ||
                            nextMsg.isFromMe != msg.isFromMe ||
                            (nextMsg.timestamp - msg.timestamp > 120_000L)

                        val topSpacing = when {
                            showDateSeparator -> MeshTheme.spacing.extraSmall
                            isFirstInGroup -> MeshTheme.spacing.small
                            else -> 3.dp
                        }

                        Box(modifier = Modifier.padding(top = topSpacing)) {
                            BroadcastBubble(
                                uiMsg = uiMsg,
                                isFirstInGroup = isFirstInGroup,
                                isLastInGroup = isLastInGroup
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastChannelIntro() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MeshTheme.spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(MeshTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.Podcasts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            Text(
                text = "Broadcast Channel",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Messages sent here are transmitted over BLE mesh to all nearby devices in range without cellular or Wi-Fi.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BroadcastBubble(
    uiMsg: BroadcastUiMessage,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean
) {
    val msg = uiMsg.message
    val isMe = msg.isFromMe
    val senderDisplayName = uiMsg.senderName.trim().ifBlank { "Unknown User" }

    val formattedTime = remember(msg.timestamp) {
        DateTimeUtils.formatTimeHHMM(msg.timestamp)
    }

    val semanticDescription = remember(isMe, senderDisplayName, msg.text, formattedTime) {
        buildString {
            if (isMe) append("Your broadcast: ") else append("Broadcast from $senderDisplayName: ")
            append("${msg.text}. Sent at $formattedTime.")
        }
    }

    val cornerRadius = MeshTheme.spacing.mediumLarge
    val smallRadius = 4.dp

    if (isMe) {
        // Outgoing local user message
        val shape = RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = if (isFirstInGroup) cornerRadius else smallRadius,
            bottomStart = cornerRadius,
            bottomEnd = if (isLastInGroup) cornerRadius else smallRadius
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 290.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .semantics(mergeDescendants = true) {
                        contentDescription = semanticDescription
                    }
                    .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small)
            ) {
                Text(
                    text = msg.text,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(MeshTheme.spacing.extraSmall))
                    val deliveryState = DeliveryUiState.fromDomain(msg.status)
                    val (statusIcon, iconTint) = when (deliveryState) {
                        DeliveryUiState.Sending -> Icons.Default.Schedule to MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        DeliveryUiState.Sent -> Icons.Default.Done to MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        DeliveryUiState.Delivered -> Icons.Default.DoneAll to MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        DeliveryUiState.Seen -> Icons.Default.DoneAll to MaterialTheme.colorScheme.primary
                        DeliveryUiState.Failed -> Icons.Default.Error to MaterialTheme.colorScheme.error
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Delivery status: ${msg.status}",
                        modifier = Modifier.size(13.dp),
                        tint = iconTint
                    )
                }
            }
        }
    } else {
        // Incoming peer message
        val shape = RoundedCornerShape(
            topStart = if (isFirstInGroup) cornerRadius else smallRadius,
            topEnd = cornerRadius,
            bottomStart = if (isLastInGroup) cornerRadius else smallRadius,
            bottomEnd = cornerRadius
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (isLastInGroup) {
                UserAvatarImage(
                    meshId = msg.senderId,
                    displayName = senderDisplayName,
                    profilePhotoPath = uiMsg.senderProfilePhotoPath,
                    size = 30.dp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(30.dp))
            }

            Spacer(modifier = Modifier.width(MeshTheme.spacing.small))

            Column(
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 290.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics(mergeDescendants = true) {
                        contentDescription = semanticDescription
                    }
                    .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small)
            ) {
                if (isFirstInGroup) {
                    Text(
                        text = senderDisplayName,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = msg.text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formattedTime,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}
