# Configuration Guide

Mesh Link V3.0 is highly configurable to suit different application requirements (e.g., high-bandwidth vs. low-power).

## Runtime Configuration

Configuration is passed via the `MeshConfig` object during initialization.

```kotlin
val config = MeshConfig.Builder()
    .setMtuSize(512)
    .setRoutingTtl(15)
    .enableWiFiDirect(true)
    .build()

MeshManager.init(context, config)
```

## Feature Flags

Certain experimental or heavy features can be toggled:

*   `FLAG_ENABLE_METRICS`: Enables or disables the internal metrics collection.
*   `FLAG_STRICT_SECURITY`: Enforces the highest security protocols, rejecting legacy downgrade attempts.

## Logging Configuration

Logging verbosity can be adjusted per module.

```kotlin
Logger.setLevel(Module.ROUTING, LogLevel.DEBUG)
Logger.setLevel(Module.TRANSPORT, LogLevel.WARN)
```

For production, it is recommended to set all levels to `ERROR` or disable logging entirely to improve performance and save battery.

## Metrics Configuration

If metrics are enabled, you can configure the snapshot interval and local export path.

```kotlin
MetricsConfig.setSnapshotInterval(Duration.ofMinutes(5))
MetricsConfig.setExportPath("/local/app/dir/diagnostics.json")
```

## Performance Tuning

*   **Low Latency:** Decrease advertising intervals (consumes more battery).
*   **High Throughput:** Force Wi-Fi Direct connections for payloads larger than a specific threshold.
*   **Dense Networks:** Decrease routing TTL and increase the duplicate cache size to prevent broadcast storms.
