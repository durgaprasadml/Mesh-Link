# Telemetry & Logging Governance Policy

## The Zero-Telemetry Guarantee
Mesh Link fundamentally rejects continuous cloud telemetry. 
1. **No PII:** The application collects absolutely ZERO Personally Identifiable Information. 
2. **No Mixpanel/Firebase:** Behavioral analytics SDKs are strictly banned from the codebase.
3. **No Key Extraction:** Cryptographic session keys (ECDH) and static identities (ECDSA) are generated on the secure enclave/keystore and never transmitted.

## Local Log Retention
- **Audit Logs:** Application state transitions (e.g., "Bluetooth Started", "Database Migrated") are logged locally.
- **Log Purging:** The local audit table (`AuditLogEntity`) auto-prunes logs older than 7 days to preserve device storage and enforce data minimization.
