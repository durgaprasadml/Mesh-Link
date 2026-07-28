package com.meshlink.ui.sos

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    onBack: () -> Unit,
    viewModel: SosViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EMERGENCY SOS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MeshTheme.colors.danger
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = MeshTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.large)
        ) {
            item {
                EmergencyStatusCard(state)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MeshTheme.spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = state.status,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "sos_status_transition"
                    ) { targetStatus ->
                        when (targetStatus) {
                            SosStatus.SAFE, SosStatus.FAILED -> {
                                HoldToActivateButton(
                                    onActivate = { viewModel.sendSos() }
                                )
                            }
                            else -> {
                                ActiveSosState(
                                    state = state,
                                    onCancel = { viewModel.resetSos() }
                                )
                            }
                        }
                    }
                }
            }

            item {
                EmergencyInfoCard(
                    state = state,
                    onRefresh = { viewModel.refreshLocation() }
                )
            }

            item { NearbyResponders(state) }
            item {
                val context = LocalContext.current
                QuickActions(
                    state = state,
                    onCall = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:911")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore or show toast
                        }
                    },
                    onShare = {
                        try {
                            val locationStr = if (state.latitude != null && state.longitude != null) {
                                "My emergency location: https://maps.google.com/?q=${state.latitude},${state.longitude}"
                            } else {
                                "I'm having an emergency, but my location is currently unavailable."
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, locationStr)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Location via"))
                        } catch (e: Exception) {
                            // Ignore or show toast
                        }
                    },
                    onFlashlight = { viewModel.toggleFlashlight() },
                    onAlarm = { viewModel.toggleAlarm() }
                )
            }
            item { SafetyTips() }
        }
    }
}

