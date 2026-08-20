package com.meshlink.ui.components

import com.meshlink.ui.designsystem.theme.MeshTheme
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp

import android.net.wifi.WifiManager
import android.content.BroadcastReceiver
import android.content.IntentFilter

@Composable
fun rememberRadioAndPermissionState(context: Context = LocalContext.current): Boolean {
    var isReady by remember { mutableStateOf(areRadiosAndPermissionsReady(context)) }

    // Real-time broadcast listener for immediate Bluetooth & Wi-Fi radio state changes
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isReady = areRadiosAndPermissionsReady(context)
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

    // Real-time broadcast listener for Bluetooth & Wi-Fi radio state changes
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    isBluetoothEnabled = when (btState) {
                        BluetoothAdapter.STATE_ON -> true
                        BluetoothAdapter.STATE_OFF -> false
                        else -> isBluetoothEnabled(context)
                    }
                } else if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    isWifiEnabled = when (wifiState) {
                        WifiManager.WIFI_STATE_ENABLED -> true
                        WifiManager.WIFI_STATE_DISABLED -> false
                        else -> isWifiEnabled(context)
                    }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (!hasPermissions) {
            val activity = context.findActivity()
            if (activity != null) {
                permanentlyDenied = results.filter { !it.value }.keys.any { permission ->
                    !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
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

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    if (!hasPermissions) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MeshTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (permanentlyDenied) {
                Text(
                    "Permissions were permanently denied. Please open App Settings, tap Permissions, and grant them to use Mesh Link.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                Button(onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    settingsLauncher.launch(intent)
                }) {
                    Text("Open App Settings")
                }
            } else {
                Text(
                    "Mesh Link requires Bluetooth and Location permissions to discover and chat with nearby devices.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                    Text("Grant Permissions")
                }
            }
        }
    } else if (!isLocationEnabled) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MeshTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Android requires device Location to be turned on to scan for background Bluetooth signals.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
            Button(onClick = { 
                val enableLocationIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                locationEnableLauncher.launch(enableLocationIntent)
            }) {
                Text("Turn on Location")
            }
        }
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
                    activeContainerColor = Color(0xFFE8F1FD),
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
                    activeContainerColor = Color(0xFFE6F7ED),
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
                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))
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
                color = if (isBluetoothEnabled) Color(0xFFE8F1FD) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                color = if (isWifiEnabled) Color(0xFFE6F7ED) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionButtonText: String,
    onActionClick: () -> Unit,
    activeAccentColor: Color,
    activeContainerColor: Color,
    contentDescription: String
) {
    // Interactive button press scale state
    val buttonInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_press_scale"
    )

    // Animated container colors and elevation
    val cardBorderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isEnabled) activeAccentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label = "card_border"
    )

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
                    color = if (isEnabled) activeContainerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
            androidx.compose.animation.AnimatedVisibility(
                visible = !isEnabled,
                enter = androidx.compose.animation.fadeIn(tween(250)) + androidx.compose.animation.expandVertically(tween(250)),
                exit = androidx.compose.animation.fadeOut(tween(200)) + androidx.compose.animation.shrinkVertically(tween(200))
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
    val badgeBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isEnabled) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
        animationSpec = tween(300),
        label = "pill_bg"
    )
    val badgeTextColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isEnabled) Color(0xFF15803D) else Color(0xFFDC2626),
        animationSpec = tween(300),
        label = "pill_text"
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

