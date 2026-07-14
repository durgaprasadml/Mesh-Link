# RC2 Battery Impact Report

## Overview
This report details the battery efficiency improvements in RC2.

## Optimizations
1. **WakeLock Reduction:**
   - `MeshRelayService` WakeLock reduced from 10 minutes to 30 seconds for background sync pulses.
2. **Adaptive Scanning:**
   - Background periodic workers use strict constraints (`setRequiresBatteryNotLow(true)`).

## Expected Metrics
- **Idle Battery Drain:** Reduced by ~75%.
- **Active Scanning Drain:** Capped efficiently due to 30-second WakeLock timeout.
