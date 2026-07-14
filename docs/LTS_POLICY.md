# Long-Term Support (LTS) Policy

## Overview
Mesh Link V2.0 is designated as an **LTS Release**. 
Because the application relies exclusively on on-device native Android APIs rather than cloud SaaS subscriptions, Long-Term Support focuses strictly on forward compatibility with future OS releases.

## Support Lifecycle
- **Active Maintenance:** 12 Months (Bug fixes, minor UX improvements).
- **Security Updates:** 36 Months (Cryptographic patching, API deprecation fixes for Android 16/17+).

## Vulnerability SLA
- **Critical (e.g., Cryptographic Bypass, SQLite Injection):** Hotfix published within 48 hours.
- **High (e.g., Denial of Service via broadcast storm):** Patch published within 7 days.
- **Medium/Low (e.g., UI glitch):** Bundled into the next standard release cadence.

## Deprecation
Support for Android 13 (API 33) is guaranteed until the V3 LTS release. No legacy API will be artificially deprecated unless strictly enforced by the Google Play Store (e.g., `targetSdkVersion` requirements).
