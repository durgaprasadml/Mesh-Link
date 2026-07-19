# Mesh Link V3.0.0 Release Notes

**Date:** July 2026
**Target:** Enterprise, Disaster Response, Defense

Mesh Link V3.0 is a milestone release. It elevates the offline MANET routing engine into a fully observable, highly fault-tolerant, and enterprise-manageable platform.

## Key Features
- **Zero-Touch Provisioning**: MDM integration via `RestrictionsManager`.
- **Fault-Tolerance**: Complete process-death recovery and SQLite database corruption healing.
- **Immutable Auditing**: SHA-256 hash-chained ledgers for offline compliance verifications.
- **Mass Scalability**: Optimized background execution handling 1,000+ node simulations without WakeLock drain.

## Upgrade Guide
This version requires a Room Database Schema migration. `DeploymentLifecycleManager` will automatically manage the upgrade. Note that MDM Rollbacks to V2.x are explicitly blocked by the new Lifecycle constraints to prevent data loss.

## Recommended Environments
- Minimum API 33 (Android 13)
- Target API 35 (Android 15)
- Support for API 37 (Android 17) tested and verified.
