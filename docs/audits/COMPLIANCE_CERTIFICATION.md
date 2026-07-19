# Compliance Certification

This document outlines Mesh Link's adherence to Android Enterprise operational standards.

## Android Enterprise Baseline
- [x] **Foreground Services**: Correctly declared types (`connectedDevice`, `location`) for Android 14+ compatibility.
- [x] **Permissions**: Graceful degradation when `BLUETOOTH_SCAN` or `ACCESS_BACKGROUND_LOCATION` are denied or revoked by MDM.
- [x] **Managed Configurations**: Support for zero-touch XML key-value pairs via `RestrictionsManager`.
- [x] **Battery Optimization**: Validates Doze Mode exemptions to prevent the OS from killing the background mesh routing service after 2 hours.
