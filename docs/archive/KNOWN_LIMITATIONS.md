# Known Limitations

While Mesh Link is certified for production, the underlying nature of Android's physical radios introduces certain unavoidable constraints:

1. **Android 14+ Wi-Fi Direct Disconnects**:
   - The OS may aggressively tear down Wi-Fi Direct connections if it detects a high-bandwidth internet network is preferred and the P2P connection lacks internet.
   - *Workaround*: Mesh Link detects this tear-down and falls back to BLE MTU chunks.
2. **Background BLE Scanning on Custom ROMs**:
   - MIUI (Xiaomi) and ColorOS (Oppo) may freeze BLE scanning when the screen is off to preserve battery.
   - *Workaround*: A Foreground Service is used, but users MUST manually enable the "Autostart" permission in device settings.
3. **Cross-OS Compatibility**:
   - iOS devices are strictly incompatible with Android Wi-Fi Direct. Therefore, a future iOS client will rely purely on BLE.
