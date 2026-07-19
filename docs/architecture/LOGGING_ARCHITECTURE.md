# Logging Architecture

## MeshLogger Overhaul
Mesh Link has migrated away from standard string-based logging to a highly structured `LogEvent` system.

### Components
1. **`LogEvent` Data Class**: Mandates explicit tagging (Category, Module, Component, Thread, trace ID) rather than ad-hoc string building.
2. **`MeshLogger`**: Acts as a central sink. When an event is logged, it passes through the `PrivacyLogInterceptor` to scrub PII.
3. **`TelemetryStore`**: The final destination for the logs, persisting them in a circular buffer for offline access.
