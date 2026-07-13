package com.meshlink.ui.sos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Emergency color palette
private val EmergencyRed = Color(0xFFDC2626)
private val DarkEmergency = Color(0xFF1A0000)
private val DeepRed = Color(0xFF7F1D1D)
private val BrightRed = Color(0xFFEF4444)
private val EmergencyOrange = Color(0xFFF97316)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    onBack: () -> Unit,
    viewModel: SosViewModel = hiltViewModel()
) {
    val state by viewModel.sosState.collectAsState()

    // Pulsing animation for SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🚨 EMERGENCY SOS", fontWeight = FontWeight.ExtraBold, color = EmergencyRed)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkEmergency
                )
            )
        },
        containerColor = DarkEmergency
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // GPS Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepRed.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location icon", tint = EmergencyOrange, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GPS Coordinates", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.isFetchingLocation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = EmergencyOrange, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Acquiring GPS fix...", color = Color.White.copy(alpha = 0.7f))
                        }
                    } else if (state.latitude != null && state.longitude != null) {
                        Text(
                            text = "Lat: ${String.format(java.util.Locale.US, "%.6f", state.latitude)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Lng: ${String.format(java.util.Locale.US, "%.6f", state.longitude)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text("⚠️ GPS unavailable — coordinates will send as 0,0", color = EmergencyOrange)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🔋 Battery", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${state.batteryPercent}%",
                                color = if (state.batteryPercent < 20) EmergencyRed else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🕐 Timestamp", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.refreshLocation() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyOrange),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(EmergencyOrange, EmergencyRed))
                        )
                    ) {
                        Text("↻ Refresh Location")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SOS Button (Center)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.sosSent) {
                    // Confirmed state
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓ SENT", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "SOS broadcast sent to all reachable mesh nodes.",
                        color = Color(0xFF4ADE80),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Pulsing SOS button
                    Box(contentAlignment = Alignment.Center) {
                        // Outer pulse ring
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(EmergencyRed.copy(alpha = pulseAlpha * 0.3f))
                                .border(2.dp, EmergencyRed.copy(alpha = pulseAlpha), CircleShape)
                        )

                        // Main button
                        Button(
                            onClick = { viewModel.sendSos() },
                            enabled = !state.isSending,
                            modifier = Modifier.size(160.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmergencyRed,
                                disabledContainerColor = EmergencyRed.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.White)
                                    Text("SOS", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tap to broadcast your emergency\nlocation to all nearby mesh nodes",
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Warning footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DeepRed.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "⚠ SOS broadcasts via multi-hop mesh routing with TTL=15.\nNo internet required. Works fully offline.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
