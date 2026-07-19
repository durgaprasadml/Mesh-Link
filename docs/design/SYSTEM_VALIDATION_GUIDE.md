# Mesh Link End-to-End System Validation Guide

This document outlines the rigorous manual and automated validation procedures required to certify Mesh Link for production deployment across Android 13–17+ devices. Because of the nature of Bluetooth Low Energy (BLE) Mesh and Wi-Fi Direct networking, full End-to-End (E2E) testing requires physical device matrix testing.

## Table of Contents
1. [Device Matrix Requirements](#device-matrix-requirements)
2. [Phase 1: Installation & Registration](#phase-1-installation--registration)
3. [Phase 2: Discovery & Handshake](#phase-2-discovery--handshake)
4. [Phase 3: Messaging & Media](#phase-3-messaging--media)
5. [Phase 4: Routing & Multi-Hop](#phase-4-routing--multi-hop)
6. [Phase 5: Background & Recovery](#phase-5-background--recovery)
7. [Phase 6: Chaos & Degradation](#phase-6-chaos--degradation)

---

## Device Matrix Requirements
To certify a release, the following configurations must be executed in a physical test lab.

### Topology Matrix
- **2 Devices**: Basic P2P connectivity, media transfer, hybrid transport switching.
- **3 Devices**: Single-hop relay validation, Group Owner election stability.
- **5 Devices**: Complex mesh routing, broadcast scaling, node failure.
- **10 Devices (Stress)**: Airwave saturation, collision avoidance, and TTL routing loops.

### OS Compatibility Matrix
- Android 13 (Tiramisu) - Verify legacy permission mappings.
- Android 14 (Upside Down Cake) - Verify strict foreground service requirements.
- Android 15 (Vanilla Ice Cream) - Verify partial screen recording and background networking limits.
- Android 16 (Baklava) - Verify aggressive battery optimization.
- Android 17 (Future) - Verify API deprecation safety.

### OEM Hardware Matrix
Due to severe Bluetooth Stack fragmentation, tests MUST include:
- **Google Pixel**: Baseline AOSP behavior.
- **Samsung Galaxy**: Validate aggressive battery management and Wi-Fi Direct MAC randomization.
- **OnePlus/Oppo/Vivo**: Validate background execution limits.
- **Xiaomi**: Validate custom permission enforcement for autostart.

---

## Phase 1: Installation & Registration
**Goal**: Verify fresh install, upgrade paths, and cryptographic identity generation.

| Test Case | Steps | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :--- |
| Fresh Install | Install APK. Grant Location & Nearby Devices. | Success, Mesh ID generated, Keypair created in Keystore. | [ ] |
| Upgrade Install | Upgrade from previous build containing data. | SQLCipher migration succeeds, all chats remain. | [ ] |
| Permission Denial | Deny Nearby Devices permission. | App gracefully degrades, shows rationale UI. | [ ] |
| Database Init | Launch app and inspect SQLite WAL mode. | Database encrypted with AES-256. | [ ] |

---

## Phase 2: Discovery & Handshake
**Goal**: Verify devices find each other and establish secure ECDH sessions.

| Test Case | Steps | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :--- |
| Device Appears | Place 2 devices in range. | Device appears in Nearby list within 5s. | [ ] |
| RSSI Update | Move devices apart. | Signal strength bars update dynamically. | [ ] |
| ECDH Handshake | Tap 'Connect' on a peer. | ECDH exchange succeeds. AES-GCM session created. | [ ] |
| Replay Protection | Intercept and inject old packets. | Packets rejected due to sequence number mismatch. | [ ] |
| Reconnect | Disable/Enable Bluetooth. | Device immediately reconnects using cached session. | [ ] |

---

## Phase 3: Messaging & Media
**Goal**: Verify the core application workflows (Text, Images, Voice).

| Test Case | Steps | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :--- |
| Text Stress | Send 1000 messages rapidly. | No loss, no duplicates, perfect ordering. | [ ] |
| Offline Queue | Send message while peer is off. | Message queues. Delivers upon reconnect. | [ ] |
| Image Transfer | Send a 5MB image. | BLE initiates, Wi-Fi Direct negotiates, 100% integrity. | [ ] |
| Voice Note | Record a 60-second voice note. | Plays clearly, background playback functions. | [ ] |
| Broadcast | Send SOS broadcast to 5 peers. | Delivered to all 5 peers instantly. | [ ] |

---

## Phase 4: Routing & Multi-Hop
**Goal**: Verify the mesh network can route packets through intermediary nodes.

| Test Case | Steps | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :--- |
| 3-Hop Route | A sends to C (out of range), B is middle. | B stores and forwards. C receives perfectly. | [ ] |
| Route Rebuild | Turn off B mid-transfer, turn on D. | A finds D and re-routes to C dynamically. | [ ] |
| TTL Expiry | Inject packet with TTL=1 for distant peer. | Packet is dropped after 1 hop to prevent loops. | [ ] |
| Broadcast Storm | 5 devices broadcast simultaneously. | Deduplication layer drops duplicate hashes. | [ ] |

---

## Phase 5: Background & Recovery
**Goal**: Verify the app functions reliably when minimized or screen is off.

| Test Case | Steps | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :--- |
| Screen OFF | Lock screen for 1 hour. Send message. | Message delivered. Notification triggers. | [ ] |
| Doze Mode | Leave device idle for 12 hours. | WorkManager periodic sync functions. | [ ] |
| Process Kill | Force Stop app. | WorkManager/FGS recovers networking state. | [ ] |
| Database Lock | Simulate concurrent reads/writes. | Room database handles transactions flawlessly. | [ ] |

---

## Phase 6: Chaos & Degradation
**Goal**: Ensure catastrophic failures degrade gracefully rather than crashing.

| Test Case | Steps | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :--- |
| Wi-Fi Direct Crash | Force disable Wi-Fi mid-transfer. | Socket breaks. Fallbacks to BLE MTU instantly. | [ ] |
| Bluetooth Restart | Restart Bluetooth mid-mesh routing. | Routing table flushes, rebuilds successfully. | [ ] |
| Corrupted DB | Inject corrupted SQL cipher key. | Fails safely, prompts user to reset data. | [ ] |
| Storage Full | Fill device storage. Send large image. | Toast error, prevents database corruption. | [ ] |

---
*End of Validation Guide*
