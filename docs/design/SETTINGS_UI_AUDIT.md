# Settings, Profile & Security Experience UI Audit (Phase 1)

## Overview
This document evaluates the existing UI/UX architecture of the `SettingsScreen` to establish a baseline for the Phase G7 redesign.

## Current Experience Analysis

### 1. Settings Home Layout
**Current State:**
- The entire settings interface is crammed into a single `LazyColumn` within `SettingsScreen.kt`.
- Uses a basic layout with inline text fields and raw segmented buttons for Mesh Modes.
- Lacks a unified "Search Settings" capability.
- Categories have no icons, making the UI feel like a simple list rather than a premium management dashboard.

**Issues:**
- **Poor Information Architecture:** Mixing profile editing, encryption toggles, and data clearance on one screen is an anti-pattern for enterprise apps.
- **Navigation:** There is no sub-navigation to dedicated screens for Security, Network, Storage, or Diagnostics.

### 2. User Profile Management
**Current State:**
- Profile is handled by an inline `OutlinedTextField` at the top of the settings page.
- Avatar is just a simple text initial.
- Missing critical mesh identity information (Mesh ID, Trust Level, Device Name).

**Issues:**
- Does not feel like a distinct "Identity". In secure apps (like Signal), the Profile is a dedicated, highly polished screen that establishes trust.

### 3. Missing Categories
**Current State:**
- The app completely lacks dedicated screens for:
  - **Security Center** (No key rotation, session info, or trusted devices view).
  - **Network Settings** (No BLE/WiFi priority controls).
  - **Diagnostics** (No health metrics for the mesh routing).
  - **Developer Options** (No packet viewer or mesh simulator controls).

## Proposed Resolution

To deliver an enterprise-grade management experience:
1. **Refactor Settings Home:** Convert `SettingsScreen.kt` into a true "Settings Dashboard". It will feature a prominent Search bar, a large Profile summary card at the top, and nicely categorized icon-lists (Security, Network, Appearance, Storage, Diagnostics, Developer) that navigate to sub-screens.
2. **Create Dedicated Sub-Screens:** 
   - `ProfileScreen.kt`: A detailed identity page with Mesh ID and Trust levels.
   - `SecurityCenterScreen.kt`: Visually displaying AES-256-GCM encryption status and key rotation.
   - `NetworkSettingsScreen.kt`: Material switches for BLE/WiFi Direct toggles.
   - `StorageSettingsScreen.kt`: Usage charts and cache clearance.
   - `DiagnosticsScreen.kt`: Realtime visual health scores for the mesh.
3. **Motion Design:** Utilize Material 3's shared axis transitions (if applicable) or standard fade/slide animations when navigating between these deep settings tiers.
