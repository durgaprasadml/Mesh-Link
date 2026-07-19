# Topology Engine

## Graphing the Mesh Offline
The `TopologyManager` provides a mathematical representation of the active offline network.

### Node and Edge Mapping
- **Nodes**: Known devices. Categorized as Gateways (connected to internet/other large clusters) or Leafs (edges of the network).
- **Edges**: The active transport link (BLE or Wi-Fi Direct) and its associated RSSI.

### Exports
Because field technicians cannot use cloud-based visualization, `TopologyManager.exportToMermaid()` generates a markdown-compatible `graph TD;` string. This allows diagnostic tools to render the mesh visually, entirely offline, revealing bottlenecks and disconnected islands.
