# Trace Manager Guide

## Distributed Tracing in Mesh Link
To solve the problem of correlating multiple log entries to a single user action (like "Send Message"), we introduced `TraceManager`.

### Mechanism
- Uses `ThreadLocal` storage for synchronous `currentOperationId`.
- **`beginTrace()`**: Generates a UUID and injects it into subsequent `LogEvent` creations.
- **`trace { ... }`**: A scoped inline function that automatically bounds a trace around a block of code, ensuring it emits start/end metrics regardless of exceptions.

This allows developers to filter the `TelemetryStore` dump by `traceId` and see exactly where a message failed.
