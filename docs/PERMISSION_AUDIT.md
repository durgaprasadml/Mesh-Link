# Permission Audit

A comprehensive audit of all permissions requested by Mesh Link and their runtime flows.

## Core Permissions

### Bluetooth Permissions (Android 12+)
- `BLUETOOTH_SCAN`: Required to find nearby mesh nodes.
- `BLUETOOTH_ADVERTISE`: Required to broadcast mesh presence.
- `BLUETOOTH_CONNECT`: Required to connect to GATT servers and send/receive data.
- **Runtime Flow**: Handled via `PermissionHandler.kt`. If denied, the user is presented with a rationale UI.
- **Fallback**: The app cannot function without BLE permissions. It stays on the permission rationale screen.

### Location Permissions
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Required by Android (specifically <= Android 11, and sometimes Wi-Fi Direct) to discover nearby devices since Bluetooth can be used to infer location.
- **Runtime Flow**: Handled via `PermissionHandler.kt`.
- **Fallback**: App cannot discover peers effectively without this on older OS versions or for Wi-Fi Direct.

### Wi-Fi Direct (Android 13+)
- `NEARBY_WIFI_DEVICES`: Required to negotiate high-speed file transfers via Wi-Fi Direct.
- **Runtime Flow**: Handled gracefully.
- **Fallback**: If denied, the app falls back to standard BLE transfer.

### Notifications & Background
- `POST_NOTIFICATIONS` (Android 13+): Required to show the Foreground Service notification and message notifications.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_CONNECTED_DEVICE`: Required to keep the Mesh Relay active in the background.
- `WAKE_LOCK`: To temporarily keep the CPU awake during critical BLE routing.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: To request exemption from Doze mode to maintain stable mesh connectivity.
- `RECEIVE_BOOT_COMPLETED`: To automatically restart the mesh relay on device boot.

## Runtime Error Handling
- Attempting to start the foreground service from the background (e.g., from `BootCompletedReceiver`) catches `ForegroundServiceStartNotAllowedException` to prevent crashes on Android 12+.
- Attempting to set exact alarms for service restarts catches `SecurityException` and falls back to inexact alarms.
