# Data Integrity Guide

## Validating Local Storage
Before Mesh Link trusts any offline data, it validates its integrity.

### `IntegrityManager`
- Uses `CRC32` hashes for fast chunk validation during media transfers.
- Uses `SHA-256` hashing to guarantee the authenticity of database backup snapshots.
- A `PerformSystemIntegrityCheck` is executed during the app's `onCreate()` before Room is allowed to initialize, preventing catastrophic corruption propagation.
