# Disaster Recovery & Data Portability

## Scenarios
1. **Corrupted Database Recovery:** 
   - SQLite Write-Ahead Logging (WAL) prevents 99% of corruption events.
   - If an unrecoverable corruption is detected by `SupportSQLiteOpenHelper`, the app gracefully deletes the corrupt DB and reconstructs a fresh routing table to prevent crash loops.
2. **Data Export:**
   - Users can generate an encrypted backup archive of their chats and cryptographic keys, protected by a user-supplied password (AES-256 encrypted).
3. **Device Migration:**
   - The backup archive can be imported onto a new device, seamlessly restoring the user's cryptographic identity and chat history.
