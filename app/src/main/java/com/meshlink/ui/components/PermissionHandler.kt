package com.meshlink.ui.components

import com.meshlink.ui.designsystem.theme.MeshTheme
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

@Composable
fun rememberRadioAndPermissionState(context: Context = LocalContext.current): Boolean {
    var isReady by remember { mutableStateOf(areRadiosAndPermissionsReady(context)) }

    // Real-time broadcast listener for immediate Bluetooth & Wi-Fi radio state changes
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(android.location.LocationManager.MODE_CHANGED_ACTION)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action
                val isBtReady = if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    btState == BluetoothAdapter.STATE_ON
                } else {
                    isBluetoothEnabled(context)
                }

                val isWifiReady = if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    wifiState == WifiManager.WIFI_STATE_ENABLED
                } else {
                    isWifiEnabled(context)
                }

                isReady = hasRequiredPermissions(context) && isBtReady && isWifiReady
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Re-check state on ON_RESUME when returning from Android System Settings or Dialogs
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isReady = areRadiosAndPermissionsReady(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return isReady
}

@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit = {}
) {
    val context = LocalContext.current

    var hasPermissions by remember { mutableStateOf(hasRequiredPermissions(context)) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var isBluetoothEnabled by remember { mutableStateOf(isBluetoothEnabled(context)) }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    var isLocationEnabled by remember {
        mutableStateOf(
            locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        )
    }

    var isWifiEnabled by remember { mutableStateOf(isWifiEnabled(context)) }

    // Real-time broadcast listener for Bluetooth, Wi-Fi, and Location changes
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(android.location.LocationManager.MODE_CHANGED_ACTION)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    isBluetoothEnabled = when (btState) {
                        BluetoothAdapter.STATE_ON -> true
                        BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> false
                        else -> isBluetoothEnabled(context)
                    }
                } else if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    isWifiEnabled = when (wifiState) {
                        WifiManager.WIFI_STATE_ENABLED -> true
                        WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> false
                        else -> isWifiEnabled(context)
                    }
                } else if (action == android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION) {
                    val p2pState = intent.getIntExtra(android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (p2pState == android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                        isWifiEnabled = isWifiEnabled(context)
                    } else if (p2pState == android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        isWifiEnabled = true
                    }
                } else if (action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION || action == android.location.LocationManager.MODE_CHANGED_ACTION) {
                    isLocationEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                                        locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                }
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Re-check state on ON_RESUME when returning from Android System Settings or Dialogs
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermissions = hasRequiredPermissions(context)
                isBluetoothEnabled = isBluetoothEnabled(context)
                isWifiEnabled = isWifiEnabled(context)
                isLocationEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                                    locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = hasRequiredPermissions(context)
        if (!hasPermissions) {
            val activity = context.findActivity()
            if (activity != null) {
                permanentlyDenied = results.filter { !it.value }.keys.any { permission ->
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                }
            } else {
                permanentlyDenied = true
            }
        } else {
            permanentlyDenied = false
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermissions = hasRequiredPermissions(context)
        if (hasPermissions) {
            permanentlyDenied = false
        }
    }

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isBluetoothEnabled = isBluetoothEnabled(context)
    }

    val locationEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isLocationEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                            locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
    }

    val wifiEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isWifiEnabled = isWifiEnabled(context)
    }

    val isLocationHardwareRequired = Build.VERSION.SDK_INT < Build.VERSION_CODES.S

    if (!hasPermissions) {
        GrantPermissionsScreen(
            context = context,
            permanentlyDenied = permanentlyDenied,
            onGrantPermissions = { permissionLauncher.launch(permissionsToRequest) },
            onOpenSettings = {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                settingsLauncher.launch(intent)
            }
        )
    } else if (isLocationHardwareRequired && !isLocationEnabled) {
        LocationRequirementSetupScreen(
            onTurnOnLocation = {
                val enableLocationIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                locationEnableLauncher.launch(enableLocationIntent)
            }
        )
    } else if (!isBluetoothEnabled || !isWifiEnabled) {
        RadioRequirementSetupScreen(
            isBluetoothEnabled = isBluetoothEnabled,
            isWifiEnabled = isWifiEnabled,
            onTurnOnBluetooth = {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(enableBtIntent)
            },
            onTurnOnWifi = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(android.provider.Settings.Panel.ACTION_WIFI)
                } else {
                    Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                }
                try {
                    wifiEnableLauncher.launch(intent)
                } catch (_: Exception) {
                    val fallbackIntent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    wifiEnableLauncher.launch(fallbackIntent)
                }
            }
        )
    } else {
        LaunchedEffect(Unit) {
            onPermissionsGranted()
        }
    }
}

