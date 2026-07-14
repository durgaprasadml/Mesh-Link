# Database Initialization Trace

The complete initialization path for the database and SQLCipher is documented below. This trace identifies the exact flow of execution up to the crash point.

1. **`MainActivity` / `Login`**
   - The user completes the login flow, transitioning the application state to authenticated.

2. **`AuthViewModel`**
   - Dispatches authenticated requests to the Repository layer.

3. **`Repository`**
   - The repository requires database access and triggers dependency injection (Hilt) to resolve `MeshDatabase`.

4. **`DatabaseModule.provideMeshDatabase` (DI)**
   - Hilt invokes the `@Provides` method for `MeshDatabase`.
   - The module depends on `DatabaseSecurityManager` and calls `databaseSecurityManager.getDatabasePassphrase()`.

5. **`DatabaseSecurityManager`**
   - Resolves the PBKDF2 derived passphrase (`securePassphraseString`).
   - Detects an incomplete migration from the legacy UUID passphrase.
   - Invokes `migrateDatabaseIfNeeded(legacyPassphrase, securePassphraseString)`.
   - **(Failure Point)**: The migration fails silently (due to a transaction around `PRAGMA rekey` and WAL mode incompatibility).
   - The `legacyPassphrase` is erroneously deleted from `SharedPreferences`.
   - The manager returns the UTF-8 bytes of the NEW passphrase, while the database on disk is still encrypted with the OLD legacy passphrase.

6. **`SupportOpenHelperFactory`**
   - Created with the incorrect `passphraseBytes` (the new key) returned by `DatabaseSecurityManager`.

7. **`Room` (MeshDatabase)**
   - Room builds the database and delegates physical database opening to the factory.
   - When Room runs its internal schema verification (`SELECT COUNT(*) FROM sqlite_schema;`), it accesses the database for the first time.

8. **`SQLCipher`**
   - Attempts to decrypt the SQLite database header using the wrong passphrase.
   - Decryption fails, causing SQLCipher to interpret the encrypted header bytes as raw plaintext metadata.
   - The corrupted pseudo-header results in an astronomical page size or malformed schema structure.
   - SQLite fails to allocate enough memory, crashing with `android.database.sqlite.SQLiteOutOfMemoryException: out of memory (code 7)`.
