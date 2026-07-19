# Routing Analytics

## Understanding Broadcast Efficiency
In a MANET (Mobile Ad-Hoc Network), broadcast flooding is a major battery drain.

### `RoutingAnalytics` Class
This engine tracks:
- **Cache Hit Ratio**: How often we use a known route vs performing a flood.
- **Duplicate Suppression**: When Node A receives the same packet from Node B and Node C, it drops the second. If this rate is > 50%, the network is heavily congested.

By monitoring these, administrators can tune the TTL (Time-To-Live) parameters to limit the flood radius.
