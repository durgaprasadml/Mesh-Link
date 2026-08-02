package com.meshlink.ui.sos

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.MeshScreen

/**
 * Production Ready Emergency SOS & Safety Experience Screen.
 * Implements M3 layout, adaptive viewports, hold-to-confirm, and safety diagnostic components.
 */
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

    val locationUiState = remember(state) {
        EmergencyLocationUi(
            latitude = state.latitude,
            longitude = state.longitude,
            address = state.address,
            isFetching = state.isFetchingLocation,
            batteryPercent = state.batteryPercent
        )
    }

    // Dial 112 / Emergency Services handler
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

    // Share location / Alert contacts handler
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

    // Open map handler
    val handleOpenMap = remember(context) {
        { lat: Double, lng: Double ->
            try {
                val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Emergency Location)")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                context.startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Map application unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Copy location handler
    val handleCopyLocation = remember(context) {
        { coords: String ->
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Emergency Location", coords)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Location copied to clipboard", Toast.LENGTH_SHORT).show()
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
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EmergencyTopBar(
                state = state,
                onBack = onBack,
                onOpenSafetyTips = { isSafetyTipsOpen = true },
                onOpenHistory = {},
                onMoreClick = { isSafetyTipsOpen = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 600.dp

                if (isWide) {
                    // Wide / Tablet / Foldable Two-Pane Layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Pane (Hero, SOS Button, Emergency Status)
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
                            EmergencyStatusCard(
                                state = state,
                                onRetry = onSendSos,
                                onCancel = onResetSos
                            )
                        }

                        // Right Pane (Quick Actions, Location, Responders, History, Diagnostics)
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
                                EmergencyLocationCard(
                                    locationUi = locationUiState,
                                    onRefreshLocation = onRefreshLocation,
                                    onOpenMap = handleOpenMap,
                                    onCopyCoordinates = { handleCopyLocation(it) }
                                )
                            }
                            item {
                                EmergencyResponders(
                                    responders = state.nearbyResponders,
                                    onChatWithResponder = {}
                                )
                            }
                            item {
                                EmergencyTimeline(
                                    state = state,
                                    onLearnMore = { isSafetyTipsOpen = true }
                                )
                            }
                            item {
                                EmergencyDiagnostics(state = state)
                            }
                        }
                    }
                } else {
                    // Mobile Single Column Layout
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
                            EmergencyLocationCard(
                                locationUi = locationUiState,
                                onRefreshLocation = onRefreshLocation,
                                onOpenMap = handleOpenMap,
                                onCopyCoordinates = { handleCopyLocation(it) }
                            )
                        }
                        item {
                            EmergencyResponders(
                                responders = state.nearbyResponders,
                                onChatWithResponder = {}
                            )
                        }
                        item {
                            EmergencyTimeline(
                                state = state,
                                onLearnMore = { isSafetyTipsOpen = true }
                            )
                        }
                        item {
                            EmergencyDiagnostics(state = state)
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
