# Mesh Link — GitHub Repository Documentation Pack

## 1. Repository Description (≤350 characters)

```
Secure offline P2P mesh communication for Android. Text, images, voice & location over Bluetooth LE — no internet, no servers. End-to-end AES-256-GCM encrypted, SQLCipher database, MVVM + Hilt + Jetpack Compose.
```

---

## 2. GitHub About Section

### Repository Description
```
Secure offline Android mesh-chat app. P2P BLE communication with E2E encryption, multi-hop routing, image/voice/location sharing, SOS broadcasts — zero infrastructure required.
```

### Website
```
(None — add your portfolio URL when available)
```

### Topics (30 tags)

```
android
kotlin
jetpack-compose
bluetooth-le
ble
mesh-network
offline-chat
p2p
peer-to-peer
room-database
sqlcipher
hilt
dagger
mvvm
clean-architecture
coroutines
kotlin-flow
aes-gcm
ecdh
end-to-end-encryption
material3
navigation-compose
wifi-direct
emergency-communication
disaster-relief
sos
broadcast-messaging
offline-first
android-security
open-source
```

---

## 3. Repository Tags

```
mesh-network, offline-chat, ble, bluetooth, android, kotlin, jetpack-compose,
encrypted-messaging, p2p, e2e-encryption, sqlcipher, mvvm, hilt, emergency,
sos, decentralized, no-internet, privacy, open-source, college-project
```

---

## 4. Social Preview Text

```
Mesh Link — Secure offline Android chat over Bluetooth LE.
No internet. No servers. End-to-end encrypted messaging, images, voice notes, location sharing, and SOS broadcasts across a decentralized mesh network.
```

---

## 5. Release Notes — v3.0.0

### Mesh Link v3.0.0 — Production Release

**Release Date:** July 2026

---

#### Highlights

This release marks the first production-quality milestone of Mesh Link. The core BLE mesh communication stack, encryption layer, and Compose UI are all stable. The app is self-contained — it requires no external services, accounts, or connectivity to operate.

---

#### Features Included in This Release

**Messaging**
- Encrypted text messaging with delivery status: `PENDING → SENT → RELAYED → DELIVERED → SEEN → FAILED`
- Image sharing with chunked BLE transfer (ACK/NACK, retry, progress indicator)
- Camera capture and direct send
- Voice note recording and playback over BLE
- Location sharing with tap-to-open-maps
- Broadcast messaging to all reachable peers
- SOS emergency broadcast with animated UI and haptic feedback

**Networking**
- BLE GATT server + client with MTU negotiation and packet fragmentation/reassembly
- Multi-hop mesh routing engine (TTL=10, max 5 hops by default)
- 20,000-entry LRU deduplication cache preventing broadcast storms and routing loops
- Battery-aware BLE scanning with Kalman RSSI smoothing
- Wi-Fi Direct manager and socket transport layer (experimental)
- Foreground service with boot persistence

**Security**
- ECDH key exchange + AES-256-GCM encryption on all unicast messages
- SQLCipher full-database encryption at rest
- Session management, re-keying, and trust verification
- Security event audit log

**UI**
- Material 3 design system with dark / light / AMOLED / system theme
- Material You dynamic colour (Android 12+)
- Adaptive navigation: bottom bar (phones) + navigation rail (tablets/foldables)
- Animated screen transitions

---

#### Known Issues

- Wi-Fi Direct connection handshake is unreliable on some OEM variants (Samsung, Xiaomi)
- Voice streaming over mesh relay is not yet end-to-end verified
- Android 14 background location permission requires explicit user approval step
- `DatabaseModule.kt` has uncommitted local changes that need to be merged before building from a clean checkout

---

#### Upgrade Notes

- First-launch creates a local profile and generates a unique mesh identity
- No account migration required — all data is device-local
- Minimum Android version remains API 26 (Android 8.0)

---

## 6. Professional Project Descriptions

### 50-Word Version (Resume / LinkedIn headline)

> Mesh Link is an Android app for secure offline peer-to-peer communication over Bluetooth LE. It enables text, image, voice, location, and SOS messaging across a self-forming mesh network — with end-to-end AES-256-GCM encryption, SQLCipher storage, and no internet or server dependency.

---

### 100-Word Version (LinkedIn About / Portfolio card)

> Mesh Link is a fully offline Android messaging application built around a Bluetooth LE mesh network. Devices discover each other automatically and form ad-hoc, multi-hop communication chains without any internet connectivity, Wi-Fi, or centralised infrastructure.
>
> Messages are end-to-end encrypted using ECDH key exchange and AES-256-GCM. All data is persisted in a SQLCipher-encrypted Room database. The app supports text, images (with chunked BLE transfer), voice notes, GPS location sharing, group broadcasts, and an SOS emergency mode.
>
> Built with Kotlin, Jetpack Compose (Material 3), Hilt, MVVM + Clean Architecture, Kotlin Coroutines, and Flow — following production Android engineering standards throughout.

---

### 250-Word Version (Portfolio writeup / Cover letter supplement)

