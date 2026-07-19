package com.meshlink.ui.sos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    onBack: () -> Unit,
    viewModel: SosViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Pulsing animation for SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(MeshTheme.animations.normal * 2, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(MeshTheme.animations.normal * 2, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🚨 EMERGENCY SOS", style = MaterialTheme.typography.titleLarge, color = MeshTheme.colors.danger)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(MeshTheme.spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // GPS Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MeshTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(MeshTheme.spacing.large)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location icon", tint = MeshTheme.colors.warning, modifier = Modifier.size(MeshTheme.spacing.extraLarge))
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                        Text("GPS Coordinates", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                    if (state.isFetchingLocation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(MeshTheme.spacing.mediumLarge), color = MeshTheme.colors.warning, strokeWidth = MeshTheme.spacing.extraSmall)
                            Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                            Text("Acquiring GPS fix...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                        }
                    } else if (state.latitude != null && state.longitude != null) {
                        Text(
                            text = "Lat: ${String.format(java.util.Locale.US, "%.6f", state.latitude)}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Lng: ${String.format(java.util.Locale.US, "%.6f", state.longitude)}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text("⚠️ GPS unavailable — coordinates will send as 0,0", style = MaterialTheme.typography.bodyMedium, color = MeshTheme.colors.warning)
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🔋 Battery", color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${state.batteryPercent}%",
                                color = if (state.batteryPercent < 20) MeshTheme.colors.danger else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🕐 Timestamp", color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                    OutlinedButton(
                        onClick = { viewModel.refreshLocation() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MeshTheme.colors.warning)
                    ) {
                        Text("↻ Refresh Location", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))

            // SOS Button (Center)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.sosSent) {
                    // Confirmed state
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(MeshTheme.colors.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓ SENT", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                    Text(
                        "SOS broadcast sent to all reachable mesh nodes.",
                        color = MeshTheme.colors.success,
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
                                .background(MeshTheme.colors.danger.copy(alpha = pulseAlpha * 0.3f))
                                .border(MeshTheme.spacing.extraSmall, MeshTheme.colors.danger.copy(alpha = pulseAlpha), CircleShape)
                        )

                        // Main button
                        Button(
                            onClick = { viewModel.sendSos() },
                            enabled = !state.isSending,
                            modifier = Modifier.size(160.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MeshTheme.colors.danger,
                                disabledContainerColor = MeshTheme.colors.danger.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = MeshTheme.elevation.level5)
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(MeshTheme.spacing.extraHuge))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(MeshTheme.spacing.extraHuge), tint = MaterialTheme.colorScheme.onPrimary)
                                    Text("SOS", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                    Text(
                        "Tap to broadcast your emergency\nlocation to all nearby mesh nodes",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))

            // Warning footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MeshTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "⚠ SOS broadcasts via multi-hop mesh routing with TTL=15.\nNo internet required. Works fully offline.",
                    modifier = Modifier.padding(MeshTheme.spacing.mediumLarge),
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
