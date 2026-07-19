# Enterprise Configuration

## Android Enterprise Integration
Mesh Link reads its master configuration directly from the Android `RestrictionsManager`.

### `EnterpriseConfigurationManager`
- **Zero-Touch Provisioning**: When an MDM pushes a payload to a Work Profile, Mesh Link intercepts it automatically.
- **Strict Enforcement**: Local users cannot override settings defined in the XML payload (e.g., they cannot re-enable analytics if the organization has locked it down for privacy reasons).
