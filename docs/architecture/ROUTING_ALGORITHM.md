# Routing Algorithm Specification

Mesh Link utilizes a highly dynamic, weighted scoring algorithm (`RouteScorer.kt`) rather than traditional Distance-Vector (hop count only).

## Route Scoring Weights
A route score (0-100) is calculated per destination using the following weights:
- **25% Link Quality**: Normalized RSSI (-100 dBm to -40 dBm).
- **20% Reliability**: A composite of Historical Success Rate (60%) and Recent Packet Loss Rate (40%).
- **15% Congestion**: Penalizes paths running through nodes with high queue depths.
- **15% Battery**: Penalizes routing through critically low battery devices.
- **10% Latency**: Penalizes paths with high turnaround ping times.
- **5% Trust**: Prefers routing through cryptographically trusted nodes.
- **5% Stability**: Penalizes flapping routes.
- **5% Hops**: Prefers fewer jumps, but will take a 5-hop healthy route over a 1-hop dying route.

## Transport Bias
After the base score is calculated, the transport type applies an absolute boost:
- **Wi-Fi Direct**: +10 Score (Massive bandwidth advantage).
- **Hybrid**: +5 Score (Redundancy advantage).
- **BLE**: +0 (Baseline).

## Failover
If `RouteOptimizer.getOptimalRoute` determines the primary route is "Predicted to Fail" (e.g. Battery < 15%, or Congestion > 90%), it is instantly excluded from the primary selection, forcing a hot swap to the next best candidate.
