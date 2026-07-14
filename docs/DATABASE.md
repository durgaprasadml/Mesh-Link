# Database Architecture

## Room + SQLCipher
The persistence layer is powered by **Room**, layered on top of **SQLCipher** for AES-256 encryption.

## Entities
1. **UserEntity**: Stores identity details, cryptographic public keys, and trust scores.
2. **ChatEntity**: Represents a conversation thread (Direct or Group).
3. **MessageEntity**: Stores individual messages, media URIs, delivery status (ACK/Pending/Failed), and timestamps.
4. **RelayEntity**: Manages the known routing table for multi-hop BLE forwarding.
5. **AuditLogEntity**: Immutable ledger for security events and application lifecycle diagnostics.

## Concurrency
Room DAOs expose Kotlin `Flow` for reactive UI updates. Read/Write operations are strictly dispatched to `Dispatchers.IO` to prevent Main thread blocking during heavy cryptography or JSON serialization.
