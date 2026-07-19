# Phase H4 - Observability Audit

**Date:** July 2026
**Status:** Completed

## 1. Current State
- **MeshLogger**: Basic wrapper around `android.util.Log`. Lacks structured fields like thread IDs, trace boundaries, or component specific routing.
- **MeshCrashReporter**: Simply forwards exceptions to the Android Logcat. In a production environment, this provides no runtime context (memory, BLE state, DB health).
- **DiagnosticsManager**: Provides a basic JSON export of system health, but doesn't aggregate active trace events or a time-series of metrics.
- **MetricsManager**: Exists as a basic key-value map. Lacks CPU, frame drop, and heap memory tracking.
- **EventTimeline**: Circular buffer in memory, but loses all data on crash or process death.

## 2. Identified Deficiencies
- **Missing Logs:** Worker execution times, lifecycle state changes, database query times.
- **Duplicated Logs:** The routing engine spits out multiple debug lines per packet hop without a unified `TraceID`.
- **Missing Tracing:** No distributed tracing exists. We cannot follow a message from the sender's UI to the receiver's UI.
- **Privacy Risks:** Crash logs might include plain-text message previews or internal cryptographic keys inadvertently dumped by standard exception traces.

## 3. Recommended Actions
- Refactor `MeshLogger` to accept structured `LogEvent` objects.
- Introduce `PrivacyLogInterceptor` to automatically scrub PII.
- Build `TelemetryStore` to persist logs to disk securely.
- Create `TraceManager` for distributed span tracking.
