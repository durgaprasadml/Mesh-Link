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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onPermissionsGranted: @Composable () -> Unit = {}
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
        onPermissionsGranted()
    }
}

@Composable
private fun RadioRequirementSetupScreen(
    isBluetoothEnabled: Boolean,
    isWifiEnabled: Boolean,
    onTurnOnBluetooth: () -> Unit,
    onTurnOnWifi: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3. Top Icon Area: Two circular containers side-by-side
            Row(
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bluetooth Circle Container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F1FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Wi-Fi Circle Container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6F7ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // 4. Main Heading
            Text(
                text = "Bluetooth & Wi-Fi are required",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                ),
                color = Color(0xFF111827),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Description
            Text(
                text = "Mesh Link needs both Bluetooth and Wi-Fi\nto connect with nearby devices and provide\nbest performance.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 25.sp
                ),
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 52.dp),
                thickness = 1.dp,
                color = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Bluetooth Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F1FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bluetooth",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = if (isBluetoothEnabled) "ON" else "OFF",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = if (isBluetoothEnabled) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Required to discover and\nconnect to nearby devices.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 17.sp,
                                lineHeight = 22.sp
                            ),
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                if (!isBluetoothEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onTurnOnBluetooth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Turn on Bluetooth",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 8. Section Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 52.dp),
                thickness = 1.dp,
                color = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 9. Wi-Fi Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE6F7ED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Wi-Fi",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = if (isWifiEnabled) "ON" else "OFF",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = if (isWifiEnabled) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Required for Wi-Fi Direct and\nhigh-speed connections.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 17.sp,
                                lineHeight = 22.sp
                            ),
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                if (!isWifiEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onTurnOnWifi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Turn on Wi-Fi",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 10. Security / Information Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Mesh Link Security",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Both will be used only for\nMesh Link connectivity.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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

