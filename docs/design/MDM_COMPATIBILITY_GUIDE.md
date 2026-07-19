# MDM Compatibility Guide

## Managed Configurations

Mesh Link relies on Android Enterprise `RestrictionsManager` to support zero-touch deployments across major MDMs.

### Supported Platforms
- Microsoft Intune
- VMware Workspace ONE
- IBM MaaS360
- SOTI MobiControl
- Samsung Knox Manage

### Exposing Capabilities
To expose configurations to your MDM, Mesh Link utilizes a `restrictions.xml` file.

**Supported MDM Keys:**
- `disable_media_transfers` (Boolean): Drops large payloads to preserve network routing capacity.
- `force_emergency_mode` (Boolean): Locks discovery cycles to 1000ms for active disaster environments.
- `enforce_encrypted_backups` (Boolean): Mandates that local recovery DBs are secured via SHA-256 and KeyStore (Default: True).
- `disable_analytics` (Boolean): Disables the `MeshAnalyticsManager` from aggregating peer diagnostics for privacy compliance.
