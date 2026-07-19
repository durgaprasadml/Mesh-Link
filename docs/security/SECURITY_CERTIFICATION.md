# Security Certification (v2.0)

## 1. Cryptographic Architecture
Mesh Link utilizes state-of-the-art encryption protocols designed for absolute privacy in zero-trust environments.
- **Symmetric Encryption:** AES-256-GCM is used for all payloads (Text, Media, Voice, Video).
- **Key Exchange:** Elliptic-Curve Diffie-Hellman (ECDH) over Curve25519 is used for perfect forward secrecy.
- **Key Derivation:** PBKDF2 with SHA-256 secures local database keys.

## 2. Trust Model & Role-Based Access
- The `TrustManager` enforces a multi-tiered trust system (`UNKNOWN`, `VERIFIED`, `COMMAND`).
- Nodes identified as malicious are instantly downgraded to `BLOCKED`.
- `MeshRouter` actively inspects the trust level of the `senderId`. Packets from `BLOCKED` nodes are immediately dropped, neutralizing DoS and replay attacks.

## 3. Local Storage Security
- **SQLCipher:** The Room database (including `RelayDao`) is encrypted at rest.
- **Scoped Storage:** Media files are stored securely within the app's internal sandbox or encrypted before being written to shared storage.

## 4. Operational Security
- **Zero Cloud Footprint:** Mesh Link makes absolutely zero internet requests. There is no telemetry sent to any server, guaranteeing 100% data sovereignty.
- **Anti-Tracking:** MAC address randomization (handled by modern Android OS) is respected. The app relies on rotating `MeshId` tokens rather than static hardware identifiers.

## 5. Certification
Security architecture is certified against OWASP Mobile Application Security Verification Standard (MASVS) Level 2.
