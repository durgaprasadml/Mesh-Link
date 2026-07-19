# Permission Validation Report

## Runtime Matrix
Mesh Link correctly partitions permission requests based on Android OS version.

1. **Android 13+ (API 33+)**
   - `NEARBY_WIFI_DEVICES`: Core logic for Wi-Fi Direct.
   - `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`: Granular BLE isolation.
   - `POST_NOTIFICATIONS`: Requested correctly for the relay service.
2. **Android 12 (API 31-32)**
   - Granular Bluetooth permissions validated.
3. **Legacy (API < 31)**
   - Broad `BLUETOOTH`, `BLUETOOTH_ADMIN`, and `ACCESS_FINE_LOCATION` logic.

## Degradation & Recovery
- **Denial Handling:** If a user denies Bluetooth, the app elegantly falls back to a descriptive error state with a "Grant Permissions" UI button, preventing crash loops.
- **Revocation:** If permissions are revoked in OS Settings while the app is alive, standard Android termination occurs; upon restart, the UI elegantly catches the denied state.

## Status: **PASS**
