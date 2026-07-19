# Phase H5 - Network Analytics Audit

**Date:** July 2026
**Status:** Completed

## 1. Subsystem Review
- **MeshRouter**: Currently lacks metrics on flood propagation, cache hit rates, and dead-end routes.
- **BleRepository & WifiDirectManager**: Provide basic connectivity states but do not track handshake durations, GATT latencies, or fragmentation ratios.
- **TransportManager**: Switches transports blindly based on disconnects; lacks intelligence on degradation prior to total failure.
- **RelayService**: No metrics on relay efficiency (how many packets are successfully forwarded vs dropped due to TTL).
- **RoutingManager**: Lacks visibility into convergence times.
- **MetricsManager (from H4)**: Tracks memory and CPU, but does not correlate this with network load (e.g. packet storms).

## 2. Identified Deficiencies
- **Missing Metrics:** No latency tracking, packet delivery rates, or hop counts.
- **Routing Blind Spots:** Duplicate packet suppression is happening silently; we don't know the broadcast efficiency of the mesh.
- **Transport Bottlenecks:** We cannot definitively say if a slow transfer is due to BLE MTU limits or Wi-Fi interference.
- **Latency Hotspots:** No tracking of Round Trip Time (RTT).
- **Missing Dashboards:** No unified StateFlow exists to feed a hypothetical UI dashboard.

## 3. Recommended Actions
- Implement `MeshAnalyticsManager` to aggregate these missing metrics.
- Introduce `PeerIntelligenceManager` to grade individual node stability.
- Build `TopologyManager` to construct an offline graph representation of the mesh.
- Use `PerformanceAdvisor` to proactively suggest optimization actions based on collected metrics.
