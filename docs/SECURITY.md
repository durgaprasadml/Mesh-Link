# Security Architecture

## 1. At-Rest Encryption
- **SQLCipher**: The Room database is fully encrypted using AES-256 via SQLCipher.
- **AndroidKeyStore**: The 256-bit database passphrase is hardware-backed and stored securely in the `AndroidKeyStore`.

## 2. In-Transit Encryption
- **ECDH (Elliptic Curve Diffie-Hellman)**: Used for secure key exchange over public/insecure BLE channels to prevent MITM attacks.
- **AES-GCM**: Once a session key is established via ECDH, all payloads (text, media) are encrypted using AES-256-GCM, providing both confidentiality and authenticity.

## 3. Protocol Security
- **Forward Secrecy**: Ephemeral keys ensure that compromised long-term keys do not compromise past communications.
- **Replay Protection**: Cryptographic nonces and sequence numbers prevent attackers from replaying intercepted packets.
- **Trust Management**: Devices establish internal trust scores. Devices with invalid signatures are dropped immediately.
