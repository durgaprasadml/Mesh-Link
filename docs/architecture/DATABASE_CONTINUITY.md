# Database Continuity

## Preventing Fatal Exceptions
The `DatabaseContinuityManager` acts as the primary shield for the SQLCipher/Room integration.

### Logic Flow
1. **Boot**: `ensureDatabaseContinuity()` is invoked before `Room.databaseBuilder`.
2. **Integrity Validation**: Passes the raw `.db` file to `IntegrityManager`.
3. **Backup Fallback**: If corrupt, it immediately requests the `RecoveryManager` to hot-swap the database file with the latest backup.
4. **Safe Failure**: If no backup exists, it refuses to silently overwrite the DB. This prevents the classic Android issue where a minor SQL error results in the app wiping a user's entire identity and chat history to "start fresh".
