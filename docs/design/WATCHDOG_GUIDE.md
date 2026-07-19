# Watchdog Engineering Guide

**Date:** July 14, 2026

## Overview
The `RuntimeWatchdog` is a lightweight daemon component that runs in the background of Mesh Link to detect silent stalls, hung coroutines, and thread starvation.

## Mechanism
1. **Registration/Pinging**: Critical components (like `MeshRelayService`) inject the `RuntimeWatchdog` and call `watchdog.ping("ComponentName")` in their main operational loops.
2. **Monitoring**: The Watchdog wakes up every 15 seconds to check the `componentPings` map.
3. **Threshold**: If any component has not pinged within the last `45,000ms` (45 seconds), it is considered stalled.
4. **Action**: The Watchdog invokes `SelfHealer.triggerRecovery("ComponentName")`.

## Integrating a New Component

To protect a new component with the Watchdog:

```kotlin
@Inject lateinit var watchdog: RuntimeWatchdog

// Inside your main work loop
while(isActive) {
    watchdog.ping("MyNewWorker")
    doHeavyLifting()
    delay(10_000L) // Must be < 45 seconds
}

// Ensure you clean up when done
override fun onDestroy() {
    watchdog.remove("MyNewWorker")
}
```

## Considerations
- Do not ping the watchdog from a UI click listener; ping it from the actual background thread performing the work.
- Keep the `TIMEOUT_MS` strictly at 45 seconds to accommodate for deep doze mode latency on older Android versions without triggering false positives.
