# Business Continuity Plan

## Graceful Degradation
Android devices are unpredictable. The `BusinessContinuityManager` ensures Mesh Link survives OS-level crises.

### Disk Space Crisis
If a device drops below 5% available storage, `BusinessContinuityManager` flags `SystemDegradationLevel.CRITICAL`.
- **Response**: The app automatically halts large file transfers, media downloads, and deep diagnostic logging, preserving the remaining bytes exclusively for emergency text communications.
