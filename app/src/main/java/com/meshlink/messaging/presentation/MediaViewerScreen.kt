package com.meshlink.messaging.presentation

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.meshlink.domain.model.Message
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaMessages: List<Message>,
    initialIndex: Int,
    transferProgress: Map<String, Float> = emptyMap(),
    onBack: () -> Unit,
    onDelete: (Message) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { mediaMessages.size }
    var showControls by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val message = mediaMessages[page]
            val msgTransferProgress = transferProgress[message.messageId]

            ZoomableImageWithFallback(
                message = message,
                transferProgress = msgTransferProgress,
                onTap = { showControls = !showControls }
            )
        }

        // Subtle top progress indicator for the currently visible message
        val currentMessage = mediaMessages.getOrNull(pagerState.currentPage)
        val currentProgress = currentMessage?.let { transferProgress[it.messageId] }
        val currentFilePath = currentMessage?.mediaPath
        val currentFileExists = remember(currentFilePath) {
            currentFilePath != null && File(currentFilePath).exists()
        }
        // Show progress strip only while original is still in-flight
        val showProgress = !currentFileExists && currentProgress != null && currentProgress >= 0f
        if (showProgress) {
            if (currentProgress!! < 1f) {
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} / ${mediaMessages.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentMessage != null) {
                        val filePath = currentMessage.mediaPath
                        val fileExists = remember(filePath) {
                            filePath != null && File(filePath).exists()
                        }
                        // Share only available once original is on disk
                        if (fileExists && filePath != null) {
                            IconButton(onClick = {
                                val file = File(filePath)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                        IconButton(onClick = {
                            onDelete(currentMessage)
                            if (mediaMessages.size <= 1) onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    }
}

/**
 * Zoomable image that immediately shows the thumbnail preview (from [message.thumbnailBase64])
 * and automatically upgrades to the full-resolution original once [message.mediaPath] points to
 * a valid on-disk file.
 *
 * The transition is handled transparently by Coil: different model keys mean Coil loads the new
 * source and crossfades into it without any manual intervention or screen reload.
 *
 * STATE A / B — original in flight:  thumbnail bitmap displayed immediately, no blank screen.
 * STATE C     — original complete:   Coil loads File(mediaPath), crossfades from thumbnail.
 * STATE D     — future opens:        File is cached by Coil + OS; instant load.
 */
@Composable
fun ZoomableImageWithFallback(
    message: Message,
    transferProgress: Float?,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val filePath = message.mediaPath
    // Re-key on status so recomposition fires when transfer completes
    val fileExists = remember(filePath, message.status) {
        filePath != null && File(filePath).exists()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale == 1f) {
                        offset = Offset.Zero
                    } else {
                        val maxX = (size.width * (scale - 1)) / 2
                        val maxY = (size.height * (scale - 1)) / 2
                        offset = Offset(
                            x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                        )
                    }
                }
            }
    ) {
        if (fileExists && filePath != null) {
            // Original is on disk — Coil loads it with disk cache; future opens are instant.
            // crossfade(true) provides a smooth visual transition from thumbnail to original.
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(filePath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Full resolution image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else if (!message.thumbnailBase64.isNullOrEmpty()) {
            // Original not ready — decode thumbnail on IO and display immediately.
            // No blank screen, no spinner blocking the image.
            val thumbnailBitmapState = remember(message.thumbnailBase64) {
                mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
            }
            LaunchedEffect(message.thumbnailBase64) {
                if (!message.thumbnailBase64.isNullOrEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val bytes = Base64.decode(message.thumbnailBase64, Base64.DEFAULT)
                            thumbnailBitmapState.value =
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (_: Exception) { /* ignore corrupt thumbnail */ }
                    }
                }
            }

            val bitmap = thumbnailBitmapState.value
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "Image preview — full quality loading",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                // Thumbnail still decoding on IO — show a minimal centered indicator
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            // No thumbnail and no file — unlikely in practice but handled gracefully
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// Legacy entry-point kept for any callers that still use the old ZoomableImage signature.
// Internally delegates to ZoomableImageWithFallback with no thumbnail.
@Composable
fun ZoomableImage(filePath: String, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero } else { scale = 2.5f }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale == 1f) {
                        offset = Offset.Zero
                    } else {
                        val maxX = (size.width * (scale - 1)) / 2
                        val maxY = (size.height * (scale - 1)) / 2
                        offset = Offset(
                            x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                        )
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(filePath))
                .crossfade(true)
                .build(),
            contentDescription = "Zoomable Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