/**
 * High-visibility, modern Grant Permissions screen adhering to the Mesh-Link design system.
 * Displays clear heading, explanatory description, granular permission status cards,
 * and high-contrast action button across Light, Dark, AMOLED, and High-Contrast modes.
 */
@Composable
private fun GrantPermissionsScreen(
    context: Context,
    permanentlyDenied: Boolean,
    onGrantPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffset = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        launch {
            entranceAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
        launch {
            entranceOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
    }

    val isBtGranted = isBluetoothPermissionGranted(context)
    val isLocGranted = isLocationPermissionGranted(context)
    val isNearbyWifiGranted = isNearbyWifiPermissionGranted(context)
    val isNotifGranted = isNotificationPermissionGranted(context)

    // Interactive button press scale state
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "perm_btn_scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeshTheme.spacing.large, vertical = MeshTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

            // 1. Hero 3D Icon Badge Area
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        scaleX = 0.92f + (0.08f * entranceAlpha.value)
                        scaleY = 0.92f + (0.08f * entranceAlpha.value)
                    }
                    .padding(top = MeshTheme.spacing.small, bottom = MeshTheme.spacing.large)
            ) {
                HeroPermissionIconCluster(permanentlyDenied = permanentlyDenied)
            }

            // 2. Main Title (fade + slide up)
            Text(
                text = if (permanentlyDenied) "Permissions Required" else "Grant Permissions",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                ),
                color = if (permanentlyDenied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.75f
                    }
                    .padding(horizontal = MeshTheme.spacing.mediumSmall)
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))

            // 3. Description (fade + slide up)
            Text(
                text = if (permanentlyDenied) {
                    "Permissions were permanently denied. Please open Android App Settings and grant Bluetooth, Location, and Nearby permissions to use Mesh Link."
                } else {
                    "Mesh Link is an offline peer-to-peer network.\nDevice permissions are required to discover and chat with nearby devices without internet."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.6f
                    }
                    .padding(horizontal = MeshTheme.spacing.small)
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            // 4. Granular Permission Status Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.4f
                    },
                verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
            ) {
                // Bluetooth / Nearby Scanning Card
                ModernPermissionCard(
                    title = "Bluetooth & Nearby",
                    description = "Discovers, advertises, and connects with nearby mesh peers.",
                    isGranted = isBtGranted,
                    icon = Icons.Default.Bluetooth,
                    accentColor = Color(0xFF2563EB),
                    contentDescription = "Bluetooth permission status"
                )

                // Location Access Card
                ModernPermissionCard(
                    title = "Location Access",
                    description = "Required by Android for peer-to-peer wireless mesh scanning.",
                    isGranted = isLocGranted,
                    icon = Icons.Default.LocationOn,
                    accentColor = Color(0xFFF59E0B),
                    contentDescription = "Location permission status"
                )

                // Nearby Wi-Fi Card (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ModernPermissionCard(
                        title = "Nearby Wi-Fi Devices",
                        description = "Enables Wi-Fi Direct for high-speed offline media & file transfer.",
                        isGranted = isNearbyWifiGranted,
                        icon = Icons.Default.Wifi,
                        accentColor = Color(0xFF16A34A),
                        contentDescription = "Nearby Wi-Fi permission status"
                    )
                }

                // Notifications Card (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ModernPermissionCard(
                        title = "Notifications",
                        description = "Alerts you to incoming messages, peer connections, and SOS alarms.",
                        isGranted = isNotifGranted,
                        icon = Icons.Default.Notifications,
                        accentColor = Color(0xFF8B5CF6),
                        contentDescription = "Notification permission status"
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))

            // 5. Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.2f
                    }
            ) {
                if (permanentlyDenied) {
                    Button(
                        onClick = onOpenSettings,
                        interactionSource = buttonInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(buttonScale),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = "Open App Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = onGrantPermissions,
                        interactionSource = buttonInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(buttonScale),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = "Grant Permissions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            // 6. Security / Privacy Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                    }
                    .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.small),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security and Privacy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                Text(
                    text = "Permissions are used strictly for local mesh communication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
        }
    }
}

