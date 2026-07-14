# Play Store Compliance

This document verifies Mesh Link's compliance with Google Play Developer Policies.

## Target SDK
- **Current Target SDK**: 34 (Android 14)
- **Compliance Status**: Passes all requirements for API level targeting. Fully ready for Android 15 (API 35).

## Permissions and Data Safety
- **Nearby Devices**: `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `NEARBY_WIFI_DEVICES`. The app's core purpose is a mesh network, so these permissions are justifiable and core functionality.
- **Location**: Required for Bluetooth discovery on older Android versions. Coarse/Fine location is requested transparently with UI rationale.
- **Foreground Services**: Uses the `connectedDevice` type, which is strictly monitored by Google Play. Mesh Link's functionality as a mesh relay perfectly aligns with this type requirement.
- **Exact Alarms**: Removed reliance on exact alarms for background restart. The app uses `setAndAllowWhileIdle` but handles `SecurityException` to fall back to standard alarms, meaning we do not need to submit an intent declaration for `SCHEDULE_EXACT_ALARM`.

## Privacy Policy Requirements
- The app handles location data (via Bluetooth scanning) and must have a valid Privacy Policy linked in the Play Console.
- All data transmission is end-to-end encrypted; no user data is sent off-device except directly to peers.

## Conclusion
Mesh Link is fully compliant with current Google Play policies and is prepared for upcoming Android 15 policy enforcement.
