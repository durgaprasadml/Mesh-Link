# Privacy Audit

Mesh Link is an offline-first application designed for absolute privacy. It operates entirely without relying on centralized servers or cloud infrastructure.

## Data Collection & Telemetry
- **Analytics**: Firebase Analytics is disabled in the `release` build variant via manifest rules.
- **Crash Reporting**: Firebase Crashlytics is utilized strictly for stability monitoring. All logs are heavily sanitized. No user identifiers (Mesh IDs, names), location data, or message contents are ever logged.
- **IP / MAC Addresses**: Mesh Link does not log internal IP addresses or Wi-Fi Direct MAC addresses to persistence. They are held in transient memory only for active sockets.

## Data Residency
All data remains strictly on-device, secured inside the encrypted SQLCipher database. Deleting the app instantly obliterates all cryptographic material and data, effectively functioning as a secure wipe.
