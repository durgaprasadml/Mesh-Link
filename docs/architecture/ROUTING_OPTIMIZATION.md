# Routing Optimization

## Efficiency Scoring
Legacy shortest-path routing is fragile. It favors a 1-hop path with 90% packet loss over a stable 2-hop path.

### `RoutingOptimizationManager`
- Computes an **Efficiency Score** combining hop count, latency, and stability.
- Routes that historically drop packets or exhibit >500ms latency are heavily penalized.
- Ensures the routing table dynamically prefers stable enterprise bridges over fleeting peripheral nodes.
