# Wi-Fi Direct & Hybrid Transport Testing Guide

This guide details the architecture, testing methodology, and behavior of the Wi-Fi Direct and Hybrid Transport subsystems in Mesh Link.

## Overview
Mesh Link uses a hybrid networking model:
- **BLE Mesh**: Always-on control plane for discovery, text messages, and low-latency signaling.
- **Wi-Fi Direct**: High-bandwidth data plane for large media transfers and rapid synchronization.

## Wi-Fi Direct Lifecycle
1. **Discovery (`WifiDirectManager.startDiscovery()`)**: Broadcasts P2P intents to nearby devices.
2. **Negotiation (`connectToPeer()`)**: Uses MAC address comparison to deterministically elect a Group Owner (GO intent = 15 for the numerically higher MAC address, 0 for the lower).
3. **Connection (`WIFI_P2P_CONNECTION_CHANGED_ACTION`)**: Upon successful group formation, the OS assigns the GO an IP address (typically `192.168.49.1`).
4. **Socket Binding (`WifiSocketTransport`)**: The GO binds a `ServerSocket` to port `8888`. The client device connects to the GO's IP address.

## Socket Architecture
- **Transport**: JVM `java.net.Socket` and `ServerSocket` operating over TCP.
- **Threading**: Managed via Kotlin Coroutines on `Dispatchers.IO` to prevent blocking the main thread during heavy reads/writes.
- **Protocol**: Packets are serialized to JSON using `MeshPacketParser` and terminated with a newline (`\n`). The socket reader reads line-by-line.

## Hybrid Transport Logic
The `BleRepositoryImpl` coordinates the two transports:
- When Wi-Fi Direct establishes a connection, `WifiSocketTransport.onPacketReceived` routes incoming payloads into the exact same pipeline as BLE packets.
- **BLE → Wi-Fi Upgrade**: When a media transfer initiates, the system checks if a Wi-Fi socket is available. If so, chunks are pushed over TCP seamlessly.
- **Wi-Fi → BLE Fallback**: If the socket disconnects, the socket teardown triggers a `PeerConnectionState` update. Pending messages in the database are retried, automatically falling back to BLE fragmentation.

## Testing Methodology
### Automated Testing
Due to Robolectric's limited support for native Wi-Fi Direct APIs, testing is split:
- **`WifiDirectManagerTest`**: Uses MockK to inject simulated Android system broadcast intents (`WIFI_P2P_PEERS_CHANGED_ACTION`, `WIFI_P2P_CONNECTION_CHANGED_ACTION`) to validate peer discovery and deterministic GO election.
- **`WifiSocketTransportTest`**: Uses real loopback networking (`127.0.0.1:8888`) within the JVM test environment to stress-test socket read/writes, IO blocking, and massive payload parsing.
- **`HybridTransportIntegrationTest`**: Tests the flow of data from the socket layer back into the repository business logic.

### Physical Device Testing (Android 13–17)
To fully validate the hybrid system, manual testing on physical devices is required:
1. Ensure both devices have **Nearby Devices** (Android 12+) and **Location** permissions granted.
2. Open the chat UI and send a high-resolution image.
3. Verify the system upgrades from BLE to Wi-Fi Direct seamlessly.
4. Disable Wi-Fi mid-transfer on one device to verify BLE fallback handles the remaining packets without crashing.

## Known Limitations & OEM Quicks
- **Samsung Devices**: Often aggressive with Wi-Fi Direct timeouts.
- **Android 14+**: Requires explicit user consent or foreground services for continuous background Wi-Fi Direct operation.
