# Privacy Governance

## Data Classification
Mesh Link now enforces strict boundaries between operational telemetry and Personally Identifiable Information (PII).

### `PrivacyManager`
- Defines tiers: `PUBLIC`, `INTERNAL`, `CONFIDENTIAL`, `RESTRICTED`, `HIGHLY_RESTRICTED`.
- Example mappings:
  - Battery Level -> `INTERNAL`
  - MAC Address -> `HIGHLY_RESTRICTED`
- When an administrator requests a `FleetStatus` JSON/CSV export, the payload is piped through `sanitizeExportPayload()`, which automatically redacts any fields exceeding the active deployment's authorized clearance level.
