# Performance Advisor

## Intelligent Offline Analytics
The `PerformanceAdvisor` is an expert system that consumes data from `MeshAnalyticsManager`, `RoutingAnalytics`, and `TransportAnalytics`.

### Autonomous Detection
Rather than relying on human interpretation of raw numbers, it generates actionable `OptimizationRecommendation` objects.
- **Example 1**: If Duplicate Suppression is > 60%, it outputs a `WARNING` for Broadcast Flooding.
- **Example 2**: If RTT > 1500ms, it outputs a `CRITICAL` recommendation to switch to Wi-Fi Direct.

**Note:** The Advisor does *not* automatically modify network parameters. It strictly acts as a telemetry advisor to preserve deterministic routing logic.
