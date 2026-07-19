# Power Metrics Guide

## Overview
The `PowerMetricsManager` provides a rudimentary but effective telemetry system for diagnosing background battery drain locally on the user's device.

## Monitored Metrics
- **Active CPU Time:** Total elapsed time since the session started.
- **BLE Scan Duration:** Time spent actively running Bluetooth LE operations.
- **WakeLock Duration:** Time spent holding partial wake locks.
- **Power Score:** A 0-100 score that degrades heavily if the BLE radio active time exceeds 50% or 80% of the total CPU time, or if wake locks exceed 5 minutes.

## Usage
Currently outputs warnings to `Logcat` (`MeshLogger`). Can be exposed in future UI iterations for users to monitor the application's battery health.
