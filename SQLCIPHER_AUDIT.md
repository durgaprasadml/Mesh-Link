# SQLCipher Audit Report

## SQLCipher Configuration
- **Dependency Version**: `net.zetetic:sqlcipher-android:4.9.0`
- **Native Library**: Embedded via the SQLCipher dependency (AAR contains `libsqlcipher.so` for `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`).
- **Framework Fallback**: Uses `androidx.sqlite:sqlite-ktx:2.4.0` for Room framework bindings.

## Cryptographic Setup
- **Page Size**: `4096` bytes (SQLCipher 4 default).
- **KDF Algorithm**: PBKDF2-HMAC-SHA512 (SQLCipher 4 default).
- **KDF Iterations**: `256000` (SQLCipher 4 default).
- **HMAC Algorithm**: HMAC-SHA512.

## Compatibility and Integrity
- **Database Format**: The database files on disk are standard SQLCipher 4 databases. The version format is identical between the previously used 4.6.1 and current 4.9.0.
- **Compatibility Mode**: No SQLCipher 3 compatibility mode (`PRAGMA cipher_page_size = 1024;`) is active, nor is it required.
- **Library Match**: The native library matches the expected database format perfectly.

## Crash Evaluation
The `out of memory (code 7)` is **not** caused by a library version mismatch, ABI degradation, or an incompatible page size due to a SQLCipher 3 to 4 upgrade. It is explicitly caused by the database engine receiving an **invalid cryptographic key**. When provided with an invalid key, SQLCipher fails to decrypt the first page of the database and subsequently parses raw cipher-text as the SQLite header, leading to anomalous memory allocations.
