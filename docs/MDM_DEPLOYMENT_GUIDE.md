# MDM (Mobile Device Management) Deployment Guide

## Overview
Mesh Link is architected for frictionless enterprise provisioning via standard EMM/MDM platforms like Microsoft Intune, VMware Workspace ONE, and Samsung Knox.

## AppConfig / Managed Configurations
Mesh Link supports standard Android Enterprise App Restrictions (managed configurations) via the `restrictions.xml` schema. IT Administrators can push policies to:
- Disable unencrypted ad-hoc routing.
- Force `Require Wi-Fi Direct Only` (disabling BLE).
- Force a static Company Mesh Profile.

## Deployment Strategy
1. **Silent Install:** The APK is fully compliant with zero-touch Android Enterprise enrollment.
2. **Work Profile Separation:** The application cleanly respects the Android Work Profile sandbox. Mesh packets and the local Room database (`meshlink_db`) are securely isolated within the work profile's encrypted partition and cannot cross-pollinate with the host profile's data.

**Status:** Certified for Android Enterprise Deployment.
