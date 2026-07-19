# Production Monitoring Guide

## Philosophy
To maintain absolute privacy and offline capability, Mesh Link **DOES NOT** integrate standard cloud telemetry platforms (like Firebase Crashlytics, Datadog, or Sentry) by default. 

## Diagnostics & Telemetry
1. **Local Audit Logs:** System events, routing table flushes, and catastrophic errors are logged locally into the encrypted `AuditLogEntity` Room table.
2. **Exportable Diagnostics:** Users can export an obfuscated ZIP of their application state (`Export Diagnostics` in Settings) to send to support. 
3. **Enterprise Monitoring:** MDM deployments can inject optional remote-logging configurations via Managed App Config, routing syslog traffic securely to a corporate SIEM.

**Privacy Guarantee:** No PII (names, messages, MAC addresses) is ever included in local debug traces.
