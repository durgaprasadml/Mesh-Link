# Doze Mode Report

## Handling Doze (Device Idle)
When Android enters Doze Mode, network access and CPU wakeups are strictly deferred to maintenance windows.

### Mesh Link's Strategy
- **Detection**: `PowerStateManager` listens to `PowerManager.isDeviceIdleMode`.
- **Mitigation**: Rather than attempting to break out of Doze via exact alarms (which Android restricts for non-clock apps), Mesh Link actively suppresses its own internal loops (`MeshRelayService` skips syncs). 
- **Recovery**: When the intent fires that Doze mode is disabled, `AdaptiveMeshPowerManager` instantly issues an `autoStartMesh()` command, minimizing the reconnection latency back to the offline network.
