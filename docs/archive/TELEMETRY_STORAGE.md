# Telemetry Storage

## Offline-First Telemetry
Because Mesh Link is an offline platform, cloud-based telemetry (like Datadog or Crashlytics) is fundamentally insufficient for diagnosing mesh faults in the field.

### `TelemetryStore`
- Implements a bounded `ConcurrentLinkedQueue` limiting the backlog to 5000 `LogEvent`s.
- Avoids infinite storage growth.
- **`exportToDisk()`**: Allows an administrator to flush the buffer to a JSON file in the application's cache directory, which can then be zipped and exfiltrated over Wi-Fi Direct or transferred via physical connection (USB).
