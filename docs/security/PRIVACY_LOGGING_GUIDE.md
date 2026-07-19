# Privacy Logging Guide

## Zero-Trust Logging
Enterprise operations require strict compliance (e.g., GDPR, HIPAA). Telemetry must not leak Personally Identifiable Information (PII) or security tokens.

### `PrivacyLogInterceptor`
- Acts as middleware inside `MeshLogger.logEvent()`.
- **Regex Redaction:** Actively scans for MAC address patterns (`[REDACTED_MAC]`) and IPv4 configurations (`[REDACTED_IP]`).
- **Metadata Scrubbing:** Keys containing terms like `token`, `secret`, `password`, or `key` have their values masked automatically (`[REDACTED_SENSITIVE]`).
- **Cryptographic Safety:** Prevents cryptographic handshake tokens from accidentally dumping into the crash telemetry buffer.
