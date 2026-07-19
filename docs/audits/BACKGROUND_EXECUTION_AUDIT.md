# Phase H3 - Background Execution Audit

**Date:** July 2026  
**Status:** Completed  
**Objective:** Identify battery waste, duplicate workers, unnecessary wakeups, and lifecycle leaks in Mesh Link's background stack.

## 1. WorkManager
- **RetryWorker:** Missing strict constraints. It currently executes without checking if the device is in Doze mode or low battery, causing excessive wakeups. It lacks exponential backoff configuration.
- **CleanupWorker:** Currently only checks for DeviceIdle and Charging. Can be optimized to batch work better.

## 2. MeshRelayService & WakeLocks
- **WakeLock Usage:** In Phase H2, we added a robust lock, but it lacks a strong `try-finally` around the exact block of code doing the work. If the work takes longer than 30s or hangs, we rely entirely on the 30s timeout, but this can cause partial locks if requested too frequently.
- **Unnecessary Restarts:** The alarm manager restarts the service every 3s if it dies. If the OS is intentionally killing the app due to extreme memory or battery pressure (App Standby Bucket: Restricted), this loop will severely penalize the app's Vitals score.

## 3. BLE Scanning & Advertising
- **Continuous Mode:** Currently, the BLE scanner operates at a static power profile (likely `SCAN_MODE_LOW_LATENCY`) regardless of whether the screen is on or the battery is at 5%. This is a massive battery drain.

## 4. Lifecycle Leaks
- **Process Lifecycle:** Long-running coroutines aren't strictly bounded by the `ProcessLifecycleOwner`. Work continues at high priority even when the app is backgrounded.

## Conclusion
Mesh Link requires a dynamic power manager that throttles mesh operations according to the device's battery state, Doze Mode, and App Standby Buckets.
