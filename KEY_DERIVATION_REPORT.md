# Key Derivation Report

## Keystore Architecture
- **Provider**: `AndroidKeyStore`
- **Algorithm**: `AES/GCM/NoPadding` (256-bit)
- **StrongBox**: Automatically utilizes StrongBox backing (`setIsStrongBoxBacked(true)`) if available (API 28+), with graceful fallback to standard hardware-backed Keystore.
- **Authentication**: The key is *not* bound to `setUserAuthenticationRequired(true)`, meaning it survives biometric changes and device lock states.

## Database Passphrase Generation
- **Seed**: A 32-byte Cryptographically Secure Pseudo-Random Number Generator (CSPRNG) array, encrypted at rest via the Android Keystore.
- **Salt**: A 16-byte CSPRNG array.
- **Derivation Algorithm**: `PBKDF2WithHmacSHA256`
- **Iterations**: `100,000` (Defined in `SecurityConfig.PBKDF2_ITERATIONS`)
- **Output Length**: 256 bits

## Stability Verification
The key derivation logic is highly deterministic:
1. `seed` and `salt` are persistently stored as Base64 strings in `EncryptedSharedPreferences`.
2. Keystore decryption yields the exact identical `seed` byte array.
3. The PBKDF2 execution on the identical seed and salt guarantees an identical derived passphrase.
4. Memory hygiene processes (`Arrays.fill()`) correctly wipe temporary arrays without corrupting the finalized Base64 immutable String used for SQLCipher.

The exact same passphrase is produced across launches. The crash is not caused by Keystore derivation instability.
