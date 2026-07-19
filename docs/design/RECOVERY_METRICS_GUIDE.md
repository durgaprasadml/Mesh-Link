# Recovery Metrics Guide

## Observability of Fault Tolerance
The `RecoveryMetricsManager` exposes a `StateFlow` that tracks the real-time durability of the node.

### Metrics Tracked
- **recoveryAttempts**: How many times the app tried to save itself.
- **successfulRecoveries**: How many times a DB swap or chunk resume worked.
- **totalIntegrityFailures**: Number of times the CRC32 or SHA-256 hashes failed.
- **continuityScore**: A dynamically computed float (0.0 to 1.0) grading the health of the local system based on penalties from failures.
