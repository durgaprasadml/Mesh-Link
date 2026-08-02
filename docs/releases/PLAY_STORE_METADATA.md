# Google Play Store Release Package — Mesh-Link

This document contains the complete metadata, graphic assets requirements, data safety form details, privacy policy reference, and release deployment strategy for **Mesh-Link** on the Google Play Store.

---

## 1. App Listing Information

- **App Name**: Mesh-Link: Off-Grid Mesh Messaging
- **Package Name**: `com.meshlink`
- **Default Language**: English (United States) [en-US]
- **Category**: Communication / Social
- **Content Rating**: Everyone (PEGI 3, USK 0, ESRB Everyone)
- **Target Audience**: General Public, Emergency Responders, Outdoor Enthusiasts, Privacy-conscious Users

---

## 2. Store Descriptions

### Short Description (Max 80 characters)
> Secure, decentralized off-grid peer-to-peer mesh messaging over BLE & Wi-Fi.

### Full Description (Max 4,000 characters)
> **Mesh-Link** is a decentralized, peer-to-peer off-grid messaging application designed for reliable communication when cellular networks and internet connectivity are unavailable, degraded, or compromised.
>
> Built from the ground up for privacy, resilience, and battery efficiency, Mesh-Link forms an ad-hoc local mesh network directly between nearby Android devices using Bluetooth Low Energy (BLE) and Wi-Fi Direct. Every device acts as a secure relay node, extending network coverage across multiple hops.
>
> ### Key Features:
>
> - **100% Off-Grid Messaging**: Send direct text messages, location coordinates, and voice notes without cellular data, Wi-Fi routers, or central cloud servers.
> - **Peer-to-Peer Encryption**: End-to-end encryption secures private unicast conversations and peer key exchanges using modern cryptographic primitives.
> - **Automatic Mesh Routing**: Dynamic multi-hop routing automatically discovers optimal delivery paths across neighboring devices.
> - **Emergency SOS & Safety Alerts**: Broadcast emergency beacons and distress signals with high-priority mesh propagation and location sharing.
> - **Nearby Radar & Discovery**: Visualize active P2P mesh nodes and signal strength in real-time.
> - **Public Broadcast Channels**: Share community announcements and situational updates across local mesh zones.
> - **Adaptive Layouts**: Full Material 3 support for compact phones, large screens, foldables, tablets, and landscape desktop displays.
> - **Zero Data Collection**: No account creation, phone number, or email required. No user data, analytics, or telemetry leaves your device.
>
> ### Designed For:
> - Emergency Response & Disaster Relief
> - Hiking, Camping, and Remote Outdoor Expeditions
> - Crowded Festivals, Concerts, and Stadium Events
> - Privacy & Off-Grid Communication Enthusiasts

---

## 3. Data Safety Form Summary

| Category | Status | Details |
| :--- | :--- | :--- |
| **Data Collection** | **No Data Collected** | Mesh-Link does not collect, record, or transmit any user data to external servers or third parties. |
| **Data Sharing** | **No Data Shared** | Zero user data is shared with third parties or advertising networks. |
| **Security Practices** | **Encrypted in Transit** | P2P data packets sent over BLE/Wi-Fi Direct are encrypted using peer-to-peer session keys. |
| **Data Deletion** | **User Control** | All database tables (messages, contacts, logs) reside strictly on device and can be cleared instantly in Settings. |

---

## 4. Play Store Graphics & Visual Assets Checklist

- [x] **App Icon**: 512px × 512px, 32-bit PNG with alpha, max 1024KB.
- [x] **Feature Graphic**: 1024px × 500px, PNG or JPEG, max 15MB.
- [x] **Phone Screenshots**: Minimum 4 screenshots (1080 × 1920 or 1080 × 2400 resolution):
  1. Home Screen & Conversation List
  2. End-to-End Encrypted Chat
  3. Nearby Mesh Radar & Node Discovery
  4. Emergency SOS Beacon & Safety Center
- [x] **7-inch & 10-inch Tablet Screenshots**: Minimum 2 screenshots showcasing multi-pane adaptive viewports.

---

## 5. Release Rollout Plan

1. **Internal Testing**: Release AAB (`bundleProductionRelease`) uploaded to Google Play Console Internal Track for team validation.
2. **Closed Beta (100 Users)**: Rollout to closed testing track for 14-day stability and battery performance monitoring.
3. **Staged Production Release**:
   - Day 1: 10% rollout
   - Day 3: 25% rollout
   - Day 5: 50% rollout
   - Day 7: 100% Full Production Release
