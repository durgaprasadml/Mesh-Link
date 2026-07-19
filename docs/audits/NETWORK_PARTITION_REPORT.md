# Network Partition Simulation Report

## Scenario: The Split Brain
- **Topology:** Group A (50 users) geographically separates from Group B (50 users).
- **Behavior:** `MeshRouter` actively detects failed next-hops and gracefully flags `isVerified = false` in `RouteCache`.
- **Offline Queuing:** `Room` DB holds `PENDING` messages in persistent storage asynchronously.

## Recovery Protocol
Upon reunification of Group A and B:
1. `RouteHealthMonitor` rapidly rebuilds adjacencies.
2. `RetryWorker` orchestrates the bulk-replay of `PENDING` items utilizing adaptive pacing to prevent collision storms.

**Status:** Certified Split-Brain resilient.
