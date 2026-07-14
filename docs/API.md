# API Documentation

Mesh Link exposes several internal Domain interfaces that allow for modular swapping of implementation details.

## Core Interfaces
- `BleScannerManager`: `startScanning()`, `stopScanning()`, `scannedDevices: StateFlow`
- `BleAdvertiserManager`: `startAdvertising(payload)`, `stopAdvertising()`
- `WifiDirectManager`: `discoverPeers()`, `connectToPeer()`, `disconnect()`
- `CryptoManager`: `generateKeyPair()`, `encrypt(data, key)`, `decrypt(data, key)`

## Dependency Injection
All interfaces are bound to their implementations via Hilt modules in the `di/` package. Avoid instantiating concrete implementations directly.
