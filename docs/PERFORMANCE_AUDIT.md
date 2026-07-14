# Performance Audit

This document summarizes the performance audit conducted on Mesh Link to ensure there are no regressions or bottlenecks introduced during the compatibility hardening phase.

## Startup
- Startup latency is minimal. The `BootCompletedReceiver` does not block the UI thread and dispatches the `MeshRelayService` immediately.
- The `try/catch` added for `ForegroundServiceStartNotAllowedException` prevents cold-start crashes on restricted devices.

## BLE Latency
- The MTU negotiation in `BleGattManager` (requesting 512 bytes) significantly improves throughput for large JSON packets.
- The 15-minute scheduled BLE scanner restart prevents Android from downgrading the scan to opportunistic mode, which historically increased discovery latency from ~2 seconds to >15 seconds.

## Wi-Fi Latency
- High-speed transfers are negotiated via Wi-Fi Direct seamlessly. The added try/catch blocks do not introduce latency but prevent silent failures during capability negotiation.

## Memory & Battery
- Explicit `Job` and `CoroutineScope` cancellations prevent memory leaks inside `MeshRelayService` and `BleScannerManager`.
- Device cleanup job (every 5 seconds) effectively trims stale BLE nodes from the `scannedDevices` map, maintaining low memory overhead.
- Partial WakeLocks are renewed efficiently every 2 minutes rather than being held indefinitely, reducing power consumption during idle periods.

## Optimization Suggestions
1. **Adaptive Scanning**: Dynamically adjust the BLE scan interval based on the device's battery level (e.g., pause scans if battery < 15%).
2. **Batch Advertising**: Reduce advertise interval if no devices have been seen for a prolonged period.
