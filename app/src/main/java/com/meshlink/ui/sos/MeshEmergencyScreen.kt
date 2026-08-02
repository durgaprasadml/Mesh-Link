package com.meshlink.ui.sos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshTheme

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
    var isSheetOpen by remember { mutableStateOf(false) }

    val emergencyUiState = remember(state) {
        EmergencyUiState(rawState = state)
    }

    // Call 112 intent handler
    val handleCall112 = remember(context) {
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

    // Share location intent handler
    val handleShareLocation = remember(context, state.latitude, state.longitude) {
        {
            try {
                val locationMsg = if (state.latitude != null && state.longitude != null) {
                    "EMERGENCY ALERT: My location is https://maps.google.com/?q=${state.latitude},${state.longitude}"
                } else {
                    "EMERGENCY ALERT: Requesting urgent assistance via Mesh-Link."
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, locationMsg)
                }
                context.startActivity(Intent.createChooser(intent, "Share Distress Location"))
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to share location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Open map intent handler
    val handleOpenMap = remember(context) {
        { lat: Double, lon: Double ->
            try {
                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Emergency+Location)")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Map application unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Copy text handler
    val handleCopyCoordinates = remember(context) {
        { coords: String ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Emergency Coordinates", coords)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    // Error Dialog
    AnimatedErrorDialog(
        visible = state.errorMessage != null,
        title = "Emergency Action Error",
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
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isSheetOpen = true },
                icon = { Icon(Icons.Default.Menu, contentDescription = "Emergency Actions") },
                text = { Text("Actions Sheet") },
                containerColor = MeshTheme.colors.danger,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ambient Danger Glow Background
            DangerGlowBackground(isActive = state.status == SosStatus.BROADCASTING || state.status == SosStatus.FAILED)

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
                        // Left Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
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

                        // Right Column
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                EmergencyLocationCard(
                                    locationUi = emergencyUiState.locationUi,
                                    onRefreshLocation = onRefreshLocation,
                                    onOpenMap = handleOpenMap,
                                    onCopyCoordinates = { handleCopyCoordinates(it) }
                                )
                            }
                            item {
                                EmergencyContacts(contacts = emergencyUiState.contactsUi)
                            }
                            item {
                                EmergencyProgress(
                                    deliveryUi = emergencyUiState.deliveryUi,
                                    statistics = emergencyUiState.statistics
                                )
                            }
                            item {
                                EmergencyTimeline(state = state)
                            }
                            item {
                                EmergencyDiagnostics(state = state)
                            }
                        }
                    }
                } else {
                    // Standard Mobile Single Column Feed
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            EmergencyStatusCard(
                                state = state,
                                onRetry = onSendSos,
                                onCancel = onResetSos
                            )
                        }
                        item {
                            EmergencyLocationCard(
                                locationUi = emergencyUiState.locationUi,
                                onRefreshLocation = onRefreshLocation,
                                onOpenMap = handleOpenMap,
                                onCopyCoordinates = { handleCopyCoordinates(it) }
                            )
                        }
                        item {
                            EmergencyContacts(contacts = emergencyUiState.contactsUi)
                        }
                        item {
                            EmergencyProgress(
                                deliveryUi = emergencyUiState.deliveryUi,
                                statistics = emergencyUiState.statistics
                            )
                        }
                        item {
                            EmergencyTimeline(state = state)
                        }
                        item {
                            EmergencyDiagnostics(state = state)
                        }
                    }
                }
            }
        }
    }

    // Modal Action Sheet
    if (isSheetOpen) {
        EmergencyBottomSheet(
            state = state,
            onResend = onSendSos,
            onCancel = {
                onResetSos()
                isSheetOpen = false
            },
            onCopyCoordinates = {
                if (state.latitude != null && state.longitude != null) {
                    handleCopyCoordinates("${state.latitude}, ${state.longitude}")
                }
            },
            onShareLocation = handleShareLocation,
            onCallEmergency = handleCall112,
            onToggleFlashlight = onToggleFlashlight,
            onToggleAlarm = onToggleAlarm,
            onDismiss = { isSheetOpen = false }
        )
    }
}
