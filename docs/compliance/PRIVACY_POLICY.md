# Privacy Policy — Mesh-Link

**Effective Date**: August 2, 2026  
**App Name**: Mesh-Link  
**Developer**: Mesh-Link Open Source Project  

---

## 1. Overview

Mesh-Link ("the Application") is a decentralized, peer-to-peer off-grid messaging application. We respect your privacy and are committed to protecting it through our commitment to zero data collection.

This Privacy Policy explains how Mesh-Link handles your information. **In short: Mesh-Link collects, stores, tracks, and transmits ZERO personal data to any external server, developer, or third party.**

---

## 2. Information We Collect

### A. Personal Information
- **None**. Mesh-Link does not require registration, phone numbers, email addresses, names, or accounts of any kind.

### B. Messages & Content
- All messages, voice notes, media attachments, and SOS alerts created within Mesh-Link are stored **locally on your device** in an encrypted SQLite database.
- Messages sent over the mesh network are transmitted directly device-to-device via Bluetooth Low Energy (BLE) and Wi-Fi Direct. No messages pass through any central server or cloud relay.

### C. Device & Diagnostics Information
- **None**. Mesh-Link contains no analytics SDKs, advertising IDs, crash reporting trackers, or telemetry tools.

---

## 3. How Device Permissions Are Used

Mesh-Link requests Android runtime permissions strictly for local peer-to-peer communication functionality:

1. **Bluetooth & Nearby Devices (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES`)**: Required to discover nearby peer devices and establish local P2P Bluetooth and Wi-Fi mesh connections.
2. **Location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)**: Required by the Android OS framework to enable Bluetooth Low Energy scanning and Wi-Fi Direct peer discovery. Mesh-Link does not record your GPS location or send it anywhere unless you explicitly choose to include coordinates in an Emergency SOS message.
3. **Camera & Microphone (`CAMERA`, `RECORD_AUDIO`)**: Optional permissions required only if you capture a photo or record a voice note to send locally over the mesh.
4. **Notifications (`POST_NOTIFICATIONS`)**: Used to display incoming message notifications and foreground service status on your device.

---

## 4. Encryption & Security

- Direct messages between peers are secured using peer-to-peer end-to-end encryption.
- Local database storage utilizes SQLCipher encryption to protect data at rest on your device.
- Because Mesh-Link is serverless, there are no remote servers to breach or intercept.

---

## 5. Data Retention & Deletion

You have complete ownership and control over your data:
- All stored messages, contacts, and configuration data reside exclusively on your local device.
- You can clear all application data at any time via Android Settings > Apps > Mesh-Link > Clear Data, or through the in-app Storage Settings.

---

## 6. Third-Party Services & Links

Mesh-Link incorporates **no third-party SDKs, ad networks, tracking libraries, or analytics frameworks**.

---

## 7. Contact Us

If you have any questions or feedback regarding this Privacy Policy, please visit our official repository or contact the project maintainers via the application repository.
