package com.meshlink.messaging.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.meshlink.ui.components.chat.DateSeparator
import com.meshlink.ui.components.chat.MessageBubble
import com.meshlink.ui.components.chat.MessageComposer
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (uiState.messages.isNotEmpty()) uiState.messages.size - 1 else 0
    )

    var fullscreenMessageId by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.sendDocument(it) }
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { viewModel.sendImage(it) }
        }
    }

    var previousMessageCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && uiState.messages.size > previousMessageCount) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
        previousMessageCount = uiState.messages.size
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.messageId) {
        if (uiState.messages.isNotEmpty()) {
            viewModel.markChatAsRead()
        }
    }

    if (fullscreenMessageId != null) {
        val mediaMessages = uiState.messages.filter { it.messageType == com.meshlink.domain.model.MessageType.IMAGE }
        val initialIndex = mediaMessages.indexOfFirst { it.messageId == fullscreenMessageId }.coerceAtLeast(0)
        
        Dialog(
            onDismissRequest = { fullscreenMessageId = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            MediaViewerScreen(
                mediaMessages = mediaMessages,
                initialIndex = initialIndex,
                onBack = { fullscreenMessageId = null },
                onDelete = { msg ->
                    if (!uiState.selectedMessageIds.contains(msg.messageId)) {
                        viewModel.toggleMessageSelection(msg.messageId)
                    }
                    viewModel.deleteSelectedMessages()
                    fullscreenMessageId = null
                }
            )
        }
    }

    DisposableEffect(viewModel.address) {
        com.meshlink.util.NotificationHelper.setCurrentChatId(viewModel.address)
        onDispose {
            com.meshlink.util.NotificationHelper.setCurrentChatId(null)
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
                onDocumentClick = {
                    showAttachmentSheet = false
                    documentPickerLauncher.launch(arrayOf("*/*"))
                },
                onCameraClick = {
                    showAttachmentSheet = false
                    val dir = File(context.cacheDir, "images")
                    dir.mkdirs()
                    val tempFile = File(dir, "camera_${System.currentTimeMillis()}.jpg")
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                    cameraUri = uri
                    cameraLauncher.launch(uri)
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
                    if (uiState.isSelectionMode) {
                        Text("${uiState.selectedMessageIds.size} selected")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = viewModel.name.firstOrNull()?.uppercase() ?: "?"
                                Text(
                                    text = initial,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                            Column {
                                Text(
                                    text = viewModel.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val statusText = when (uiState.connectionStatus) {
                                    ConnectionState.DIRECT -> "Direct connection"
                                    ConnectionState.RELAY -> "Via mesh (relay)"
                                    ConnectionState.OFFLINE -> "Offline"
                                }
                                val statusColor = when (uiState.connectionStatus) {
                                    ConnectionState.DIRECT -> MaterialTheme.colorScheme.primary
                                    ConnectionState.RELAY -> MaterialTheme.colorScheme.tertiary
                                    ConnectionState.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isSelectionMode) viewModel.clearSelection() else onBack()
                    }) {
                        Icon(
                            if (uiState.isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
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
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isSelectionMode) {
                MessageComposer(
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    isRecording = uiState.isRecording,
                    recordingElapsedMs = uiState.recordingElapsedMs,
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecordingAndSend = { viewModel.stopRecordingAndSend() },
                    onCancelRecording = { viewModel.cancelRecording() },
                    onSendText = {
                        viewModel.sendMessage(it)
                        inputText = ""
                    },
                    onAttachClick = { showAttachmentSheet = true }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp) // tighter spacing between messages, wider spacing handled logic
        ) {
            itemsIndexed(uiState.messages, key = { _, it -> it.messageId }, contentType = { _, _ -> "message_item" }) { index, msg ->
                val showDateSeparator = shouldShowDateSeparator(
                    currentTimestamp = msg.timestamp,
                    previousTimestamp = if (index > 0) uiState.messages[index - 1].timestamp else null
                )

                if (showDateSeparator) {
                    DateSeparator(timestamp = msg.timestamp)
                }

                val isSelected = uiState.selectedMessageIds.contains(msg.messageId)
                val msgTransferProgress = uiState.transferProgress[msg.messageId]

                // Add extra padding if consecutive messages are from different senders
                val previousMsg = if (index > 0) uiState.messages[index - 1] else null
                val extraTopPadding = if (previousMsg != null && previousMsg.isFromMe != msg.isFromMe && !showDateSeparator) 8.dp else 0.dp

                Box(modifier = Modifier.padding(top = extraTopPadding)) {
                    MessageBubble(
                        message = msg,
                        isSelected = isSelected,
                        isSelectionMode = uiState.isSelectionMode,
                        currentlyPlaying = uiState.currentlyPlaying,
                        playbackProgress = uiState.playbackProgress,
                        transferProgress = msgTransferProgress,
                        onToggleSelection = { viewModel.toggleMessageSelection(msg.messageId) },
                        onPlayVoice = { viewModel.playVoice(it) },
                        onStopPlayback = { viewModel.stopPlayback() },
                        onImageClick = { if (!uiState.isSelectionMode) fullscreenMessageId = it },
                        onLocationClick = { lat, lng ->
                            if (!uiState.isSelectionMode) {
                                try {
                                    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Location)")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    }
                                } catch (_: Exception) { /* No map app installed */ }
                            }
                        },
                        onRetryMedia = { viewModel.retryTransfer(it) }
                    )
                }
            }
        }
    }
}

private fun shouldShowDateSeparator(currentTimestamp: Long, previousTimestamp: Long?): Boolean {
    if (previousTimestamp == null) return true
    
    val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    val previousCalendar = Calendar.getInstance().apply { timeInMillis = previousTimestamp }
    
    return currentCalendar.get(Calendar.YEAR) != previousCalendar.get(Calendar.YEAR) ||
           currentCalendar.get(Calendar.DAY_OF_YEAR) != previousCalendar.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun AttachmentMenu(
    onGalleryClick: () -> Unit,
    onDocumentClick: () -> Unit,
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
                color = MaterialTheme.colorScheme.secondary,
                onClick = onGalleryClick
            )
            AttachmentIcon(
                icon = Icons.Default.InsertDriveFile,
                label = "Document",
                color = MaterialTheme.colorScheme.primary,
                onClick = onDocumentClick
            )
            AttachmentIcon(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                color = MaterialTheme.colorScheme.tertiary,
                onClick = onCameraClick
            )
            AttachmentIcon(
                icon = Icons.Default.LocationOn,
                label = "Location",
                color = MaterialTheme.colorScheme.primary,
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
                tint = MaterialTheme.colorScheme.onPrimary,
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
