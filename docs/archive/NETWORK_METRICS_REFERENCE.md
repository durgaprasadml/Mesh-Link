# Network Metrics Reference

## Analytics Dictionary
- **connectedPeers**: Total number of established sockets.
- **discoveredPeers**: Total number of devices seen via BLE scanning but not connected.
- **averageRttMs**: Global Round Trip Time for application-level ping/pong packets.
- **packetDeliveryRate**: Percentage of sent packets that received a cryptographic ACK.
- **packetLossRate**: Inferred by un-ACKed packets after retry limits are exhausted.
- **totalHopCount**: Aggregate number of hops required for all messages in the current session.
- **duplicatePacketRate**: Percentage of received packets dropped due to duplicate UUIDs (indicates flood density).
