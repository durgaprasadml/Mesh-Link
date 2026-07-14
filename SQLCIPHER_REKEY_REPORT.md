# SQLCipher Rekey Report

## The Challenge
SQLCipher 4.9.0 explicitly prohibits changing a database's encryption key (using `PRAGMA rekey`) while inside a transaction. The previous implementation attempted to execute the rekey within a `BEGIN IMMEDIATE` / `COMMIT` block, which resulted in a silent, swallowed exception that left the database un-migrated.

## The Solution
The `BEGIN IMMEDIATE`, `COMMIT`, and `ROLLBACK` wrappers have been entirely eradicated from `DatabaseSecurityManager.kt`. 
`PRAGMA rekey` is now executed as a standalone statement on a pristine database connection. This satisfies SQLCipher's engine constraints and allows the encryption pages to be re-written flawlessly.

## Verification
To ensure that the rekey actually succeeded (and to prevent the `SQLiteOutOfMemoryException`), an explicit verification step was added. The database is closed immediately after the rekey, and then re-opened using the new key. A raw query (`SELECT COUNT(*) FROM sqlite_schema;`) is executed to ensure the header decrypts properly before the legacy key is discarded.
