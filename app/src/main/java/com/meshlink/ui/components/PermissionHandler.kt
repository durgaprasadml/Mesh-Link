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

import android.net.wifi.WifiManager
import android.content.BroadcastReceiver
import android.content.IntentFilter

@Composable
fun PermissionHandler(
    onPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current

    var hasPermissions by remember { mutableStateOf(hasRequiredPermissions(context)) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var isBluetoothEnabled by remember { mutableStateOf(isBluetoothEnabled(context)) }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    var isLocationEnabled by remember { mutableStateOf(locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) }

    var isWifiEnabled by remember { mutableStateOf(isWifiEnabled(context)) }

    // Real-time broadcast listener for Bluetooth & Wi-Fi radio state changes
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isBluetoothEnabled = isBluetoothEnabled(context)
                isWifiEnabled = isWifiEnabled(context)
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
                isLocationEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
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
        isLocationEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MeshTheme.spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val titleText = when {
            !isBluetoothEnabled && !isWifiEnabled -> "Bluetooth & Wi-Fi are required"
            !isBluetoothEnabled -> "Bluetooth is required"
            else -> "Wi-Fi is required"
        }

        val descriptionText = when {
            !isBluetoothEnabled && !isWifiEnabled -> "Mesh-Link requires both Bluetooth and Wi-Fi to discover nearby devices and provide complete mesh connectivity."
            !isBluetoothEnabled -> "Bluetooth is required to discover and connect with nearby devices."
            else -> "Wi-Fi is required for Wi-Fi Direct and high-speed mesh connections."
        }

        Text(
            text = titleText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

        // Status Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MeshTheme.spacing.medium),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioStatusBadge(label = "Bluetooth", isEnabled = isBluetoothEnabled)
                RadioStatusBadge(label = "Wi-Fi", isEnabled = isWifiEnabled)
            }
        }

        Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))

        if (!isBluetoothEnabled) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isWifiEnabled) {
                    Text(
                        text = "Bluetooth",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Required to discover and connect to nearby devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                }
                Button(
                    onClick = onTurnOnBluetooth,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text("Turn on Bluetooth")
                }
            }
        }

        if (!isBluetoothEnabled && !isWifiEnabled) {
            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
        }

        if (!isWifiEnabled) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isBluetoothEnabled) {
                    Text(
                        text = "Wi-Fi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Required for Wi-Fi Direct and high-speed connections.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                }
                Button(
                    onClick = onTurnOnWifi,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text("Turn on Wi-Fi")
                }
            }
        }

        Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))

        Text(
            text = "Both Bluetooth and Wi-Fi are required for Mesh-Link.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RadioStatusBadge(label: String, isEnabled: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isEnabled) "ON" else "OFF",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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

