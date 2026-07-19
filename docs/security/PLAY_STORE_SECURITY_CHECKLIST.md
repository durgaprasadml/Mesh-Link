# Play Store Security Checklist

This document ensures compliance with Google Play's strict policy and data safety requirements.

- [x] **Data Safety Declaration**: Correctly maps that data is fully encrypted at rest and in transit.
- [x] **Zero PII Leakage**: No user names, locations, or files are sent off-device (excluding peer-to-peer transmission).
- [x] **Foreground Service Justification**: `connectedDevice` and `dataSync` are strictly justified by the core functionality of maintaining an offline mesh network.
- [x] **Backup Rules Configured**: `android:allowBackup="false"` or strict exclusion rules for the `EncryptedSharedPreferences` and KeyStore aliases, preventing cross-device key corruption during cloud backup restoration.
- [x] **Exported Components Secured**: All BroadcastReceivers and Activities are strictly internal or require strict signature permissions.
