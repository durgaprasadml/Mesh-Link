# Enterprise Risk Register

## Dynamic Risk Assessment
The `RiskManager` continuously computes operational risk, outputting a dynamic `RiskRegister`.

### Current Known Operational Risks
| ID | Risk Description | Level | Mitigation Strategy |
| :--- | :--- | :--- | :--- |
| **BAT-01** | Device battery drops below 15% during deployment. | `HIGH` | `ResourceOptimizationManager` delays graph computation to preserve routing. |
| **BKP-01** | Local DB Backup is older than 24h. | `MEDIUM` | Ensure `DatabaseContinuityManager` fires backup sweeps successfully during idle phases. |
| **NET-01** | Mesh Partition Detected. | `CRITICAL` | `PartitionManager` advises increased advertising to heal the bridge. |
