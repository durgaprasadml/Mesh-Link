# Crash Correlation

## Context Injection
Standard Android crash reports (like those sent to Firebase or Logcat) only contain the raw stack trace. In a decentralized offline system, the stack trace alone is often useless without knowing the state of the Mesh.

### `MeshCrashReporter` Updates
- **Diagnostics Payload:** When a non-fatal crash occurs, `MeshCrashReporter` immediately queries `DiagnosticsManager.exportDiagnosticsJson()`.
- **Appending:** This JSON payload is appended directly into the crash log. It contains the 50 most recent trace events, metrics (heap size), and the active connection tally.
- **Enterprise Benefit:** Allows field technicians to analyze *why* the app failed based on memory or network topology, entirely offline.
