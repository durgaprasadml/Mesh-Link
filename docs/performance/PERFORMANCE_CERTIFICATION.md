# Performance & Battery Certification (v2.0)

## 1. Battery Optimization
Mesh Link v2.0 implements extreme battery awareness through the `BatteryAwareNetworking` module and offline AI `BatteryPredictor`.
- **Normal State:** Aggressive BLE scanning and frequent Wi-Fi Direct probes.
- **Power Saver State (Battery < 50% or OS forced):** Reduces broadcast probability by 50%. Defers non-critical file chunk synchronization.
- **Critical State (Battery < 15%):** Halts all background routing except for `CRITICAL` priority SOS/Emergency beacons. Drops BLE scan intervals to lowest duty cycle.

## 2. Memory Certification
Extensive audits have been performed to prevent memory leaks:
- **Coroutines:** Bound strictly to `viewModelScope` and `lifecycleScope`. Foreground services use `SupervisorJob` to prevent cascaded failures.
- **Bitmaps:** Large media files and video frames bypass heap memory through zero-copy NIO buffers and MediaCodec hardware acceleration.
- **Singletons:** Managed safely via Hilt (`@Singleton`), ensuring no stale Activity contexts are retained.

## 3. Throughput & Network Performance
- **BLE Payload Tuning:** Tuned MTU negotiation requests 512 bytes, significantly reducing packet fragmentation overhead on modern devices.
- **Wi-Fi Direct Offloading:** Any file > 500KB automatically attempts Wi-Fi socket negotiation, achieving up to 40 Mbps throughput in clean environments, compared to 0.1 Mbps on BLE.
- **Queue Prioritization:** The strict `PriorityQueue` ensures that a 100MB file transfer will not delay a 1KB text message or an SOS beacon.

## 4. Conclusion
Performance is certified. Mesh Link meets the stringent requirements for low-power, long-duration offline operations.
