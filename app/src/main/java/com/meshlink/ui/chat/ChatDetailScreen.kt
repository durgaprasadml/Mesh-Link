package com.meshlink.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meshlink.data.local.DeliveryStatus
import com.meshlink.data.local.MessageEntity
import com.meshlink.data.local.MessageType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingElapsedMs by viewModel.recordingElapsedMs.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()

    val selectedMessageIds by viewModel.selectedMessageIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    var inputText by remember { mutableStateOf("") }
    // FIX Issue 2: Initialize list at the bottom so chat opens at latest messages
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (messages.isNotEmpty()) messages.size - 1 else 0
    )
    val context = LocalContext.current

    // Fullscreen image viewer state
    var fullscreenImagePath by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    var showAttachmentSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }

    // Audio permission launcher
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.startRecording()
    }

    // FIX Issue 2: Only auto-scroll on NEW messages (not on initial load)
    var previousMessageCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && messages.size > previousMessageCount) {
            listState.animateScrollToItem(messages.size - 1)
        }
        previousMessageCount = messages.size
    }

    LaunchedEffect(messages.lastOrNull()?.localId) {
        if (messages.isNotEmpty()) {
            viewModel.markChatAsRead()
        }
    }

    // Fullscreen image dialog
    if (fullscreenImagePath != null) {
        Dialog(onDismissRequest = { fullscreenImagePath = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullscreenImagePath = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(fullscreenImagePath!!),
                    contentDescription = "Full Image",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AttachmentMenu(
                onGalleryClick = {
                    showAttachmentSheet = false
                    imagePickerLauncher.launch("image/*")
                },
                onCameraClick = {
                    showAttachmentSheet = false
                    // TODO: Wire camera intent
                },
                onLocationClick = {
                    showAttachmentSheet = false
                    viewModel.sendLocation()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedMessageIds.size} selected")
                    } else {
                        Column {
                            Text(viewModel.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            val statusText = when (connectionStatus) {
                                ChatDetailViewModel.ConnectionState.DIRECT -> "Direct connection"
                                ChatDetailViewModel.ConnectionState.RELAY -> "Via mesh (relay)"
                                ChatDetailViewModel.ConnectionState.OFFLINE -> "Offline"
                            }
                            val statusColor = when (connectionStatus) {
                                ChatDetailViewModel.ConnectionState.DIRECT -> Color(0xFF00FF88)
                                ChatDetailViewModel.ConnectionState.RELAY -> Color(0xFFFFC107)
                                ChatDetailViewModel.ConnectionState.OFFLINE -> Color(0xFFAAAAAA)
                            }
                            Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) viewModel.clearSelection() else onBack()
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.deleteSelectedMessages() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete Chat") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.deleteChat()
                                        onBack()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isSelectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    if (isRecording) {
                        // Recording indicator bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Recording... ${recordingElapsedMs / 1000}s / 10s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.cancelRecording() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { viewModel.stopRecordingAndSend() }) {
                                Icon(Icons.Default.Send, contentDescription = "Send Voice", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        // Normal input bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            IconButton(
                                onClick = { showAttachmentSheet = true },
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                placeholder = { Text("Type a message...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            if (inputText.isBlank()) {
                                IconButton(
                                    onClick = {
                                        if (hasAudioPermission) {
                                            viewModel.startRecording()
                                        } else {
                                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Record Voice", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    },
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.localId }) { msg ->
                val isSelected = selectedMessageIds.contains(msg.messageId)
                val msgTransferProgress = transferProgress[msg.messageId]

                MessageBubble(
                    message = msg,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    currentlyPlaying = currentlyPlaying,
                    playbackProgress = playbackProgress,
                    transferProgress = msgTransferProgress,
                    onToggleSelection = { viewModel.toggleMessageSelection(msg.messageId) },
                    onPlayVoice = { viewModel.playVoice(it) },
                    onStopPlayback = { viewModel.stopPlayback() },
                    onImageClick = { if (!isSelectionMode) fullscreenImagePath = it },
                    onLocationClick = { lat, lng ->
                        if (!isSelectionMode) {
                            try {
                                val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Location)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                }
                            } catch (_: Exception) { /* No map app installed — ignore */ }
                        }
                    },
                    onRetryMedia = { viewModel.retryTransfer(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    currentlyPlaying: String?,
    playbackProgress: Float,
    transferProgress: Float?,
    onToggleSelection: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onImageClick: (String) -> Unit,
    onLocationClick: (Double, Double) -> Unit,
    onRetryMedia: (String) -> Unit
) {
    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            isMe -> Color(0xFF1B3D2B) // BubbleSent
            else -> Color(0xFF2A2A2A) // BubbleReceived
        }, label = "bubbleColor"
    )
    val textColor = Color(0xFFFFFFFF)
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() },
                onLongClick = { onToggleSelection() }
            ),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .padding(8.dp)
                .widthIn(max = 280.dp)
                .animateContentSize()
        ) {
            when (message.messageType) {
                MessageType.IMAGE -> {
                    val mediaPath = message.mediaPath
                    if (mediaPath != null && File(mediaPath).exists()) {
                        AsyncImage(
                            model = File(mediaPath),
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(mediaPath) },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (message.isFromMe) "Sending image..." else "Receiving image...", style = MaterialTheme.typography.labelSmall)
                                if (message.status == DeliveryStatus.FAILED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IconButton(
                                        onClick = { onRetryMedia(message.messageId) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.error, CircleShape).size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                                    }
                                } else if (transferProgress != null && transferProgress >= 0f) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { transferProgress },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                MessageType.VOICE -> {
                    val mediaPath = message.mediaPath
                    val fileExists = mediaPath != null && File(mediaPath).exists()
                    val isThisPlaying = currentlyPlaying == mediaPath
                    val durationText = message.mediaDurationMs?.let { "${it / 1000}s" } ?: ""

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (fileExists && mediaPath != null) {
                                    if (isThisPlaying) onStopPlayback() else onPlayVoice(mediaPath)
                                }
                            },
                            enabled = fileExists && !isSelectionMode,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isThisPlaying) "Stop" else "Play",
                                tint = if (fileExists) textColor else textColor.copy(alpha = 0.3f)
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            if (message.status == DeliveryStatus.FAILED) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onRetryMedia(message.messageId) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Failed. Tap to retry.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            } else if (transferProgress != null && transferProgress >= 0f && message.status == DeliveryStatus.PENDING) {
                                // Show transfer progress instead of playback progress during transfer
                                Text(
                                    text = if (message.isFromMe) "Sending..." else "Receiving...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { transferProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = textColor.copy(alpha = 0.3f)
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { if (isThisPlaying) playbackProgress else 0f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = textColor.copy(alpha = 0.8f),
                                    trackColor = textColor.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (fileExists) "🎤 $durationText" else "🎤 File missing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                MessageType.LOCATION -> {
                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable(enabled = !isSelectionMode) {
                                val lat = message.latitude
                                val lng = message.longitude
                                if (lat != null && lng != null) {
                                    onLocationClick(lat, lng)
                                }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📍 Location Shared", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (message.latitude != null && message.longitude != null) {
                            Text("Lat: ${String.format("%.6f", message.latitude)}", color = textColor, style = MaterialTheme.typography.bodySmall)
                            Text("Lng: ${String.format("%.6f", message.longitude)}", color = textColor, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap to open in Maps", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                        }
                        if (message.batteryPercent != null && message.batteryPercent >= 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("🔋 ${message.batteryPercent}%", color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                MessageType.SOS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDC2626))
                            .padding(12.dp)
                    ) {
                        Text("🚨 SOS EMERGENCY", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (message.latitude != null && message.longitude != null) {
                            Text("📍 Lat: ${String.format("%.6f", message.latitude)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Text("📍 Lng: ${String.format("%.6f", message.longitude)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        if (message.batteryPercent != null && message.batteryPercent >= 0) {
                            Text("🔋 Battery: ${message.batteryPercent}%", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                MessageType.TEXT -> {
                    Text(text = message.text, color = textColor, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }

            // Timestamp + status row
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val statusIcon = when (message.status) {
                        DeliveryStatus.PENDING -> Icons.Default.Info
                        DeliveryStatus.SENT -> Icons.Default.Check
                        DeliveryStatus.RELAYED -> Icons.Default.DoneAll
                        DeliveryStatus.DELIVERED -> Icons.Default.DoneAll
                        DeliveryStatus.SEEN -> Icons.Default.DoneAll
                        DeliveryStatus.FAILED -> Icons.Default.Warning
                    }
                    val iconTint = when (message.status) {
                        DeliveryStatus.SEEN -> Color(0xFF34B7F1) // WhatsApp blue
                        DeliveryStatus.FAILED -> Color.Red
                        else -> textColor.copy(alpha = 0.8f)
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Status",
                        modifier = Modifier.size(14.dp),
                        tint = iconTint
                    )
                }
            }
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}

@Composable
fun AttachmentMenu(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentIcon(
                icon = Icons.Default.InsertPhoto,
                label = "Gallery",
                color = Color(0xFFAC54F1),
                onClick = onGalleryClick
            )
            AttachmentIcon(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                color = Color(0xFFF15469),
                onClick = onCameraClick
            )
            AttachmentIcon(
                icon = Icons.Default.LocationOn,
                label = "Location",
                color = Color(0xFF4CAF50),
                onClick = onLocationClick
            )
        }
    }
}

@Composable
fun AttachmentIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
