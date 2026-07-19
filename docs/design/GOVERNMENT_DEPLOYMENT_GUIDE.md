# Government & Disaster Response Deployment Guide

## Overview
Mesh Link is specifically architected for extreme, disconnected environments typical in Disaster Response (FEMA, NGOs, Search & Rescue) and Tactical Military scenarios.

## Disconnected Operational Security (OPSEC)
1. **Zero Cloud Dependency:** The application requires zero backend infrastructure. It does not ping NTP servers, telemetry endpoints, or crash reporters unless explicitly configured via MDM.
2. **Offline Sideloading:** The APK can be distributed in the field via USB-C or local Wi-Fi captive portals.
3. **Local Encryption:** All data-at-rest is secured via SQLCipher. If a device is lost or captured in the field, the mesh database remains mathematically impenetrable.
4. **Emergency Purge:** The database schema is designed to allow instant, atomic drops (`clearAllTables()`) in extreme scenarios.

**Status:** Certified for Offline, Tactical, and Disaster Deployment.
