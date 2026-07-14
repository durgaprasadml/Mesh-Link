# Database Testing Guide

This document outlines the testing architecture and conventions for Mesh Link's Persistence layer (Room DB, SQLCipher, caching, etc.), establishing a foundation for a robust, enterprise-grade automated testing suite.

## Database Testing Architecture

The project follows a localized, hermetic approach to database testing, executing largely under Robolectric rather than requiring a connected Android device.

1. **Entity & Schema Validation**: Ensures `Data Classes` mapped to Room `@Entity` annotations correctly model their business requirements, relationships, and nullability.
2. **DAO CRUD & Transactions**: Ensures queries execute flawlessly and return the correct structures using `Room.inMemoryDatabaseBuilder()`. Tests ensure that complex `@Transaction` blocks commit atomically or rollback on failure.
3. **Reactive Flow Queries**: Uses `Turbine` to subscribe to Room `Flow` queries and guarantees that when underlying tables change, the new data is automatically and correctly emitted.
4. **Concurrency & Thread Safety**: Launches intensive parallel coroutine jobs inserting bulk rows simultaneously to ensure standard Room locks operate smoothly, preventing `SQLiteDatabaseLockedException`.
5. **Security & Migrations**: Explores `DatabaseSecurityManager` logic, which transparently wraps `SQLCipher` migrations to PBKDF2 derived keys and Keystore encryptions.

## Running Tests

To run the database test suite locally from the command line:

```bash
# Run all unit tests for the InternalDebug variant
./gradlew :app:testInternalDebugUnitTest
```

Reports are generated at:
`app/build/reports/tests/testInternalDebugUnitTest/index.html`

## Current Test Coverage
- **Entities**: `UserEntityTest`, `ChatEntityTest`, `MessageEntityTest`, `RelayPacketEntityTest`, `TrustEntityTest`, `AuditLogEntityTest`
- **DAOs**: `UserDaoTest`, `ChatDaoTest`, `RelayDaoTest`, `TrustDaoTest`, `AuditLogDaoTest`
- **Concurrency**: `DatabaseConcurrencyTest`
- **Security & Storage**: `DatabaseSecurityManagerTest`, `CacheManagerTest`
- **Migrations**: Since the project is utilizing `fallbackToDestructiveMigration` via Hilt configuration in dev variants, `MigrationTest` contains a placeholder infrastructure. Real world SQLCipher legacy string migrations are verified via `DatabaseSecurityManagerTest`.

## Common Failure Scenarios & Fixes

- **Robolectric Keystore errors**: The `DatabaseSecurityManager` tests instantiate Keystore instances. If it complains about providers, ensure your JVM has BouncyCastle initialized or allow Robolectric to simulate Android's default providers via SDK API levels.
- **Main Thread Query Exceptions**: If you get exceptions about accessing the database on the main thread, ensure you chain `.allowMainThreadQueries()` onto `Room.inMemoryDatabaseBuilder()` in your `@Before` block, since Robolectric runs test code synchronously on a single test thread by default.
- **Corrupted test DBs**: Always ensure you cleanly instantiate and close your `MeshDatabase` reference inside `@Before` and `@After` via `database.close()` to prevent one test polluting the next.
