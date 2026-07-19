# CHANGELOG V3.0.0

## [3.0.0] - 2026-07-15
### Added
- Phase H2: Fault-Tolerant Crash Recovery (`RecoveryManager`)
- Phase H3: Android 14+ Lifecycle Compliance & WorkManager Orchestration
- Phase H4: Centralized Telemetry & Observability (`DiagnosticsManager`)
- Phase H5: Mesh Analytics & Real-Time Intelligence
- Phase H6: Disaster Recovery & SQLCipher Integrity checks (`IntegrityManager`)
- Phase H7: Enterprise Scalability (`ResourceOptimizationManager`)
- Phase H8: Enterprise Deployment (`EnterpriseConfigurationManager`)
- Phase H9: Compliance & Governance (`AuditManager`, `PrivacyManager`)

### Changed
- Refactored entire background routing sequence to respect Doze Mode without dropping packets.
- Transitioned generic preferences to Secure Enterprise Policies.

### Fixed
- Interrupted large file transfers now resume exactly from byte offset instead of restarting.
- Addressed memory leaks associated with infinite BLE scanning in dense node populations.
