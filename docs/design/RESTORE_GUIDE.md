# Restore Guide

## Safe Restoration Operations
The `RecoveryManager` is the fail-safe for SQLCipher destruction or Room fatal errors.

### Constraints
- **Validation First**: It strictly checks the SHA-256 hash of the backup before attempting to restore.
- **Forensic Retention**: If a corrupt DB must be overwritten, it is first renamed to `corrupted_db_[timestamp].db` for administrative triage.
- **No Overwrite**: It will NEVER overwrite a DB if the primary DB passes the `IntegrityManager` check.
