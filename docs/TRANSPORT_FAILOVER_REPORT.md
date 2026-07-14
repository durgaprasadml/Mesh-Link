# Transport Failover Report

## Scenario: Dynamic Environment Shift
- **Action:** Two nodes utilizing high-speed Wi-Fi Direct for a 100MB file transfer move out of Wi-Fi range (approx. 50 meters).
- **Response:**
  - Wi-Fi Direct `SocketException` triggers transport fallback logic.
  - `ChunkManager` persists the exact `chunkIndex`.
  - `IntelligentTransportManager` downgrades the payload stream seamlessly to Bluetooth Low Energy (BLE) using the pre-existing BLE connection.
  - The transfer resumes at the exact byte boundary (albeit at BLE speeds).

**Status:** Certified Zero-Data-Loss Failover.
