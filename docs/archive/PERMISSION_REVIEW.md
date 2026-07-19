# Android Permission Review

Mesh Link follows the Principle of Least Privilege. The following permissions are requested and justified:

## Critical Networking Permissions (Android 13+)
- `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`: (Grouped under "Nearby Devices"). Absolutely essential for forming the BLE mesh network.
- `ACCESS_FINE_LOCATION`: Required by Android for legacy Wi-Fi Direct P2P discovery on API < 33. On API 33+, this is circumvented via `NEARBY_WIFI_DEVICES`.
- `NEARBY_WIFI_DEVICES`: Required for establishing high-bandwidth TCP sockets without prompting for location.

## Media & Storage Permissions
- `READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO`: Required to attach photos and voice notes to chats. Granular media permissions (API 33+) prevent full storage access.

## Foreground & Background Limits
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`: Required to keep the BLE mesh alive when the screen is locked, preventing the OS from severing the mesh topology.
- `POST_NOTIFICATIONS`: Required to alert the user of incoming offline messages.
