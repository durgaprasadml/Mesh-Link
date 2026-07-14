# Background Execution Report

This document details how Mesh Link manages background execution for uninterrupted relay operations.

## Foreground Service
- **Service Name**: `MeshRelayService`
- **Type**: `connectedDevice` (compliant with Android 14+).
- **Behavior**: It maintains a persistent notification. It holds a partial `WakeLock` to ensure BLE operations are not suspended.

## Boot Recovery
- **Receiver**: `BootCompletedReceiver`
- **Behavior**: Listens for `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`. It attempts to launch `MeshRelayService`.
- **Resilience**: It catches `ForegroundServiceStartNotAllowedException` on Android 12+ devices where background launches are restricted.

## WakeLocks
- `MeshRelayService` acquires a `PARTIAL_WAKE_LOCK` upon creation to prevent the CPU from sleeping while actively routing packets.
- The WakeLock is refreshed periodically during the BLE refresh cycle to prevent OS-enforced timeouts.

## AlarmManager vs WorkManager
- The app uses `AlarmManager` to schedule a restart intent if the service is killed (`onTaskRemoved`).
- **Android 14 Mitigation**: It attempts to use `setAndAllowWhileIdle` but falls back to `set` (inexact) if the `SCHEDULE_EXACT_ALARM` permission is missing.

## Battery Optimization
- Users are prompted to ignore battery optimizations.
- BLE Scans are restarted every 15 minutes to prevent the OS from downgrading the scan to opportunistic mode, ensuring consistent discovery while balancing battery usage.
