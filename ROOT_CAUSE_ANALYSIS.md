# Root Cause Analysis

## The Incident
Production users report an application crash immediately after login.
**Exception:** `android.database.sqlite.SQLiteOutOfMemoryException: out of memory (code 7) while compiling: SELECT COUNT(*) FROM sqlite_schema;`

## The Mechanism of the Crash
The `SQLiteOutOfMemoryException` (Code 7: `SQLITE_NOMEM`) in SQLCipher is a known symptom of attempting to open an encrypted database with an **incorrect passphrase**. 
When the wrong key is supplied, SQLCipher fails to decrypt the SQLite file header. It subsequently parses the gibberish cipher-text bytes as a valid SQLite header, frequently reading an astronomical, invalid page size. When SQLite attempts to allocate memory for this corrupted page size during Room's initial `sqlite_schema` query, the memory allocation (`malloc()`) fails, and the engine crashes.

## The Root Cause
The incorrect key is passed to SQLCipher due to a fatal flaw in the legacy password migration logic located in `DatabaseSecurityManager.kt`.

1. **The Migration Trigger**: On launch, the system detects an un-migrated legacy UUID passphrase and attempts to rekey the database to the new PBKDF2 secure passphrase using `migrateDatabaseIfNeeded()`.
2. **Transaction Violation**: The migration code executes `db.execSQL("PRAGMA rekey = ...")` inside a `BEGIN IMMEDIATE` transaction. **SQLCipher strictly forbids executing `PRAGMA rekey` within a transaction**, causing the command to throw an exception.
3. **WAL Mode Violation**: Room previously configured the database into Write-Ahead Logging (WAL) mode. **SQLCipher strictly forbids executing `PRAGMA rekey` on a database in WAL mode**, requiring it to be reverted to `DELETE` mode first. This guarantees a secondary exception.
4. **The Silent Failure**: The `migrateDatabaseIfNeeded()` function wrapped the execution in a `try-catch` block that captured the exception, logged it to Firebase Crashlytics, but **swallowed it**, preventing the caller from knowing the migration failed.
5. **The Lockout**: Unaware of the failure, the caller confidently deleted the legacy passphrase from `SharedPreferences` and marked the migration as complete.
6. **The Crash**: Room initializes and provides the NEW passphrase to SQLCipher. However, because the `PRAGMA rekey` failed, the database on disk is STILL encrypted with the OLD legacy passphrase. SQLCipher attempts to decrypt the old database with the new key, reads garbage data, and crashes.

## Evidence
- SQLCipher Documentation: *"PRAGMA rekey... must not be called from within a transaction."*
- SQLCipher Documentation: *"If the database is operating in WAL mode, it must be changed to journal_mode=DELETE prior to the rekey operation."*
- Codebase: `migrateDatabaseIfNeeded()` swallows `Exception` and does not return a success/failure state, leading to premature deletion of the legacy key.
