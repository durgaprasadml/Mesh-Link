# Diagnostics Center

## Aggregation Hub
`DiagnosticsManager` has been expanded from a simple health monitor into a robust diagnostics aggregator.

### JSON Structure
1. **Core Health:** Validates BLE, Wi-Fi, Database, and memory.
2. **Metrics:** Ingests dynamic hardware and software counters from `MetricsManager`.
3. **Timeline:** Aggregates milestone events.
4. **Telemetry & Tracing:** Fetches the circular buffer from `TelemetryStore`, formatting it into a timeline of granular operations with their respective `traceId`s.

This payload is exported securely via `exportDiagnosticsJson()`.