/**
 * 3D Hero Icon Cluster with pulsing animation for permission landing.
 */
@Composable
private fun HeroPermissionIconCluster(permanentlyDenied: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_perm_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_perm_pulse"
    )

    val isDark = isSystemInDarkTheme()
    val primaryColor = if (permanentlyDenied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing halo
        Box(
            modifier = Modifier
                .size(84.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = if (isDark) 0.15f else 0.10f))
        )

        Surface(
            modifier = Modifier
                .size(72.dp)
                .scale(pulseScale)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = primaryColor.copy(alpha = 0.25f),
                    spotColor = primaryColor.copy(alpha = 0.35f)
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.5.dp,
                primaryColor.copy(alpha = 0.4f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (permanentlyDenied) Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = "Permission Security Hero",
                    tint = primaryColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

/**
 * Modern permission status card showing icon, name, rationale, and live Granted/Required status pill.
 */
@Composable
private fun ModernPermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    accentColor: Color,
    contentDescription: String
) {
    val isDark = isSystemInDarkTheme()
    val cardBorderColor by animateColorAsState(
        targetValue = if (isGranted) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        } else {
            accentColor.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "perm_card_border"
    )

    val iconBgColor = if (isGranted) {
        if (isDark) Color(0xFF1B382B) else Color(0xFFE8F5E9)
    } else {
        if (isDark) accentColor.copy(alpha = 0.18f) else accentColor.copy(alpha = 0.10f)
    }

    val iconTintColor = if (isGranted) {
        if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    } else {
        accentColor
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isGranted) 1.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isGranted) Color.Black.copy(alpha = 0.02f) else accentColor.copy(alpha = 0.08f),
                spotColor = if (isGranted) Color.Black.copy(alpha = 0.04f) else accentColor.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Icon Badge
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = iconBgColor,
                border = BorderStroke(1.dp, iconTintColor.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = iconTintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    PermissionStatusPill(isGranted = isGranted)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Status badge pill indicating GRANTED vs REQUIRED with high contrast in both light and dark themes.
 */
@Composable
private fun PermissionStatusPill(isGranted: Boolean) {
    val isDark = isSystemInDarkTheme()

    val badgeBgColor by animateColorAsState(
        targetValue = if (isGranted) {
            if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
        } else {
            if (isDark) Color(0xFF451A1A) else Color(0xFFFEE2E2)
        },
        animationSpec = tween(300),
        label = "perm_pill_bg"
    )

    val badgeTextColor by animateColorAsState(
        targetValue = if (isGranted) {
            if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
        } else {
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        },
        animationSpec = tween(300),
        label = "perm_pill_text"
    )

    Surface(
        shape = CircleShape,
        color = badgeBgColor
    ) {
        Text(
            text = if (isGranted) "GRANTED" else "REQUIRED",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = badgeTextColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Setup screen for Location provider on legacy Android versions (< S).
 */
@Composable
private fun LocationRequirementSetupScreen(
    onTurnOnLocation: () -> Unit
) {
    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffset = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        launch {
            entranceAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
        launch {
            entranceOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeshTheme.spacing.large, vertical = MeshTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraGiant))

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Required",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            Text(
                text = "Location Service Required",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))

            Text(
                text = "Android requires device Location services to be turned on to scan for background Bluetooth mesh nodes.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraGiant))

            Button(
                onClick = onTurnOnLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Turn on Location",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun RadioRequirementSetupScreen(
    isBluetoothEnabled: Boolean,
    isWifiEnabled: Boolean,
    onTurnOnBluetooth: () -> Unit,
    onTurnOnWifi: () -> Unit
) {
    // Staggered Page Entrance Animation
    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffset = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        launch {
            entranceAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
        launch {
            entranceOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeshTheme.spacing.large, vertical = MeshTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

            // 1. Top Hero 3D Icon Area (staggered fade & subtle scale)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        scaleX = 0.92f + (0.08f * entranceAlpha.value)
                        scaleY = 0.92f + (0.08f * entranceAlpha.value)
                    }
                    .padding(top = MeshTheme.spacing.small, bottom = MeshTheme.spacing.large)
            ) {
                HeroRadioIcons(
                    isBluetoothEnabled = isBluetoothEnabled,
                    isWifiEnabled = isWifiEnabled
                )
            }

            // 2. Main Title (fade + slide up)
            Text(
                text = "Bluetooth & Wi-Fi are required",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.75f
                    }
                    .padding(horizontal = MeshTheme.spacing.mediumSmall)
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))

            // 3. Description (fade + slide up)
            Text(
                text = "Mesh Link needs both Bluetooth and Wi-Fi\nto connect with nearby devices and\nprovide best performance.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.6f
                    }
                    .padding(horizontal = MeshTheme.spacing.small)
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))

            // 4. Bluetooth Modern Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.4f
                    }
            ) {
                ModernRadioStatusCard(
                    title = "Bluetooth",
                    description = "Required to discover and connect to nearby devices.",
                    isEnabled = isBluetoothEnabled,
                    icon = Icons.Default.Bluetooth,
                    actionButtonText = "Turn on Bluetooth",
                    onActionClick = onTurnOnBluetooth,
                    activeAccentColor = Color(0xFF2563EB),
                    contentDescription = "Bluetooth status: ${if (isBluetoothEnabled) "Enabled" else "Disabled"}"
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

            // 5. Wi-Fi Modern Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffset.value * 0.2f
                    }
            ) {
                ModernRadioStatusCard(
                    title = "Wi-Fi",
                    description = "Required for Wi-Fi Direct and high-speed connections.",
                    isEnabled = isWifiEnabled,
                    icon = Icons.Default.Wifi,
                    actionButtonText = "Turn on Wi-Fi",
                    onActionClick = onTurnOnWifi,
                    activeAccentColor = Color(0xFF16A34A),
                    contentDescription = "Wi-Fi status: ${if (isWifiEnabled) "Enabled" else "Disabled"}"
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            // 6. Security / Privacy Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                    }
                    .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.small),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security and Privacy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                Text(
                    text = "Both will be used only for Mesh Link connectivity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
        }
    }
}

