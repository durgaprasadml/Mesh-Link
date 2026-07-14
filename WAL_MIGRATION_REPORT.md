# WAL Migration Report

## Write-Ahead Logging (WAL) Constraints
Room natively enables WAL mode (`PRAGMA journal_mode = WAL;`) for enhanced concurrency. However, SQLCipher places a strict requirement on the `PRAGMA rekey` command: it cannot be executed on a database while it is in WAL mode.

## Checkpoint and Journal Mode Switch
To safely migrate the database, the following sequence was implemented during the legacy key access phase:

1. **`PRAGMA wal_checkpoint(FULL);`**: Forces SQLite to sync the existing Write-Ahead Log into the main database file, ensuring no data is left behind in the `.wal` file before the journal mode changes.
2. **`PRAGMA journal_mode = DELETE;`**: Reverts the database to the legacy rollback journal mode, closing and deleting the `.wal` file. This satisfies SQLCipher's prerequisite for the rekey operation.
3. **`PRAGMA rekey = ...;`**: Safely alters the encryption key.

## Restoration
After the database is re-keyed and successfully verified with the new key, the connection explicitly restores WAL mode:
- `PRAGMA journal_mode = WAL;`
- `PRAGMA synchronous = NORMAL;`

This ensures that Room's performance characteristics remain unchanged post-migration.
