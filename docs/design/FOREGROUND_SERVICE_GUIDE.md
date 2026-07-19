# Foreground Service Guide

## MeshRelayService Modifications
To comply with Android 14+ background execution policies, `MeshRelayService` now behaves dynamically depending on the active Doze state.

### WakeLock Scoping
Historically, `MeshRelayService` held a partial WakeLock for the entire duration of a sync pulse via separate `acquire()` and `release()` calls, which risked leaking the lock on catastrophic native errors. 

**Solution:** Introduced `withWakeLock(timeoutMs) { ... }`, a structured concurrency wrapper that leverages a `try-finally` block to guarantee lock releases, even during coroutine cancellation.

### Doze Mode Interception
Before issuing a BLE auto-start refresh cycle every 120 seconds, the service queries `PowerStateManager`. If the device is in Doze mode, it skips the cycle, maintaining OS compliance.