@Composable
private fun HeroRadioIcons(
    isBluetoothEnabled: Boolean,
    isWifiEnabled: Boolean
) {
    val isDark = isSystemInDarkTheme()

    // Micro-animation for Bluetooth: subtle breathing pulse when ON
    val infiniteTransition = rememberInfiniteTransition(label = "hero_radio_anim")
    val btPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isBluetoothEnabled) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bt_pulse_scale"
    )

    // Micro-animation for Wi-Fi: subtle wave / ripple when ON
    val wifiRippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isWifiEnabled) 1.14f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wifi_ripple_scale"
    )
    val wifiRippleAlpha by infiniteTransition.animateFloat(
        initialValue = if (isWifiEnabled) 0.35f else 0.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wifi_ripple_alpha"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bluetooth Hero Container with 3D Depth
        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isBluetoothEnabled) {
                // Subtle glowing background halo
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(btPulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = if (isDark) 0.20f else 0.12f))
                )
            }
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .scale(if (isBluetoothEnabled) btPulseScale else 1f)
                    .shadow(
                        elevation = if (isBluetoothEnabled) 4.dp else 1.dp,
                        shape = CircleShape,
                        ambientColor = if (isBluetoothEnabled) Color(0xFF2563EB).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f),
                        spotColor = if (isBluetoothEnabled) Color(0xFF2563EB).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
                    ),
                shape = CircleShape,
                color = if (isBluetoothEnabled) {
                    if (isDark) Color(0xFF1E293B) else Color(0xFFE8F1FD)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
                border = BorderStroke(
                    1.5.dp,
                    if (isBluetoothEnabled) Color(0xFF93C5FD) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth Status",
                        tint = if (isBluetoothEnabled) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        // Wi-Fi Hero Container with 3D Depth & Ripple
        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isWifiEnabled) {
                // Subtle ripple wave ring
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(wifiRippleScale)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A).copy(alpha = wifiRippleAlpha))
                )
            }
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(
                        elevation = if (isWifiEnabled) 4.dp else 1.dp,
                        shape = CircleShape,
                        ambientColor = if (isWifiEnabled) Color(0xFF16A34A).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f),
                        spotColor = if (isWifiEnabled) Color(0xFF16A34A).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
                    ),
                shape = CircleShape,
                color = if (isWifiEnabled) {
                    if (isDark) Color(0xFF142E1F) else Color(0xFFE6F7ED)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
                border = BorderStroke(
                    1.5.dp,
                    if (isWifiEnabled) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi Status",
                        tint = if (isWifiEnabled) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernRadioStatusCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    icon: ImageVector,
    actionButtonText: String,
    onActionClick: () -> Unit,
    activeAccentColor: Color,
    contentDescription: String
) {
    val isDark = isSystemInDarkTheme()

    // Interactive button press scale state
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_press_scale"
    )

    // Animated container colors and elevation
    val cardBorderColor by animateColorAsState(
        targetValue = if (isEnabled) activeAccentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label = "card_border"
    )

    val iconContainerColor = if (isEnabled) {
        if (isDark) activeAccentColor.copy(alpha = 0.20f) else activeAccentColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isEnabled) 2.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isEnabled) activeAccentColor.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.03f),
                spotColor = if (isEnabled) activeAccentColor.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.mediumLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3D Circular Icon Badge
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = iconContainerColor,
                    border = BorderStroke(
                        1.dp,
                        if (isEnabled) activeAccentColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = contentDescription,
                            tint = if (isEnabled) activeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Status Badge Pill
                        StatusBadgePill(isEnabled = isEnabled)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Smooth Action Button visibility transition
            AnimatedVisibility(
                visible = !isEnabled,
                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
                    Button(
                        onClick = onActionClick,
                        interactionSource = buttonInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .scale(buttonScale),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeAccentColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = actionButtonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadgePill(isEnabled: Boolean) {
    val isDark = isSystemInDarkTheme()

    val badgeBgColor by animateColorAsState(
        targetValue = if (isEnabled) {
            if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
        } else {
            if (isDark) Color(0xFF451A1A) else Color(0xFFFEE2E2)
        },
        animationSpec = tween(300),
        label = "radio_pill_bg"
    )
    val badgeTextColor by animateColorAsState(
        targetValue = if (isEnabled) {
            if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
        } else {
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        },
        animationSpec = tween(300),
        label = "radio_pill_text"
    )

    Surface(
        shape = CircleShape,
        color = badgeBgColor
    ) {
        Text(
            text = if (isEnabled) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = badgeTextColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

fun isBluetoothPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
    }
}

fun isLocationPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

fun isNearbyWifiPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun isNotificationPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun isBluetoothEnabled(context: Context): Boolean {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return bluetoothManager?.adapter?.isEnabled == true
}

fun isWifiEnabled(context: Context): Boolean {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    return wifiManager?.isWifiEnabled == true
}

fun areRadiosAndPermissionsReady(context: Context): Boolean {
    return hasRequiredPermissions(context) && isBluetoothEnabled(context) && isWifiEnabled(context)
}

fun hasRequiredPermissions(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
