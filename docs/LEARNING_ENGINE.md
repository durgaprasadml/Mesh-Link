# Offline Learning Engine

Mesh Link implements a lightweight, rolling-statistic based learning engine entirely in Kotlin.

## Data Storage
- **In-Memory**: `ConcurrentHashMap` for zero-latency lookups during the critical path of routing.
- **Disk**: Flushed to SharedPreferences (`mesh_ai_learning.xml`) asynchronously via Coroutines to persist learning across app reboots.

## Learning Models

### Exponentially Weighted Moving Average (EWMA)
Most metrics (like `avg_queue_depth` or `avg_broadcast_rate`) are updated using an EWMA:
```kotlin
val newBaseline = (baseline * 0.999f) + (currentRate * 0.001f)
```
This ensures the system slowly adapts to new "normals" without overreacting to temporary spikes, allowing it to accurately detect true anomalies.

### Historical Success Ratios
For `RoutePredictionEngine` and `TransportPredictor`, we track raw aggregate counts:
- `totalPacketsSent` vs `totalPacketsDelivered`
- `successfulWifiConnections` vs `successfulBleConnections`

When predicting transport, the system analyzes the ratio of Wi-Fi successes to BLE successes for a specific peer, dynamically adjusting its routing behavior based on that specific device's hardware reliability.
