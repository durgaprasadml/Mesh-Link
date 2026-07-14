# Chaos Engineering Report

This report documents the intentional injection of extreme faults into the Mesh Link environment to validate system resilience.

## Test Methodologies & Results

### 1. Bluetooth/Wi-Fi Radio Chaos
- **Scenario**: Randomly toggle Airplane Mode, Bluetooth OFF, and Wi-Fi OFF during large media transfers and mesh multi-hop routing.
- **Expected Outcome**: TCP socket failure instantly downgrades to BLE GATT MTU chunks. If BLE is disabled, messages queue to local SQLCipher DB.
- **Result**: ✅ PASS. `WifiSocketTransport` correctly identifies `SocketException` and falls back to `BleRepositoryImpl`.

### 2. Process Death & Service Kill
- **Scenario**: Execute `kill -9` or ADB force-stop during active mesh synchronization.
- **Expected Outcome**: Android 14+ Foreground Service (FGS) restarts the application. Pending WorkManager tasks flush the offline message queue.
- **Result**: ✅ PASS. Database WAL prevents corruption. Pending `MessageEntity` items remain marked `Status.PENDING` and are re-transmitted on boot.

### 3. Cryptographic Chaos
- **Scenario**: Inject malformed ECDH public keys and replay old sequence numbers.
- **Expected Outcome**: Trust scores decrement, packets are dropped at the `TrustManager` boundary, and sessions rotate.
- **Result**: ✅ PASS. No plaintext leaked. Replay packets silently dropped.
