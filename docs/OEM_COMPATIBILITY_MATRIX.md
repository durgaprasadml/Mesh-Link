# OEM Compatibility Matrix

## Manufacturer Support

| OEM | BLE Scanning | BLE Advertising | Wi-Fi Direct Group Owner | Battery Restrictions | Status |
|---|---|---|---|---|---|
| **Google Pixel** | Excellent | Excellent | Excellent | Standard | **PASS** |
| **Samsung** | Excellent | Excellent | Good | Strict (Requires manual exemption) | **PASS** |
| **Xiaomi/Redmi**| Fair | Fair | Fair | Very Strict (MIUI auto-kills) | **PASS** (with UI prompt) |
| **OnePlus/Oppo**| Good | Good | Good | Strict (ColorOS auto-sleep) | **PASS** (with UI prompt) |
| **Motorola** | Excellent | Excellent | Excellent | Standard | **PASS** |
| **Nothing** | Excellent | Excellent | Excellent | Standard | **PASS** |

## UI Prompts for Aggressive Battery OEMs
Mesh Link detects device manufacturers (e.g. `Build.MANUFACTURER == "Xiaomi"`) and prompts users to exclude the app from aggressive battery killing through the `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` intent.
