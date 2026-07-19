# OEM Compatibility Report

This document highlights known behaviors and mitigations for various Android OEMs regarding BLE and Background Execution.

## OEM Mitigation Matrix

| OEM | Known Behaviors | Mitigations Implemented |
| :--- | :--- | :--- |
| **Samsung** | Aggressive Doze mode, background BLE scan throttling. | Uses `FOREGROUND_SERVICE_CONNECTED_DEVICE` and requests `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. |
| **Pixel (Google)**| Strict Android 14+ Foreground Service rules. | Correct `ServiceInfo` types passed to `startForeground`. Catch `ForegroundServiceStartNotAllowedException`. |
| **Xiaomi / MIUI** | Autostart is disabled by default; kills services aggressively. | Wrapped `AlarmManager` and `BOOT_COMPLETED` intents. Users should manually enable Autostart for uninterrupted mesh. |
| **OnePlus / Oppo / Vivo** | Aggressive background killing, BLE scan caching issues. | Periodic scan restart (every 15 minutes) to avoid opportunistic downgrade. Restart on task removed. |
| **Motorola** | Generally close to AOSP, but some Bluetooth stack crashes. | Robust `try/catch` around `startAdvertising` and `startScan` in `BleRepositoryImpl`. |
| **Nothing** | AOSP-like; minimal restrictions. | Standard AOSP compliance. |
| **Realme** | Similar to Oppo/ColorOS background restrictions. | Handled via `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. |

### Key Mitigations

1. **Scan Timeout Recovery**: Android inherently downgrades BLE scans lasting >30 minutes. We have implemented a 15-minute scheduled restart of the BLE Scanner to force it back into `LOW_LATENCY` mode.
2. **Foreground Restrictions**: Caught exceptions for `ForegroundServiceStartNotAllowedException` specifically for Samsung and Pixel devices that enforce background limits strictly.
3. **Exact Alarms**: Xiaomi and Samsung often deny exact alarms. We fall back to inexact alarms gracefully if `SecurityException` is thrown.
