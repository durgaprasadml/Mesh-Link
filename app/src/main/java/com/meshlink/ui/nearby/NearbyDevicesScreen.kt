package com.meshlink.ui.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.components.PermissionHandler

val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val PrimaryNeonGreen = Color(0xFF00FF88)
val TextPrimary = Color(0xFFFFFFFF)
val CardBackground = Color(0xFF242424)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDevicesScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel()
) {
    PermissionHandler {
        val devices by viewModel.nearbyDevices.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.startDiscovery()
        }

        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "Nearby Devices",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radar Scanning Animation
                RadarScanner(modifier = Modifier.padding(vertical = 32.dp))
                
                // Device List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.meshId }) { device ->
                        DeviceCard(device = device, onClick = {
                            onNavigateToChat(
                                device.meshId.ifBlank { device.address },
                                device.name.ifBlank { device.address.takeLast(8) }
                            )
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun RadarScanner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    
    // Scale animation for the expanding rings
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale"
    )

    // Alpha animation for fading out as rings expand
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha"
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(PrimaryNeonGreen.copy(alpha = alpha))
        )
        
        // Secondary ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale * 0.6f)
                .clip(CircleShape)
                .background(PrimaryNeonGreen.copy(alpha = (alpha * 1.5f).coerceAtMost(1f)))
        )
        
        // Center static dot
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PrimaryNeonGreen)
        )
    }
}

@Composable
fun DeviceCard(device: BleDevice, onClick: () -> Unit) {
    // Assuming RSSI > -70 is strong/connected visually
    val isStrongSignal = device.rssi > -70
    val statusColor = if (isStrongSignal) PrimaryNeonGreen else TextPrimary
    val displayName = device.name.ifBlank { "Unknown Device" }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            val initials = displayName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
            
            Text(
                text = initials.ifBlank { "?" },
                color = PrimaryNeonGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Device Name
        Text(
            text = displayName,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        
        // Signal and Status
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isStrongSignal) Icons.Default.Wifi else Icons.Default.SignalCellular4Bar,
                    contentDescription = "Signal",
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isStrongSignal) "Connected" else "Available",
                    color = statusColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
