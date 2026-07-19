# Compliance Matrix

## Framework Alignment

| Framework | Control / Domain | Mesh Link Technical Implementation | Status |
| :--- | :--- | :--- | :--- |
| **OWASP MASVS** | V2: Data Storage | SQLCipher PBKDF2/AES-256 implementation | Compliant |
| **OWASP MASVS** | V3: Cryptography | `SecurityGovernanceManager` enforces Hardware Keystore | Compliant |
| **ISO 27001 (Tech)** | A.8.2.3: Asset Handling | `PrivacyManager` enforces Data Classification on exports | Compliant |
| **ISO 27001 (Tech)** | A.12.4.1: Event Logging | `AuditManager` generates immutable hash-chained ledgers | Compliant |
| **Play Data Safety** | Data Minimization | `PrivacyManager` strips `mac_address` and `node_id` from logs | Compliant |
