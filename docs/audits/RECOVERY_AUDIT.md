# Phase H2 - Recovery Audit Report

**Date:** July 2026  
**Status:** Completed  
**Objective:** Identify crash points, recovery gaps, unrecoverable states, and duplication vectors in Mesh Link's background and networking stack.

## 1. MeshRelayService
- **Crash Points:** Can be killed by OOM or battery optimizer. Background service start restrictions (Android 12+) can cause `ForegroundServiceStartNotAllowedException`.
- **Recovery Gaps:** AlarmManager restart logic uses inexact alarms without guaranteeing state persistence. WakeLock is released abruptly on exception.
- **Unrecoverable States:** If `meshRepository.autoStartMesh()` fails repeatedly, no backoff exists.

## 2. MeshRouter & Queues
- **Crash Points:** Process death during queue serialization or in-memory map modification.
- **Recovery Gaps:** Pending routing jobs are held in memory. If the app dies, unacknowledged jobs might be lost or duplicated when a peer reconnects.
- **Duplication Risks:** Without strong persistent transaction IDs for inflight messages, restarting a transfer might duplicate it on the receiving end.

## 3. BleRepositoryImpl & WifiDirectManager
- **Crash Points:** Bluetooth stack reset or Wi-Fi Direct framework crashes.
- **Recovery Gaps:** Lost sessions do not reliably self-heal if the native stack hangs. Needs a `RetryCoordinator` with exponential backoff.
- **Duplicate Initialization:** Restarting services can cause redundant scanning or advertising.

## 4. Database (SQLCipher)
- **Crash Points:** SQLiteNotADatabaseException due to FBE (File-Based Encryption) lock state.
- **Recovery Gaps:** FBE lock states are handled, but `PRAGMA integrity_check` is executed without actively parsing the result string for 'ok'.
- **Unrecoverable States:** Legacy key fallback exists, but if integrity check fails, it throws without attempting safe fallback reads.

## 5. Navigation & UI State
- **Recovery Gaps:** Android process death wipes `ViewModel` state unless `SavedStateHandle` or DataStore is used. Active chat, pending message drafts, and mesh status UI are lost on configuration change or low-memory kill.

## Conclusion
The audit reveals that while individual components have localized error handling, a unified, cross-component recovery engine is required to ensure fault tolerance.
