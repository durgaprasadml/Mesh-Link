# Crash Recovery Report

## Objective
Detail the changes to `CrashRecoveryManager` and `MeshRelayService` handling unexpected termination.

## Core Features
1. **Uncaught Exception Hooking:** Captures fatal threads but intentionally omits the clean exit marker to trigger recovery on the next launch.
2. **Foreground Service Hardening:** `MeshRelayService` now safely checks if it was killed via OOM or battery optimizer and uses `AlarmManager.setWindow` as a resilient backoff if exact alarms are denied by the OS.
3. **Session Restoration Trigger:** Invokes `MeshRepository.autoStartMesh()` instantly if the app restarts without a clean exit marker, minimizing mesh downtime.
4. **WakeLock Fixes:** Proper checks for `isHeld` prevent redundant acquisitions and crashes during release cycles.
