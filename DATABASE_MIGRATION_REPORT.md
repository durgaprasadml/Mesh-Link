# Database Migration Report

## Migration Strategy
The application successfully migrated from a legacy UUID-based database encryption key to a cryptographically secure PBKDF2-derived key (from a Keystore-encrypted seed). 

## Implementation
The migration utilizes SQLCipher's `PRAGMA rekey` capability. Because of SQLCipher constraints, the migration process was fundamentally refactored into a persistent State Machine to guarantee atomicity and prevent data loss.

## Execution Flow
1. The app verifies if the state is `VERIFIED`. If not, it checks for a legacy key.
2. The state is marked as `IN_PROGRESS`.
3. The database is opened using the legacy key (converted to a UTF-8 ByteArray).
4. The database is toggled to `journal_mode = DELETE` to satisfy SQLCipher's rekey requirements.
5. The `PRAGMA rekey` is executed to change the database key without using transactions.
6. The database is closed, finalizing the encryption change.
7. The database is re-opened with the new derived key.
8. The schema is validated via `SELECT COUNT(*) FROM sqlite_schema;`.
9. The state is updated to `VERIFIED` and the legacy key is deleted.
