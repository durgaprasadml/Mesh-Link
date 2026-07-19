# Database Recovery Report

## Enhancements
The `DatabaseModule` has been heavily refactored to treat production databases as mission-critical entities.

### Changes
1. **Active PRAGMA Parsing:** `PRAGMA integrity_check` is no longer a blind call. The resulting cursor is read. If the return value is not exactly `"ok"`, an exception is thrown, halting initialization safely.
2. **Anti-Recreation Rule:** Previous behavior silently deleted the database if it was unlocked and threw `SQLiteNotADatabaseException`. This has been completely removed to prevent permanent data loss in edge-cases. The app now aborts and preserves the DB file for forensic manual recovery.
3. **FBE Check:** Kept existing device lock checks to accurately identify FBE states instead of actual corruption.
