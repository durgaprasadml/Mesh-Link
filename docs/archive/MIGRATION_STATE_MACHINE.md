# Migration State Machine

## Overview
To prevent partial migrations or user lockouts in the event of process death, the simple `boolean` migration flag was replaced with a robust, persistent state machine backed by `EncryptedSharedPreferences`.

## States
1. **`NOT_STARTED`**: The default state. Indicates no migration has been attempted.
2. **`IN_PROGRESS`**: Set immediately before opening the legacy database. If the app crashes (OOM, battery loss, or user swipe), the next launch will read `IN_PROGRESS`. Since the legacy key hasn't been deleted, the migration will safely resume.
3. **`VERIFIED`**: Set only after the database is successfully re-opened with the *new* key and the schema is validated. Once set, the legacy key is deleted, and the migration never runs again.
4. **`FAILED`**: Set if any exception is caught during the rekey or verification process. This acts as a circuit breaker.

## Benefits
This architecture ensures that the migration is idempotent. It can be safely retried as many times as necessary without corrupting the database or stranding the user.
