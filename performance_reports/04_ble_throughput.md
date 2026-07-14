# RC2 BLE Throughput Report

## Overview
This report outlines the Bluetooth Low Energy (BLE) optimizations in RC2.

## Optimizations
1. **GATT Write Type:**
   - Changed to `WRITE_TYPE_NO_RESPONSE` in `BleGattManager.kt`.
2. **MTU Negotiation:**
   - Ensured `requestMtu(512)` is actively used for maximum chunk size.

## Expected Metrics
- **BLE Throughput:** Increased from ~2-4 KB/s to ~8-12 KB/s on supported Android 13+ hardware.
- **Latency:** Inter-packet delay significantly reduced due to omitted GATT ACKs.
