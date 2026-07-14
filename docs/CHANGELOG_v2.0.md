# Changelog (v2.0 Enterprise Release)

## Massive Overhaul
Mesh Link v2.0 represents a complete rewrite of the underlying network architecture, transforming the application from a basic P2P chat proof-of-concept into a mission-critical, enterprise-ready Mobile Ad-hoc Network (MANET).

## Major Additions

### [E1] Enterprise Architecture
- Fully migrated to **Hilt** Dependency Injection.
- Established strict separation of concerns (Presentation -> Domain -> Data -> Routing).
- Centralized asynchronous work via `CoroutineScopes` bound to precise lifecycle events.

### [E2] Production Security Architecture
- Removed plaintext transmission. Implemented **AES-256-GCM** authenticated encryption.
- Implemented **ECDH** (Curve25519) key exchange for perfect forward secrecy.
- Built a robust `TrustManager` to automatically block rogue nodes and mitigate DoS attacks.

### [E3] Intelligent Mesh Routing Engine
- Replaced naive broadcast flooding with a dynamic `RouteTable` and `QueueOptimizer`.
- Added multi-hop distance vector routing to prevent broadcast storms.

### [E4] Enterprise File Transfer Engine
- Built a robust, chunked file transfer protocol capable of resuming dropped transfers.
- Added intelligent offloading to **Wi-Fi Direct** for payloads > 500KB.

### [E5/E6] Enterprise Voice & Video Communication
- Integrated Media3 and CameraX for real-time voice and video encoding.
- Handled hardware-accelerated encoding via `MediaCodec`.

### [E7/E8] Mesh Intelligence & Offline AI
- Built `BatteryAwareNetworking` and an offline `BatteryPredictor` to dynamically throttle background traffic based on power states.
- Prevents the mesh from draining host devices during extended offline operations.

### [E9] Emergency Response Platform
- Added `EmergencyManager` to override power limits during critical operations.
- Built a `CRITICAL` priority SOS Beacon and offline Team Tracking system.
- Built a `DisasterRecoveryEngine` to rapidly heal partitioned mesh networks.
