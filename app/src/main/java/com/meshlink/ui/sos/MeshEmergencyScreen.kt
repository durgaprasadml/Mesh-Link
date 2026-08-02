package com.meshlink.ui.sos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.MeshScreen

@Composable
fun MeshEmergencyScreen(
    state: SosUiState,
    onBack: () -> Unit,
    onSendSos: () -> Unit,
    onResetSos: () -> Unit,
    onRefreshLocation: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleAlarm: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSafetyTipsOpen by remember { mutableStateOf(false) }

    val emergencyUiState = remember(state) {
        EmergencyUiState(rawState = state)
    }

    // Call 112 / Emergency intent handler
    val handleCallEmergency = remember(context) {
        {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:112")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Emergency dialer unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Share location / Alert contacts intent handler
    val handleAlertContacts = remember(context, state.latitude, state.longitude) {
        {
            try {
                val locationMsg = if (state.latitude != null && state.longitude != null) {
                    "EMERGENCY ALERT: Requesting urgent assistance. Location: https://maps.google.com/?q=${state.latitude},${state.longitude}"
                } else {
                    "EMERGENCY ALERT: Requesting urgent assistance via Mesh-Link."
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, locationMsg)
                }
                context.startActivity(Intent.createChooser(intent, "Notify Emergency Contacts"))
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to alert contacts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Error Dialog
    AnimatedErrorDialog(
        visible = state.errorMessage != null,
        title = "Emergency Error",
        message = state.errorMessage ?: "",
        onDismiss = onDismissError,
        primaryButtonText = "OK",
        onPrimaryClick = onDismissError
    )

    MeshScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EmergencyTopBar(
                state = state,
                onBack = onBack,
                onOpenSafetyTips = { isSafetyTipsOpen = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 600.dp

                if (isWide) {
                    // Wide / Tablet / Foldable 2-Column Grid
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column (Hero & Primary SOS Action Button)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            EmergencyHero(uiState = emergencyUiState)
                            EmergencyButton(
                                state = state,
                                onActivate = onSendSos,
                                onCancel = onResetSos
                            )
                        }

                        // Right Column (Information Cards & History)
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                EmergencyQuickActions(
                                    onCallEmergency = handleCallEmergency,
                                    onAlertContacts = handleAlertContacts,
                                    onBroadcastSos = onSendSos
                                )
                            }
                            item {
                                EmergencyStatusCard(
                                    state = state,
                                    onRetry = onSendSos,
                                    onCancel = onResetSos
                                )
                            }
                            item {
                                EmergencyDiagnostics(state = state)
                            }
                            item {
                                EmergencyTimeline(state = state)
                            }
                        }
                    }
                } else {
                    // Mobile Single Column Feed
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            EmergencyHero(uiState = emergencyUiState)
                        }
                        item {
                            EmergencyButton(
                                state = state,
                                onActivate = onSendSos,
                                onCancel = onResetSos
                            )
                        }
                        item {
                            EmergencyQuickActions(
                                onCallEmergency = handleCallEmergency,
                                onAlertContacts = handleAlertContacts,
                                onBroadcastSos = onSendSos
                            )
                        }
                        item {
                            EmergencyStatusCard(
                                state = state,
                                onRetry = onSendSos,
                                onCancel = onResetSos
                            )
                        }
                        item {
                            EmergencyDiagnostics(state = state)
                        }
                        item {
                            EmergencyTimeline(state = state)
                        }
                    }
                }
            }
        }
    }

    // Modal Safety Tips Sheet
    if (isSafetyTipsOpen) {
        EmergencyBottomSheet(
            state = state,
            onResend = onSendSos,
            onCancel = {
                onResetSos()
                isSafetyTipsOpen = false
            },
            onCopyCoordinates = {},
            onShareLocation = handleAlertContacts,
            onCallEmergency = handleCallEmergency,
            onToggleFlashlight = onToggleFlashlight,
            onToggleAlarm = onToggleAlarm,
            onDismiss = { isSafetyTipsOpen = false }
        )
    }
}

