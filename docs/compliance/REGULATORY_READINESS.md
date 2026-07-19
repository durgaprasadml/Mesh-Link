# Regulatory Readiness

## Technical Mappings
While Mesh Link is an offline tool, its technical architecture maps to several major industry frameworks.

### OWASP Mobile Application Security Verification Standard (MASVS)
- **MASVS-STORAGE-1**: Database is fully encrypted using SQLCipher.
- **MASVS-CRYPTO-1**: Keystore utilizes hardware-backing (enforced via `SecurityGovernanceManager`).

### Android Enterprise
- Fully supports Managed Configurations (`RestrictionsManager`) for zero-touch mass deployment.
- Complies with Foreground Service rules (Android 14+).

*Note: This document provides technical evidence. Formal certification (e.g. ISO 27001) requires external organizational audit.*
