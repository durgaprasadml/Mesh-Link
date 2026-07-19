# OWASP Mobile Application Security Verification Standard (MASVS)

Mesh Link was audited against the MASVS v2.0 criteria.

| MASVS Category | Score | Verification |
| :--- | :--- | :--- |
| **V1: Architecture, Design and Threat Modeling** | PASS | Zero-trust model implemented. Keys generated locally, never exported. |
| **V2: Data Storage and Privacy** | PASS | SQLCipher (AES-256) protects all data at rest. SharedPreferences are encrypted via `EncryptedSharedPreferences`. |
| **V3: Cryptography** | PASS | Strong primitives used (ECDH, AES-GCM). AndroidKeyStore protects master keys. |
| **V4: Authentication and Session Management** | PASS | Devices authenticated via ECDSA signatures. Sessions expire and rotate automatically. |
| **V5: Network Communication** | PASS | All internal network layers (BLE/Wi-Fi) are wrapped in AES-GCM. MITM prevented by ECDH. |
| **V6: Platform Interaction** | PASS | IPC boundaries secured. No exported activities/services lacking signature protection. |
| **V7: Code Quality and Build Setting** | PASS | ProGuard/R8 enabled. `abortOnError = true` enforced for linting. |
| **V8: Resilience** | PASS | App protects against basic reverse engineering via obfuscation. |
