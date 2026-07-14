# Android Compatibility Report

This document outlines Mesh Link's compatibility and readiness across Android versions 13 to 17+.

## Compatibility Matrix

| Feature | Android 13 (API 33) | Android 14 (API 34) | Android 15 (API 35) | Android 16/17 Readiness |
| :--- | :---: | :---: | :---: | :---: |
| **Foreground Services** | ✅ | ✅ | ✅ | ✅ |
| **Notifications** | ✅ | ✅ | ✅ | ✅ |
| **BLE Scanning/Adv** | ✅ | ✅ | ✅ | ✅ |
| **Wi-Fi Direct** | ✅ | ✅ | ✅ | ✅ |
| **Permissions** | ✅ | ✅ | ✅ | ✅ |

### Android 13 (API 33)
- **Notifications**: Properly handles runtime `POST_NOTIFICATIONS` permission requirement via updated `PermissionHandler.kt`.
- **Bluetooth Permissions**: Fully utilizes `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, and `BLUETOOTH_CONNECT`.
- **Wi-Fi Direct**: Uses `NEARBY_WIFI_DEVICES`.

### Android 14 (API 34)
- **Foreground Service Types**: Explicitly declares `foregroundServiceType="connectedDevice"` for `MeshRelayService` in the Manifest.
- **Service Invocation**: `startForeground()` passes `ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`.
- **Alarms**: Gracefully falls back if `SCHEDULE_EXACT_ALARM` is denied (which is the default on Android 14).

### Android 15 (API 35)
- **Deprecated APIs**: Maintained standard Jetpack/AndroidX components instead of relying on legacy Framework APIs.
- **Background Execution**: Added try-catch for `ForegroundServiceStartNotAllowedException` specifically when `BootCompletedReceiver` attempts to launch services from the background.

### Android 16/17 (Readiness)
- **Resilience**: BLE operations now have aggressive try/catch blocks for `SecurityException` and `IllegalStateException`, preparing for even stricter background/sensor restrictions.
- **Scoped Storage/Privacy**: Handled transparently by using standard FileProviders.
