# Deployment Lifecycle

## Managing Upgrades & Rollbacks
Enterprise deployments require strict control over database schema migrations.

### `DeploymentLifecycleManager`
- Defines environments: `PILOT`, `PRODUCTION`, `LTS`, `ROLLBACK`.
- Explicitly blocks unsafe Room Database migrations. If an MDM pushes an older APK to rollback a buggy update, the lifecycle manager halts the Room downgrade to prevent total data loss, instead deferring to the `RecoveryManager` to restore a previous backup.
