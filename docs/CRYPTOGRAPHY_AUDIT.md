# Cryptography Audit

This document validates the cryptographic primitives utilized within Mesh Link.

## Primitives & Usage
1. **Asymmetric Key Exchange (ECDH)**:
   - **Curve**: secp256r1 (NIST P-256).
   - **Usage**: Negotiates ephemeral shared secrets between two mesh peers. Guarantees Forward Secrecy (FS).
2. **Symmetric Encryption (AES-GCM)**:
   - **Key Size**: 256-bit.
   - **Usage**: Encrypts all message payloads and media chunks.
   - **Integrity**: GCM provides built-in authentication (AEAD) to detect tampering.
3. **Key Derivation (PBKDF2)**:
   - **Hash**: HMAC-SHA256.
   - **Iterations**: 600,000.
   - **Usage**: Derives the SQLCipher database key from a high-entropy master key stored in the AndroidKeyStore.

## Cryptographic Mitigations
- **Replay Protection**: Every AES-GCM payload includes a strictly monotonic Sequence Number (nonce) in the Additional Authenticated Data (AAD). Packets with old sequence numbers are immediately dropped.
- **Randomness**: All nonces and key generation rely exclusively on `java.security.SecureRandom`.
