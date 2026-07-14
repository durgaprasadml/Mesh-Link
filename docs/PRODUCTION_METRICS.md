# Production Metrics (KPIs)

## Overview
Because Mesh Link disables cloud telemetry to ensure privacy and offline functionality, Production Metrics are strictly evaluated via **Play Store Console Vitals** and **Opt-In MDM Log Shipping**.

## Primary KPIs
1. **Crash-Free Sessions:** Target: **99.8%**. (Monitored via Google Play Vitals).
2. **ANR (Application Not Responding) Rate:** Target: **< 0.47%**. Spikes indicate SQLite locking or main-thread WorkManager blocking.
3. **Battery Vitals:** Google Play tracks excessive WakeLock usage. `MeshRelayService` is strictly tuned to prevent "Stuck WakeLock" flags.
4. **Discovery Success Rate:** Evaluated during manual QA. The time it takes for Node A to appear on Node B's UI should be < 5 seconds.
