# Transport Analytics

## BLE vs Wi-Fi Direct Performance
Mesh Link dynamically switches between transports. `TransportAnalytics` tracks *why* and *when* these switches happen.

### Tracked Metrics
- **BLE GATT Latency**: BLE connections are stable but slow. We track fragmentation ratios (how many 512-byte fragments are needed for a large payload).
- **Wi-Fi Handshake Duration**: Wi-Fi Direct has higher bandwidth but slower negotiation times (often taking > 3000ms).

This intelligence allows the app to know if a transport layer is degrading *before* it disconnects entirely.
