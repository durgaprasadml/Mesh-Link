# Health Score Guide

**Date:** July 14, 2026

## The Composite Health Score
Mesh Link computes a live `HealthScore` via `RuntimeHealthManager`. This score is a composite of device hardware metrics and internal system flags.

## Score Levels

### `EXCELLENT`
- All systems optimal.
- BLE & Wi-Fi are responding.
- Database queries are fast.
- JVM Heap is under 128 MB.
- No memory pressure from Android OS.

### `GOOD`
- System is healthy but under load.
- JVM Heap is between 128 MB and 256 MB.
- Normal operations continue, but large file transfers might be slightly slower.

### `WARNING`
- One non-critical subsystem has failed or memory is high.
- Example: BLE is offline, but Wi-Fi is working.
- Example: JVM Heap is over 256 MB (but not critical).
- Action: Analytics will flag this, and users might see a degraded performance indicator in the future.

### `CRITICAL`
- System is in danger of an imminent crash.
- Example: Android OS fired `TRIM_MEMORY_RUNNING_CRITICAL`.
- Example: Database is completely unresponsive.
- Example: Multiple subsystems (BLE + Wi-Fi) have failed simultaneously.
- Action: `CrashPreventionManager` drops caches, flushes routing tables, and throttles incoming data.

## Metrics Used
- `SystemResourceMonitor.ResourceMetrics` (Heap, Native memory, OS Trim Memory level)
- `DiagnosticsManager.SystemHealthState` (BLE status, DB status, Wi-Fi status)
