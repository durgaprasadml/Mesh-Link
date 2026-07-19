# Mesh Analytics Guide

## The `MeshAnalyticsManager`
This acts as the core heartbeat of the offline network.

### Overview
Instead of scattering metrics across `BleManager` or `RoutingManager`, all events flow into `MeshAnalyticsManager`. It exposes a single Kotlin `StateFlow<MeshAnalytics>`, which represents the live health of the network.

### Exported Data
- `connectedPeers`: Active sockets.
- `discoveredPeers`: Discovered but unconnected peers.
- `averageRttMs`: Global Round Trip Time.
- `packetDeliveryRate`: % of packets confirmed by ACK.
- `duplicatePacketRate`: % of packets dropped by flood suppression.
