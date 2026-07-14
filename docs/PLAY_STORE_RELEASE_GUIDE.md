# Play Store Release Guide (v2.0)

## 1. Store Listing & Compliance
Mesh Link v2.0 requires careful navigation of Google Play's policies due to its offline-first and mesh capabilities.

### Privacy Policy
- Must explicitly state that Mesh Link operates **100% offline** and does not upload data to any server.
- Must detail the usage of `NEARBY_WIFI_DEVICES` and `BLUETOOTH_CONNECT`. Clarify that these are used strictly for peer-to-peer mesh routing and NOT for location tracking.

### Data Safety Form
- **Data Collection:** NO.
- **Data Sharing:** NO.
- **Encryption in Transit:** YES (Peer-to-Peer AES-256-GCM).

## 2. Permissions Justification
When submitting the App Bundle, you must provide video evidence for the following permissions if flagged:
- `FOREGROUND_SERVICE_CONNECTED_DEVICE`: Provide a video showing the app transferring files or maintaining an emergency beacon while minimized.
- `MANAGE_EXTERNAL_STORAGE` (if used for large backups): Provide a video showing the offline backup/restore workflow.

## 3. Build & Signing
1. **Target API:** Ensure `targetSdkVersion` is at least 34 (Android 14).
2. **Obfuscation:** Run `./gradlew bundleRelease`. Verify that R8 mapping files (`mapping.txt`) are generated.
3. **App Signing:** Use Google Play App Signing. Upload the encrypted release key.
4. **Testing Tracks:** Push the AAB to the Closed Testing track first. Use the pre-launch report to verify no accessibility or crash regressions occurred on various devices.
