# Networking & Mesh Routing

## BLE Mesh Control Plane
Mesh Link utilizes Bluetooth Low Energy (BLE) as the primary control plane for discovery, handshake, and small payload messaging.
- **GATT Server/Client**: Custom GATT characteristics handle packet fragmentation, reassembly, and MTU negotiation (up to 512 bytes).
- **Multi-hop Routing**: Nodes act as Relays. Packets contain a Time-To-Live (TTL) field. When a node receives a packet not intended for it, it decrements the TTL and re-broadcasts it, creating a store-and-forward mesh.

## Wi-Fi Direct Data Plane (Hybrid Transport)
For large payloads (images, voice notes), BLE MTU limits are insufficient.
- **Failover**: When a large file transfer is initiated, nodes negotiate a Wi-Fi Direct Group Owner (GO).
- **TCP Sockets**: A dedicated `ServerSocket` loopback is established. 1MB chunked JSON payloads are serialized and streamed across the high-bandwidth Wi-Fi link.
- **Fallback**: If Wi-Fi Direct drops, the system seamlessly falls back to the BLE control plane without user intervention.
