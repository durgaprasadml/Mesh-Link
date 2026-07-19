# Self-Healing System Report

**Date:** July 14, 2026

## Overview
Mesh Link now features an automated Self-Healing system (`SelfHealer.kt`) designed to silently recover broken background processes without requiring the user to restart the app.

## Playbooks

### 1. MeshRelayService Recovery
- **Trigger**: `RuntimeWatchdog` fails to receive a ping from `MeshRelayService` within 45 seconds.
- **Action**: A new `Intent` with `ACTION_START` is dispatched to Android's `startForegroundService()`.
- **Result**: Android recreates the Service or delivers the intent to the existing stuck instance to forcefully kickstart the background loop.

### 2. BleScanner Recovery
- **Trigger**: Bluetooth GATT internal state machine gets stuck (often seen on older Android devices when left running for > 24 hours).
- **Action**: `SelfHealer` calls `scannerManager.stopScanning()` and immediately calls `scannerManager.startScanning()`.
- **Result**: Re-registers the scanner callback with the OS, resetting the internal hardware buffer.

### 3. BleAdvertiser Recovery
- **Trigger**: Advertiser drops due to hardware limits or OS power management.
- **Action**: Stops and restarts the BLE advertisement payloads via `BleAdvertiserManager`.

### 4. Database Recovery
- **Trigger**: Long-running SQLite queries leading to `SQLITE_BUSY` or deadlock detection.
- **Action**: Currently marked for future expansion. The recommended approach is to close the `AppDatabase` singleton and allow Dagger/Hilt to re-instantiate it on the next query.

## Backoff Strategy
All recovery triggers are rate-limited via a 30-second exponential backoff map (`lastRecoveryTime`) to ensure Mesh Link does not enter an infinite restart loop if the hardware itself is fatally broken.
