<div align="center">

# 🔗 Mesh Link

### Secure Offline Peer-to-Peer Mesh Communication for Android

*No internet. No servers. No compromise.*

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-See%20LICENSE-blue)](./LICENSE)
[![Version](https://img.shields.io/badge/Version-3.0.0-success)](./CHANGELOG.md)

</div>

---

## Overview

**Mesh Link** is a fully offline Android application that enables peer-to-peer communication using **Bluetooth Low Energy (BLE)** — with no reliance on Wi-Fi, mobile data, or any central server.

Built for environments where internet connectivity is unavailable or untrustworthy — disaster zones, remote areas, privacy-critical scenarios, or simple local communication — Mesh Link forms an ad-hoc decentralized mesh network directly between nearby Android devices.

Every message is **end-to-end encrypted** using ECDH key exchange and AES-256-GCM, and all data is stored locally in an **SQLCipher-encrypted Room database**. Nothing leaves the device except through the encrypted BLE channel.

### Why Mesh Link?

| Problem | Mesh Link Solution |
|---|---|
| No internet access in disasters | Operates entirely over Bluetooth |
| Centralized servers can be censored | Fully decentralized — no servers |
| Privacy concerns with cloud messaging | All data encrypted locally on-device |
| Range limitations of single BLE hop | Multi-hop relay routing across devices |
| Emergency communication needs | Dedicated SOS broadcast screen |

---

## ✅ Implemented Features

### 🛰️ Offline BLE Mesh Networking
- Full BLE GATT client/server implementation with multi-point ad-hoc connections
- Automatic device discovery, scanning, and advertising via `BleAdvertiserManager` and `BleScannerManager`
- Packet fragmentation and reassembly with MTU-aware chunking
- Duplicate packet detection with a 20,000-entry LRU deduplication cache
- Battery-aware scanning with adaptive intervals via `BatteryAwareScanner`
- RSSI-based signal filtering with Kalman smoothing (`KalmanFilter`, `RssiFilter`)
- Connection state lifecycle management with automatic reconnection

### 💬 Text Messaging
- Real-time encrypted text messaging between discovered peers
- Message delivery status tracking: `PENDING → SENT → RELAYED → DELIVERED → SEEN → FAILED`
- Persistent chat history stored in encrypted Room database
- Unread message counts and last-message previews on home screen
- Searchable chat list with animated transitions

### 🖼️ Image Sharing
- Chunked image transfer over BLE GATT (300-byte chunks, ACK/NACK per chunk)
- Automatic image compression via `ImageCompressor` (max 800px, ≤200KB, JPEG 30–45 quality)
- Base64 thumbnail previews stored alongside messages
- Transfer progress tracking (0.0 → 1.0) displayed per-message in UI
- Retry on NACK, timeout after 60 seconds, max 3 retries per chunk
- Full-screen image viewer (`MediaViewerScreen`)

### 📷 Camera Capture & Send
- In-app camera launch using `ActivityResultContracts.TakePicture`
- Captured photos are immediately compressed and sent over BLE

### 🎙️ Voice Notes
- In-app audio recording using `VoiceRecorder` (MediaRecorder backed)
- Playback with progress indicator via `VoicePlayer`
- Recording timer displayed in real-time in the message composer
- Voice messages stored as files and transferred over BLE

### 📍 Location Sharing
- GPS coordinate sharing via `LocationProvider`
- Location messages rendered in chat with tap-to-open-maps integration
- Battery percentage included alongside location packets

### 📢 Broadcast Messaging
- Send a text message to **all** reachable devices simultaneously
- Dedicated Broadcast screen with live message history
- Broadcast packets identified by `targetId = "BROADCAST"` (not encrypted)

### 🆘 SOS Emergency Screen
- Dedicated SOS screen with animated pulse UI and haptic feedback
- Long-press activation to trigger SOS broadcast
- SOS messages delivered as high-priority broadcast packets across the mesh

### 🔀 Multi-Hop Mesh Routing
- `RoutingEngine` with route scoring, caching, and health monitoring
- Configurable TTL and max-hops (default: TTL=10, max hops=5)
- Loop detection via visited-path tracking
- `IntelligentRetryEngine` for automatic message retry
- `QoSManager` for Quality-of-Service prioritisation
- `CongestionMonitor` and `QueueOptimizer` for network health
- `RouteOptimizer` and `RouteHealthMonitor` for dynamic path adaptation

### 🔐 End-to-End Encryption
- **ECDH** key exchange for shared session key establishment
- **AES-256-GCM** encryption for all messages (authenticated encryption)
- Per-peer session keys managed by `SessionManager`
- Automatic re-keying via `RekeyManager`
- `TrustManager` for peer identity verification
- `MeshSecurityMonitor` for security event auditing

### 🗄️ Encrypted Local Database
- **Room** with **SQLCipher** backend for full database encryption at rest
- Entities: `MessageEntity`, `ChatEntity`, `UserEntity`, `TrustEntity`, `RelayPacketEntity`, `AuditLogEntity`
- Optimised indices on `(chatId, timestamp)` and `messageId` (unique)
- Completed migrations from v1 through v11

### 🔄 Foreground Service & Boot Persistence
- `MeshRelayService` runs as a foreground service to maintain BLE connectivity
- `BootCompletedReceiver` auto-starts the service on device boot, package replace, and timezone change
- Wake lock management for reliable background operation
- WorkManager integration for deferrable background tasks

### 🎨 UI & Design System
- **Jetpack Compose** with Material 3
- Full **dark / light / system** theme support
- **Material You** dynamic colour on Android 12+ (opt-in)
- AMOLED dark mode option
- Custom accent colours (Blue, Green, Purple, Orange, Red)
- Configurable font scale, corner radius, animation toggle, and high-contrast mode
- Adaptive layout with `NavigationBar` (compact) and `NavigationRail` (expanded screens)
- Animated screen transitions (fade + horizontal slide)
- `MeshTheme` design system: `MeshColors`, `MeshTypography`, `MeshSpacing`, `MeshShapes`, `MeshElevation`, `MeshAnimations`

### 👤 Profile & Identity
- First-launch profile setup screen (`ProfileSetupScreen`)
- Unique `meshId` generated per device
- Profile editing via `ProfileScreen` inside Settings

### ⚙️ Settings
- **Network**: BLE advertising/scanning toggle, TX power, scan interval, auto-restart
- **Wi-Fi Direct**: enable/disable, auto-connect, peer discovery, preferred group owner
- **Transport**: Hybrid / BLE-only / Wi-Fi-only mode selection
- **Relay**: Mesh relay toggle, max hops, TTL, queue size, priority
- **Storage**: (database clear, storage stats)
- **Appearance**: Dark/Light/System theme, Material You, AMOLED, accent colour, font scale, corner radius, animations, glass effects, high contrast, reduce motion

### 📶 Wi-Fi Direct Fallback
- `WifiDirectManager` for Wi-Fi Direct peer discovery and group management
- `WifiSocketTransport` for high-throughput TCP socket transfers (media)
- Automatic fallback from BLE to Wi-Fi Direct for large media files

### 🔔 In-App Notifications
- `NotificationHelper` with `SharedFlow` delivers in-app snackbar notifications when a message arrives while the app is in another screen

---

## 🚧 Work in Progress

- **Wi-Fi Direct stability** — Discovery and connection handshake not fully reliable across all Android OEM variants
- **Voice transport over mesh** — `VoiceTransport` and codec pipeline partially implemented; end-to-end streaming not yet verified
- **Permission flow robustness** — `PermissionHandler` requires additional edge-case handling for Android 14+ granular Bluetooth permissions
- **Database module** — `DatabaseModule.kt` has uncommitted local changes (see `git status`)

---

## 📌 Planned Features

- [ ] Group mesh chat rooms
- [ ] Message reactions and replies
- [ ] QR-code based device pairing (ZXing integrated, not yet wired to pairing flow)
- [ ] Biometric authentication for app unlock (AndroidX Biometric dependency present)
- [ ] File / document sharing
- [ ] Network topology visualisation map
- [ ] Analytics dashboard for mesh health metrics
- [ ] Enterprise configuration and remote policy management
- [ ] Multi-language localisation

---

## Architecture

Mesh Link follows **Clean Architecture** with an **MVVM** presentation layer, organised into clearly separated layers.

```
┌─────────────────────────────────────────────────────┐
│                  UI Layer (Compose)                  │
│  Screens: Home · Nearby · Chat · Broadcast · SOS    │
│  Settings · Profile Setup · Media Viewer            │
│  ViewModels (Hilt-injected, StateFlow-based)        │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                 Domain Layer                         │
│  Models: Message · Chat · User · MeshPacket         │
│  Repository Interfaces: MeshRepository, UserRepo    │
│  Use Cases: SendMessage · GetMessages · DeleteChat  │
│             BroadcastMessage · MarkAsRead ...       │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                  Data Layer                          │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  BLE Stack   │  │   Database   │  │  Security │ │
│  │ GattManager  │  │  Room +      │  │  ECDH     │ │
│  │ Advertiser   │  │  SQLCipher   │  │  AES-GCM  │ │
│  │ Scanner      │  │  DAOs        │  │  Session  │ │
│  │ Discovery    │  │  Migrations  │  │  Trust    │ │
│  └──────┬───────┘  └──────────────┘  └───────────┘ │
│         │                                           │
│  ┌──────▼───────────────────────┐                  │
│  │  Mesh Routing Engine         │                  │
│  │  RouteManager · RouteCache   │                  │
│  │  QoS · Retry · Topology     │                  │
│  └──────────────────────────────┘                  │
│                                                     │
│  ┌───────────────────────────────────────────────┐ │
│  │  Media Layer                                  │ │
│  │  MediaTransferManager · ImageCompressor       │ │
│  │  VoiceRecorder · VoicePlayer                  │ │
│  │  WifiDirectManager · WifiSocketTransport      │ │
│  └───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              Background Services                     │
│  MeshRelayService (Foreground) · BootCompletedRx    │
│  WorkManager Tasks                                  │
└─────────────────────────────────────────────────────┘
```

### Key Design Decisions

- **Repository Pattern**: All data access goes through domain repository interfaces (`MeshRepository`, `UserRepository`, `SettingsRepository`, `ChatRepository`), decoupling the domain from implementation details.
- **Hilt DI**: Entire object graph is managed by Hilt with scoped singletons for managers that must survive across the app lifecycle.
- **Kotlin Coroutines + Flow**: All async operations (BLE events, database queries, network state) are modelled as cold/hot `Flow`s, collected by `StateFlow` in ViewModels.
- **SQLCipher**: Room's SupportSQLiteOpenHelper is overridden with SQLCipher to encrypt the entire database file at rest.
- **Packet Deduplication**: A 20,000-entry LRU set in `RoutingEngine` prevents broadcast storms and routing loops without per-packet round-trips.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI Toolkit | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Dagger Hilt |
| Database | Room + SQLCipher |
| Concurrency | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| Image Loading | Coil |
| Background Work | WorkManager |
| Bluetooth | Android BLE (GATT client + server) |
| Wi-Fi Fallback | Wi-Fi Direct (WifiP2pManager) |
| Encryption | ECDH + AES-256-GCM (Android Keystore) |
| Secure Storage | EncryptedSharedPreferences (Jetpack Security) |
| Analytics | Firebase Analytics + Crashlytics |
| QR Code | ZXing Core |
| Data Persistence | AndroidX DataStore (Preferences) |
| Serialisation | Kotlinx Serialization + Gson |
| Camera | CameraX |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |

---

## Project Structure

```
app/src/main/java/com/meshlink/
│
├── MainActivity.kt                  # Entry point, theme/service bootstrap
├── MeshLinkApp.kt                   # Application class, Hilt entry point
│
├── ble/
│   ├── data/                        # Core BLE implementation
│   │   ├── BleAdvertiserManager.kt  # BLE peripheral advertising
│   │   ├── BleGattManager.kt        # GATT client + server (fragmentation, MTU)
│   │   ├── BleScannerManager.kt     # BLE scan with duplicate filtering
│   │   ├── BleRepositoryImpl.kt     # Repository implementation for BLE ops
│   │   ├── MeshMessagingManager.kt  # Central message orchestrator
│   │   ├── MeshPacketParser.kt      # Packet serialisation / deserialisation
│   │   └── RoutingCoordinator.kt    # Peer ID normalisation + routing glue
│   └── discovery/                   # Smart discovery engine
│       ├── DiscoveryEngine.kt
│       ├── KalmanFilter.kt          # RSSI smoothing
│       ├── PeerScoreCalculator.kt
│       └── SmartConnectionPolicy.kt
│
├── routing/
│   ├── data/                        # MeshRouter interface + implementation
│   └── engine/                      # Routing algorithms
│       ├── RoutingEngine.kt         # Core engine (dedup, loop detection)
│       ├── RouteManager.kt
│       ├── RouteCache.kt
│       ├── RouteOptimizer.kt
│       ├── QoSManager.kt
│       ├── CongestionMonitor.kt
│       ├── IntelligentRetryEngine.kt
│       └── NetworkTopologyEngine.kt
│
├── security/
│   └── data/
│       ├── MeshCryptoManager.kt     # ECDH + AES-256-GCM encryption engine
│       ├── SessionManager.kt        # Per-peer session key lifecycle
│       ├── TrustManager.kt          # Peer identity + trust verification
│       ├── RekeyManager.kt          # Automatic key rotation
│       ├── DatabaseSecurityManager.kt
│       └── MeshSecurityMonitor.kt   # Security event audit log
│
├── database/
│   └── data/local/
│       ├── MeshDatabase.kt          # Room database (SQLCipher-backed)
│       ├── MeshDatabaseMigrations.kt # Migrations v1 → v11
│       ├── MessageEntity.kt + MessageDao (not shown) via ChatDao
│       ├── ChatDao.kt / ChatEntity.kt
│       ├── TrustDao.kt / TrustEntity.kt
│       ├── RelayDao.kt / RelayPacketEntity.kt
│       ├── UserDao.kt / UserEntity.kt
│       └── AuditLogDao.kt / AuditLogEntity.kt
│
├── messaging/
│   ├── data/MessagingRepositoryImpl.kt
│   └── presentation/
│       ├── ChatDetailScreen.kt      # Full chat view (text/image/voice/location)
│       ├── ChatDetailViewModel.kt
│       ├── ChatsListScreen.kt
│       └── MediaViewerScreen.kt     # Full-screen media viewer
│
├── media/
│   └── data/
│       ├── MediaTransferManager.kt  # Chunked BLE media transfer
│       ├── ImageCompressor.kt       # Compress images before BLE transfer
│       ├── VoiceRecorder.kt
│       └── VoicePlayer.kt
│
├── wifi/
│   └── data/
│       ├── WifiDirectManager.kt     # Wi-Fi Direct peer discovery + groups
│       └── WifiSocketTransport.kt   # TCP socket for large transfers
│
├── domain/
│   ├── model/                       # Pure Kotlin domain models
│   ├── repository/                  # Repository interfaces
│   └── usecase/                     # Business logic use cases
│
├── di/                              # Hilt modules
│   ├── DatabaseModule.kt
│   ├── DataModule.kt
│   ├── SecurityModule.kt
│   ├── BluetoothModule.kt
│   └── CoroutineModule.kt
│
├── service/
│   ├── MeshRelayService.kt          # Foreground BLE relay service
│   └── BootCompletedReceiver.kt     # Auto-start on boot
│
└── ui/
    ├── home/                        # Home screen (chat list + dashboard)
    ├── nearby/                      # Nearby devices screen
    ├── broadcast/                   # Broadcast messaging screen
    ├── sos/                         # SOS emergency screen
    ├── settings/                    # Settings with sub-screens
    ├── profile/                     # Profile setup + edit
    ├── navigation/                  # AppNavigation.kt (NavHost + bottom nav)
    ├── designsystem/                # MeshTheme, colours, typography, spacing
    └── components/                  # Shared reusable Composables
```

---

## Screens

| Screen | Description |
|---|---|
| **Profile Setup** | First-launch screen to create a local identity (name + mesh ID) |
| **Home** | Dashboard cards (Nearby, Broadcast, SOS) + recent chat list with search |
| **Nearby Devices** | Live BLE scan results with RSSI, tap to start a chat |
| **Chat Detail** | Full conversation view — text, images, voice notes, location, delivery status |
| **Chats List** | All conversations with unread counts |
| **Media Viewer** | Full-screen image viewer |
| **Broadcast** | Compose and send a message to all reachable devices |
| **SOS** | Emergency broadcast with animated pulse UI and haptic feedback |
| **Settings → Home** | Profile, Network, Storage, Appearance sub-sections |
| **Settings → Network** | BLE, Wi-Fi Direct, transport mode, relay, mesh parameters |
| **Settings → Appearance** | Theme, Material You, AMOLED, accent, font, animations |

---

## Installation

### Prerequisites

| Requirement | Version |
|---|---|
| Android Studio | Ladybug (2024.2) or newer |
| JDK | 17 |
| Android SDK | API 34 |
| Minimum Device OS | Android 8.0 (API 26) |
| Required Hardware | Bluetooth LE support |

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/mesh-link.git
cd mesh-link

# 2. Open in Android Studio
# File → Open → select the mesh-link directory

# 3. Sync Gradle
# Android Studio will prompt — click "Sync Now"

# 4. Select a build variant
# Build → Select Build Variant → internalDebug (recommended for development)

# 5. Run on a physical device
# BLE cannot be tested on the Android emulator — use two real Android phones
```

> **Note:** Use **two physical Android devices** to test BLE communication. The emulator does not support Bluetooth LE.

### Build Variants

| Variant | Debug Tools | Logging | Notes |
|---|---|---|---|
| `internalDebug` | ✅ | ✅ | For development |
| `betaDebug` | ❌ | ✅ | Beta testing |
| `productionRelease` | ❌ | ❌ | Production build |

---

## Permissions

All permissions are declared in `AndroidManifest.xml` and requested at runtime via `PermissionHandler.kt`.

| Permission | Why It's Required |
|---|---|
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Legacy BLE support (Android ≤ 11) |
| `BLUETOOTH_SCAN` | Discover nearby BLE devices (Android 12+) |
| `BLUETOOTH_ADVERTISE` | Advertise this device over BLE |
| `BLUETOOTH_CONNECT` | Establish GATT connections to peers |
| `ACCESS_FINE_LOCATION` | Required by Android for BLE scanning; also used for location-sharing messages |
| `ACCESS_COARSE_LOCATION` | Fallback location for BLE scanning |
| `ACCESS_BACKGROUND_LOCATION` | Allow BLE relay to run while app is backgrounded |
| `RECORD_AUDIO` | Voice note recording |
| `FOREGROUND_SERVICE` | Keep `MeshRelayService` alive |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Android 14 foreground service type for BLE |
| `POST_NOTIFICATIONS` | In-app message notifications (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Auto-start relay service after device reboot |
| `WAKE_LOCK` | Prevent CPU sleep during active BLE relay |
| `VIBRATE` | Haptic feedback for SOS and notifications |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent system from killing background service |

---

## Security

### Encryption Architecture

```
Device A                              Device B
   │                                      │
   │  1. Generate EC Key Pair             │
   │     (Android Keystore)               │
   │                                      │
   │  2. Exchange Public Keys over BLE ──►│
   │◄── 2. Exchange Public Keys ──────────│
   │                                      │
   │  3. ECDH Key Agreement               │
   │     → Shared Secret                  │
   │                                      │
   │  4. Derive AES-256 Session Key       │
   │     (SHA-256 of shared secret)       │
   │                                      │
   │  5. Encrypt messages with AES-GCM ──►│
   │◄── 5. Encrypt messages with AES-GCM ─│
```

| Component | Implementation |
|---|---|
| Key Exchange | ECDH (Elliptic Curve Diffie-Hellman) via `KeyPairGenerator` / `KeyAgreement` |
| Message Encryption | AES-256-GCM (authenticated encryption, prevents tampering) |
| Key Storage | Android Keystore for own private keys; `EncryptedSharedPreferences` for peer public keys |
| Database Encryption | SQLCipher — the entire Room database file is encrypted at rest |
| Preference Encryption | AndroidX `EncryptedSharedPreferences` with AES256-GCM master key |
| Session Management | Per-peer `PeerSecureSession` with re-keying via `RekeyManager` |
| Security Audit | `MeshSecurityMonitor` logs security events to encrypted `AuditLogEntity` |
| Trust Verification | `TrustManager` validates peer identity before decryption |

### Threat Model

- ✅ **Passive eavesdropping** — Mitigated by AES-256-GCM encryption on every packet
- ✅ **Database extraction** — Mitigated by SQLCipher full-database encryption
- ✅ **Replay attacks** — Mitigated by unique packet IDs and session keys
- ✅ **Routing loops / storms** — Mitigated by visited-path tracking and dedup cache
- ⚠️ **Active MITM** — Partially mitigated by `TrustManager`; full certificate pinning not yet implemented

---

## Current Status

### ✅ Completed

- BLE GATT server + client with fragmentation, MTU negotiation, multi-peer connections
- Device discovery with RSSI filtering (Kalman), battery-aware scanning, smart connection policy
- Text messaging with delivery status tracking
- Image transfer over BLE (chunked, ACK/NACK, retry, progress)
- Camera capture and send
- Voice note recording and playback over BLE
- Location sharing with map launch
- Broadcast messaging to all peers
- SOS emergency screen with animated UI
- ECDH + AES-256-GCM end-to-end encryption
- Session management, re-keying, and trust verification
- Room + SQLCipher encrypted database with migrations (v1–v11)
- Multi-hop mesh routing with QoS, retry, congestion monitoring
- Wi-Fi Direct manager and socket transport layer
- Foreground service with boot receiver
- MVVM + Clean Architecture with Hilt DI
- Dark / light / AMOLED / system theme + Material You
- Adaptive navigation (bottom nav + navigation rail)
- Profile setup and editing
- Settings: network, Wi-Fi Direct, relay, appearance, storage

### 🚧 In Progress

- Wi-Fi Direct connection stability improvements
- End-to-end voice streaming via mesh relay
- Comprehensive permission handling for Android 14+
- `DatabaseModule.kt` refactor (local uncommitted changes)

### 📌 Planned

- Group chat rooms
- QR code peer pairing
- Biometric app lock
- File / document sharing
- Network topology map
- Full multi-language support
- Enterprise remote policy management

---

## Roadmap

- [x] BLE GATT mesh communication
- [x] End-to-end encryption (ECDH + AES-GCM)
- [x] Text, image, voice, location message types
- [x] Broadcast and SOS screens
- [x] SQLCipher encrypted database
- [x] Multi-hop routing engine
- [x] Foreground service + boot persistence
- [x] Material 3 design system with dark mode
- [ ] QR code device pairing
- [ ] Group mesh chat
- [ ] Biometric authentication
- [ ] Voice relay streaming (end-to-end)
- [ ] Wi-Fi Direct stability on all OEMs
- [ ] Network topology visualisation
- [ ] Play Store release

---

## Contributing

Contributions are welcome. Please follow these guidelines:

### Branch Naming

```
feature/short-description    # New features
fix/issue-description        # Bug fixes
refactor/component-name      # Refactors with no functional change
docs/what-you-documented     # Documentation updates
```

### Commit Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(ble): add MTU negotiation retry on connection failure
fix(crypto): prevent key derivation NPE on first handshake
refactor(routing): extract RouteScorer into separate class
docs(readme): update permissions table for Android 14
```

### Pull Request Checklist

- [ ] Code builds without warnings (`./gradlew assembleInternalDebug`)
- [ ] Existing tests pass (`./gradlew test`)
- [ ] New behaviour is covered by unit tests
- [ ] No new lint errors (`./gradlew lintInternalDebug`)
- [ ] Changes are scoped to one concern per PR
- [ ] PR description explains *why*, not just *what*

### Coding Standards

- **Kotlin idioms**: prefer `data class`, `sealed class`, `when` expressions, extension functions
- **Coroutines**: always use structured concurrency (`SupervisorJob` + scoped `CoroutineScope`)
- **Compose**: stateless composables with state hoisting; ViewModels own mutable state
- **Hilt**: `@Singleton` for managers; `@HiltViewModel` for ViewModels
- **Naming**: `Manager` for stateful orchestrators; `Repository` for data access; `UseCase` for single-operation domain logic

---

## Acknowledgements

| Library | Purpose |
|---|---|
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | Declarative UI toolkit |
| [Dagger Hilt](https://dagger.dev/hilt/) | Dependency injection |
| [Room](https://developer.android.com/training/data-storage/room) | Local database ORM |
| [SQLCipher for Android](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/) | Database encryption |
| [AndroidX Security Crypto](https://developer.android.com/topic/security/data) | EncryptedSharedPreferences |
| [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) | Structured async |
| [Coil](https://coil-kt.github.io/coil/) | Image loading |
| [Firebase Crashlytics](https://firebase.google.com/products/crashlytics) | Crash reporting |
| [ZXing](https://github.com/zxing/zxing) | QR code generation (planned) |
| [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | Background task scheduling |
| [AndroidX DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | Typed settings persistence |
| [CameraX](https://developer.android.com/training/camerax) | Camera capture |

---

## License

See the [LICENSE](./LICENSE) file for details.

---

<div align="center">
Built with ❤️ for offline-first communication
</div>
