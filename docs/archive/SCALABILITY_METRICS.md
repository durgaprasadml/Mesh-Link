# Scalability Metrics

## Real-Time Scale Observability
The `ScalabilityMetricsManager` exposes vital statistics proving the network's viability at scale.

### Core Metrics
- **Average & Worst Latency**: Essential for ensuring that hopping across a 100-node graph doesn't degrade text delivery below human acceptable speeds (e.g., keeping RTT under 2000ms).
- **Broadcast & Discovery Efficiency**: Values closer to 1.0 indicate zero wasted radio cycles. Drops indicate overlapping advertisements.
- **Worker Utilization**: Ensures the Android `WorkManager` and coroutine dispatchers aren't deadlocking under the weight of incoming packets.
