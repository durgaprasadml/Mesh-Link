# ANR Analysis

## Analyzed Bottlenecks
- `BleGattManager`: Replaced blocking Thread.sleep() delays with suspend-compatible delay() calls in loops.
- `RouteCache`: Replaced synchronized(this) locks with Mutex to prevent main thread blocking during high packet throughput.
- `PeerSecureSession`: Migrated blocking synchronization to coroutine-safe Mutex.

## Outcome
Application Not Responding (ANR) events are significantly reduced under heavy BLE traffic.