@Composable
fun EmergencyStatusCard(state: SosUiState) {
    val containerColor = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.surfaceVariant
        SosStatus.BROADCASTING -> MeshTheme.colors.warning.copy(alpha = 0.2f)
        SosStatus.DELIVERED -> MeshTheme.colors.success.copy(alpha = 0.2f)
        SosStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
    }
    
    val contentColor = when (state.status) {
        SosStatus.SAFE -> MaterialTheme.colorScheme.onSurfaceVariant
        SosStatus.BROADCASTING -> MeshTheme.colors.warning
        SosStatus.DELIVERED -> MeshTheme.colors.success
        SosStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
    }
    
    val statusText = when (state.status) {
        SosStatus.SAFE -> "Ready to Broadcast"
        SosStatus.BROADCASTING -> "Broadcasting SOS..."
        SosStatus.DELIVERED -> "SOS Delivered"
        SosStatus.FAILED -> "Broadcast Failed"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.large),
        shape = MeshTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (state.status == SosStatus.SAFE) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = "Active Emergency",
                tint = contentColor,
                modifier = Modifier.size(MeshTheme.spacing.huge)
            )
            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
            Column {
                Text(
                    text = "Current Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun HoldToActivateButton(onActivate: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    var isPressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var countdownValue by remember { mutableIntStateOf(3) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    // Button scale when pressed
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            countdownValue = 3
            progress.snapTo(0f)
            
            // 3-second hold logic
            for (i in 3 downTo 1) {
                countdownValue = i
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                
                // Animate progress for 1 second
                progress.animateTo(
                    targetValue = (4 - i) / 3f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            }
            
            // Activation
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Double pulse for success
            onActivate()
            isPressed = false
        } else {
            // Cancelled
            progress.animateTo(0f, animationSpec = tween(300))
            countdownValue = 3
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics {
            contentDescription = if (isPressed) "Holding to activate. $countdownValue seconds remaining." else "Hold for 3 seconds to send emergency SOS."
            role = Role.Button
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Background breathing glow when not pressed
            if (!isPressed) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(breathingScale)
                        .clip(CircleShape)
                        .background(MeshTheme.colors.danger.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(breathingScale * 1.05f)
                        .clip(CircleShape)
                        .background(MeshTheme.colors.danger.copy(alpha = 0.25f))
                )
            }

            // Progress Ring
            if (isPressed) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(190.dp),
                    color = MeshTheme.colors.danger,
                    strokeWidth = MeshTheme.spacing.mediumSmall,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            // Main Touchable Button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MeshTheme.colors.danger.copy(alpha = 0.8f),
                                MeshTheme.colors.danger
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPressed) {
                    Text(
                        text = "$countdownValue",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Responder Location",
                            modifier = Modifier.size(MeshTheme.spacing.giant),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                        Text(
                            text = "SOS",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
        Text(
            text = if (isPressed) "HOLD TO SEND" else "HOLD FOR 3 SECONDS",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
            color = if (isPressed) MeshTheme.colors.danger else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ActiveSosState(state: SosUiState, onCancel: () -> Unit) {
    val isDelivered = state.status == SosStatus.DELIVERED
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(if (isDelivered) MeshTheme.colors.success else MeshTheme.colors.warning)
                .border(MeshTheme.spacing.mediumSmall, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isDelivered) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Safe User",
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
        
        Text(
            text = if (isDelivered) "SUCCESS" else "BROADCASTING",
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
            color = if (isDelivered) MeshTheme.colors.success else MeshTheme.colors.warning
        )
        
        if (isDelivered) {
            Text(
                text = "Reached ${state.relaysReached} devices in network",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))
        
        TextButton(onClick = onCancel) {
            Text("Cancel / Stop SOS", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun EmergencyInfoCard(state: SosUiState, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.large),
        shape = MeshTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(MeshTheme.spacing.large)) {
            Text("EMERGENCY INFO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Location
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MeshTheme.colors.warning, modifier = Modifier.padding(top = MeshTheme.spacing.extraSmall))
                Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                Column {
                    if (state.isFetchingLocation) {
                        Text("Acquiring GPS fix...", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(modifier = Modifier.padding(top = MeshTheme.spacing.small).width(100.dp))
                    } else if (state.latitude != null && state.longitude != null) {
                        Text(
                            text = "${String.format(Locale.US, "%.5f", state.latitude)}, ${String.format(Locale.US, "%.5f", state.longitude)}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        if (state.address != null) {
                            Text(state.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                    } else {
                        Text("GPS Unavailable", style = MaterialTheme.typography.bodyMedium, color = MeshTheme.colors.warning)
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Grid Info
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(
                    icon = Icons.Default.BatteryStd,
                    title = "Battery",
                    value = "${state.batteryPercent}%",
                    valueColor = if (state.batteryPercent < 20) MeshTheme.colors.danger else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                InfoItem(
                    icon = Icons.Default.Wifi,
                    title = "Mesh Nodes",
                    value = "${state.nearbyResponders.size} nearby",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(
                    icon = Icons.Default.Bluetooth,
                    title = "BLE / WiFi Direct",
                    value = if (state.isBleEnabled && state.isWifiDirectEnabled) "Active" else "Warning",
                    valueColor = if (state.isBleEnabled && state.isWifiDirectEnabled) MeshTheme.colors.success else MeshTheme.colors.warning,
                    modifier = Modifier.weight(1f)
                )
                InfoItem(
                    icon = Icons.Default.Schedule,
                    title = "Timestamp",
                    value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
            
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(MeshTheme.spacing.mediumLarge))
                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                Text("Refresh Data")
            }
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, modifier = Modifier.size(MeshTheme.spacing.large), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = valueColor)
        }
    }
}


@Composable
fun NearbyResponders(state: SosUiState) {
    Column {
        Text(
            "NEARBY RESPONDERS (${state.nearbyResponders.size})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = MeshTheme.spacing.large)
        )
        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
        
        if (state.nearbyResponders.isEmpty()) {
            Text(
                "No responders nearby",
                modifier = Modifier.padding(horizontal = MeshTheme.spacing.large),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = MeshTheme.spacing.large),
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
            ) {
                items(
                    items = state.nearbyResponders,
                    key = { it.address },
                    contentType = { "responder_card" }
                ) { device ->
                    ResponderCard(device = device)
                }
            }
        }
    }
}

@Composable
fun ResponderCard(device: BleDevice) {
    Card(
        shape = MeshTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (device.transport) {
                TransportType.BLE -> Icons.Default.Bluetooth
                TransportType.WIFI_DIRECT -> Icons.Default.Wifi
                TransportType.HYBRID -> Icons.Default.WifiTethering
            }
            Icon(icon, contentDescription = "Responder transport method", tint = MeshTheme.colors.success)
            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
            Column {
                Text(device.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text("Transport: ${device.transport.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun QuickActions(
    state: SosUiState,
    onCall: () -> Unit,
    onShare: () -> Unit,
    onFlashlight: () -> Unit,
    onAlarm: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = MeshTheme.spacing.large)) {
        Text("QUICK ACTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
        
        Row(horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)) {
            ActionChip(icon = Icons.Default.Call, label = "Call 911", modifier = Modifier.weight(1f), isDanger = true, onClick = onCall)
            ActionChip(icon = Icons.Default.ShareLocation, label = "Share Live", modifier = Modifier.weight(1f), onClick = onShare)
        }
        Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
        Row(horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)) {
            ActionChip(
                icon = Icons.Default.FlashlightOn, 
                label = "Flashlight", 
                modifier = Modifier.weight(1f), 
                isActive = state.isFlashlightOn,
                onClick = onFlashlight
            )
            ActionChip(
                icon = Icons.Default.NotificationsActive, 
                label = "Loud Alarm", 
                modifier = Modifier.weight(1f), 
                isActive = state.isAlarmPlaying,
                isDanger = state.isAlarmPlaying,
                onClick = onAlarm
            )
        }
    }
}

@Composable
fun ActionChip(
    icon: ImageVector, 
    label: String, 
    modifier: Modifier = Modifier, 
    isDanger: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isActive && isDanger) MeshTheme.colors.danger.copy(alpha = 0.1f) 
        else if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
        else Color.Transparent
        
    val contentColor = if (isDanger) MeshTheme.colors.danger 
        else if (isActive) MaterialTheme.colorScheme.primary 
        else MaterialTheme.colorScheme.onSurface
        
    val borderColor = if (isDanger) MeshTheme.colors.danger.copy(alpha = 0.5f) 
        else if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
        else MaterialTheme.colorScheme.outline
        
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = MeshTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(MeshTheme.spacing.large))
        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SafetyTips() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.large),
        shape = MeshTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(MeshTheme.spacing.large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = "Information", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                Text("Safety Tips", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = MeshTheme.spacing.medium)) {
                    TipItem("Stay where you are if it is safe to do so.")
                    TipItem("Keep Bluetooth and WiFi enabled to ensure mesh connectivity.")
                    TipItem("Do not close this app; keep it running in the foreground if possible.")
                    TipItem("Conserve battery if help is delayed.")
                }
            }
        }
    }
}

@Composable
fun TipItem(text: String) {
    Row(modifier = Modifier.padding(vertical = MeshTheme.spacing.small)) {
        Text("•", modifier = Modifier.padding(end = MeshTheme.spacing.mediumSmall))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