> Mesh Link is a production-grade Android application I designed and built to solve a real problem: reliable, private communication when internet infrastructure is unavailable. The application forms a decentralised mesh network using Bluetooth Low Energy, allowing any number of nearby Android devices to exchange messages, images, voice recordings, GPS locations, and emergency SOS alerts — entirely offline.
>
> **Technical depth:** The BLE stack is built from scratch on Android's native GATT APIs, handling MTU negotiation, packet fragmentation and reassembly, multi-point connections, and battery-aware scanning with Kalman-filtered RSSI smoothing. A custom routing engine manages multi-hop message delivery (up to 5 hops by default) with QoS prioritisation, congestion monitoring, intelligent retry, and a 20,000-entry deduplication cache that prevents routing loops and broadcast storms.
>
> **Security:** Every unicast message is end-to-end encrypted with ECDH key exchange and AES-256-GCM. Peer public keys are stored in AndroidX `EncryptedSharedPreferences`. The Room database is fully encrypted at rest using SQLCipher. A security monitor logs all trust events to an encrypted audit table.
>
> **Architecture:** The project follows Clean Architecture with an MVVM presentation layer. Hilt manages the entire dependency graph. All async operations use Kotlin Coroutines and Flow, making the codebase reactive, testable, and lifecycle-safe. The UI is built entirely in Jetpack Compose with Material 3, including an adaptive layout supporting both phones and tablets.
>
> Mesh Link demonstrates applied knowledge of distributed systems, cryptography, Android platform APIs, and modern software engineering practices — built as both a functional tool and a rigorous engineering portfolio piece.

---

## 7. College / Academic Project Description

### Project Title
**Mesh Link: Decentralised Bluetooth LE Mesh Messaging Application for Android**

---

### Objective

To design and implement a fully offline peer-to-peer communication system for Android devices that operates without internet connectivity, centralised servers, or cloud infrastructure — using Bluetooth Low Energy as the sole transport medium.

---

### Problem Statement

Conventional messaging platforms depend entirely on internet connectivity and centralised server architectures. In disaster scenarios, remote areas, or privacy-critical environments, this dependency renders standard communication tools unusable. This project addresses the gap by creating a resilient, self-organising mesh communication layer that works between devices in close proximity.

---

### Methodology

1. **Mesh Network Layer**: Implemented a BLE GATT server and client to enable multi-point, bidirectional communication between Android devices. A custom packet routing engine handles multi-hop delivery (up to 5 hops), deduplication, loop detection, and Quality-of-Service prioritisation.

2. **Cryptographic Layer**: Applied Elliptic Curve Diffie-Hellman (ECDH) for session key agreement between every device pair, followed by AES-256-GCM for authenticated, confidential message encryption. Keys are managed via Android Keystore and `EncryptedSharedPreferences`.

3. **Persistence Layer**: Designed a Room database schema with 6 entity types (messages, chats, users, trust records, relay packets, audit logs), encrypted at rest using SQLCipher. Implemented schema migrations from version 1 through version 11.

4. **Application Architecture**: Followed Clean Architecture with an MVVM presentation layer, Dependency Injection via Dagger Hilt, and a reactive data pipeline using Kotlin Coroutines and StateFlow.

5. **User Interface**: Built entirely in Jetpack Compose (Material 3) with an adaptive layout, dark/light/AMOLED theme support, and animated screen transitions.

---

### Technologies Used

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Clean Architecture, Repository Pattern |
| DI | Dagger Hilt |
| Database | Room, SQLCipher |
| Async | Kotlin Coroutines, Flow |
| Bluetooth | Android BLE (BluetoothGattServer, BluetoothGatt) |
| Cryptography | ECDH, AES-256-GCM, Android Keystore |
| Storage | EncryptedSharedPreferences, AndroidX DataStore |
| Background | Foreground Service, WorkManager, BroadcastReceiver |
| Media | CameraX, MediaRecorder |

---

### Features Delivered

- Peer discovery and multi-hop BLE mesh routing
- End-to-end encrypted text messaging with delivery receipts
- Chunked image transfer with ACK/NACK reliability
- Voice note recording and playback
- GPS location sharing
- Group broadcast messaging
- SOS emergency alert system
- SQLCipher-encrypted persistent storage
- Material 3 adaptive UI with dark mode

---

### Outcome

Mesh Link demonstrates the feasibility of building a production-quality, encrypted, decentralised communication platform using only standard Android APIs and Bluetooth hardware — without any external dependencies, cloud services, or internet connectivity. The project applies theoretical concepts from computer networks (routing, fragmentation, deduplication), cryptography (ECDH, symmetric authenticated encryption), and distributed systems (mesh topology, peer-to-peer architecture) in a working, installable Android application.

---

## 8. GitHub Homepage / Landing Page Content

### Hero Section

```
🔗 Mesh Link

Secure offline P2P messaging over Bluetooth LE
No internet. No servers. No compromise.

[View Source]  [Download APK]
```

### Features Table

| Feature | Status |
|---|---|
| Encrypted text messaging | ✅ |
| Image sharing over BLE | ✅ |
| Camera capture & send | ✅ |
| Voice notes | ✅ |
| Location sharing | ✅ |
| Broadcast to all peers | ✅ |
| SOS emergency alert | ✅ |
| Multi-hop mesh routing | ✅ |
| ECDH + AES-256-GCM encryption | ✅ |
| SQLCipher encrypted database | ✅ |
| Dark / AMOLED / Material You | ✅ |

### Quick Start

```bash
git clone https://github.com/<your-username>/mesh-link.git
# Open in Android Studio → Sync → Run on two physical Android devices
```

No API keys. No configuration. Just build and run.

### Stack Badge Row

```
Kotlin · Jetpack Compose · Hilt · Room · SQLCipher · BLE · Coroutines · Flow · Material 3
```
