# Backup Architecture

## Encrypted Rolling Backups
Enterprise operations require zero data loss. Mesh Link's `BackupManager` executes a rolling strategy.

### Mechanics
1. A healthy snapshot of the `.db` file is taken whenever `DatabaseContinuityManager` verifies structural integrity.
2. The file is cloned to an internal, un-exported directory as `mesh_db_backup.enc`.
3. A `SHA-256` hash of this backup is written alongside it to prevent the recovery engine from restoring a corrupted backup.
4. Temporary files, routing caches, and Bluetooth GATT states are inherently excluded.
