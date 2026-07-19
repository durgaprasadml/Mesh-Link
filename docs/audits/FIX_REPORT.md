# Fix Report

## Overview
The legacy database migration logic in `DatabaseSecurityManager.kt` was successfully refactored to eliminate the `SQLiteOutOfMemoryException` production crash while fully preserving user data and avoiding destructive database recreation.

## Structural Changes Implemented

1. **Transaction Removal for PRAGMA rekey:**
   - **Old Behavior:** `db.execSQL("PRAGMA rekey = ...")` was wrapped in a `BEGIN IMMEDIATE` and `COMMIT` block. SQLCipher strictly prohibits rekeying inside a transaction, which guaranteed a silent exception.
   - **New Behavior:** Removed the transaction entirely. `PRAGMA rekey` is now executed in a standard non-transactional context.

2. **WAL Mode Suspension:**
   - **Old Behavior:** The database was opened, and `PRAGMA rekey` was attempted while the database remained in `WAL` mode (set during Room's earlier initialization). SQLCipher forbids rekeying a WAL mode database.
   - **New Behavior:** `db.execSQL("PRAGMA journal_mode = DELETE;")` is explicitly executed prior to rekeying to satisfy SQLCipher's requirements, and restored to `WAL` immediately after.

3. **String vs ByteArray Password Compatibility:**
   - **Old Behavior:** The legacy passphrase was passed as a Java `String` to `SQLiteDatabase.openDatabase(...)`.
   - **New Behavior:** The legacy passphrase is converted to a UTF-8 `ByteArray` before being passed to `openDatabase(...)`. This matches the original encryption context utilized by `SupportOpenHelperFactory` and ensures the legacy key decrypts the database flawlessly during migration.

4. **Boolean Status and Safe Fallback:**
   - **Old Behavior:** `migrateDatabaseIfNeeded()` returned `Unit`, swallowed exceptions, and prompted the caller to indiscriminately delete the legacy passphrase from `SharedPreferences`.
   - **New Behavior:** The function now returns a `Boolean`. If `false` (migration fails for any reason), the legacy passphrase is **not deleted**. Instead, `getDatabasePassphrase()` logs a warning and returns the legacy passphrase's bytes, falling back to legacy encryption to prevent user lock-out and eliminate the `SQLiteOutOfMemoryException`.
