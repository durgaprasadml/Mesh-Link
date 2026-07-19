# Security Governance Guide

## Cryptographic Baseline
The `SecurityGovernanceManager` acts as an automated internal auditor. 

### Policies Enforced
- **Key Storage**: Ensures the Android Keystore is backed by a Trusted Execution Environment (TEE) or Secure Element (SE) before storing the SQLCipher master key.
- **Algorithm Strength**: Validates that SQLCipher utilizes AES-256 and PBKDF2 with a minimum of 256,000 iterations.
- If these baseline requirements are not met (e.g., the app is deployed on an ancient, compromised device), the `GovernanceScore` automatically drops, flagging the device as non-compliant.
