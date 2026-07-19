# Audit Framework

## Tamper-Evident Ledger
Enterprise deployments require proof that administrative actions (like disabling analytics) were requested legitimately and not bypassed by a malicious actor.

### `AuditManager`
- Every critical event is logged as an `AuditRecord`.
- A cryptographic hash chain (SHA-256) links each record to the `previousHash`.
- If an attacker manually modifies a local file, `verifyLedgerIntegrity()` will fail because the current hash will no longer align with the subsequently recorded `previousHash`.
- *Note on Scale*: To prevent infinite storage scaling on edge devices, the ledger retains the most recent 10,000 events. Older events are dropped while preserving the cryptographic boundary of the active chain.
