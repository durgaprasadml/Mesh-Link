# Android Compliance Report

## Compatibility
Mesh Link has been hardened to comply with restrictions introduced in Android 13, 14, 15, and 17.

### Strict Foreground Service Types (Android 14+)
While Mesh Link was using appropriate types (like `dataSync`), it was risking termination due to excessive polling during Doze. This is resolved via `PowerStateManager`.

### Exact Alarm Denial (Android 14+)
Fixed in Phase H2, but further solidified here by simply obeying Doze state rather than trying to force wakeups.

### App Standby Buckets (Android 15+)
The adaptive power scaling naturally complies with Restricted and Rare buckets by dropping `MeshRelayService` refresh attempts when the intent broadcasts signal a low power state.
