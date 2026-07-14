# Enterprise Deployment Guide

## Overview
For military, industrial, or government deployments, Mesh Link is often distributed outside of the Google Play Store to maintain strict operational security.

## 1. MDM (Mobile Device Management) Rollout
- **Artifact:** Distribute the universal `app-enterprise-release.apk`.
- **Managed Configurations:** Mesh Link supports Android Enterprise Managed Configurations.
  - `force_emergency_mode`: Boolean (forces radios to ignore battery limits).
  - `disable_video`: Boolean (disables high-bandwidth video routing).
  - `trust_anchor_keys`: String (pre-loads public keys of known commanders to prevent rogue node spoofing).

## 2. Air-Gapped Sideloading
In environments with zero internet access, Mesh Link can be distributed node-to-node.
- Use the built-in "Share App" feature to send the base APK via Wi-Fi Direct or Bluetooth to an onboarding device.

## 3. Version Migration & Rollbacks
- **Database Migrations:** Room database migrations are strictly tested. Downgrading is NOT supported due to SQLCipher key derivation changes in v2.0. If a downgrade is forced via MDM, the local database will be wiped.
- **Rollout Strategy:** Deploy to 10% of the fleet. Monitor `MeshAnalytics` logs (exported via USB) for unusual congestion spikes or battery drain before rolling out to 100%.
