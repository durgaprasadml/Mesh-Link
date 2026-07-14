# Troubleshooting Guide

## Q: Messages are stuck on "Pending" (Clock Icon)
**A:** The recipient is out of Bluetooth range, and no relay node has passed within range to forward the message. Ensure both devices have Bluetooth enabled.

## Q: Image transfer is taking a very long time
**A:** The Wi-Fi Direct hybrid failover likely failed (or was denied permission), causing the app to fall back to BLE. BLE transfer speeds are approximately 8-20 KB/s. Keep the devices close together and ensure Wi-Fi is toggled ON.

## Q: "Nearby Devices Permission Required" loop
**A:** On Android 13+, the `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` permissions are grouped under "Nearby Devices". If the user selects "Don't allow" twice, Android prevents the app from prompting again.
- **Fix**: Direct the user to system settings -> Apps -> Mesh Link -> Permissions.

## Q: SQLCipher Database Corruption Crash
**A:** If the database becomes corrupted due to physical storage errors or forced power loss during a transaction, the app will fail to decrypt.
- **Fix**: Clear app data. The app is designed to wipe the corrupted DB and start fresh on the next launch.
