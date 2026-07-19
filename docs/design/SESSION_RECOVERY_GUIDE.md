# Session Recovery Guide

## Mechanics
Session recovery is intrinsically tied to the `RetryCoordinator`. Because Mesh Link uses BLE and Wi-Fi Direct protocols which are stateless over the air (besides cryptographic handshakes), a "session" is recovered by re-establishing the underlying link.

1. **State Check:** `RetryCoordinator` periodically probes `MeshRepository.getMeshStatus()`.
2. **Re-initialization:** If the state shows disconnected but the app hasn't issued a clean stop, `autoStartMesh()` is executed to re-advertise and scan.
3. **Cryptographic Recovery:** Once a transport layer connects, existing ECDH keys in the database are transparently re-used since the business logic handles key exchange caching.
