# Nearby Devices & Mesh Experience UI Audit (Phase 1)

## Overview
This document evaluates the existing UI/UX architecture of the `NearbyDevicesScreen` to establish a baseline for the Phase G6 redesign. 

## Current Experience Analysis

### 1. Discovery & Scanning Animation
**Current State:**
- The discovery UX uses a `RadarScanner` component with a repeating scale and alpha animation creating expanding rings.
- The scanner runs continuously without any clear feedback on how long it has been scanning or if it's currently active/paused.
- It occupies a large static chunk of the screen header.

**Issues:**
- **Clunky Scaling:** The current scale-and-fade animation feels rigid and unrefined compared to modern smooth fluid animations (e.g., Apple's AirDrop).
- **No Mesh Context:** It purely visualizes "scanning" but fails to depict the actual *Mesh Network Topology* (how devices connect to you and to each other).

### 2. Device Cards (List)
**Current State:**
- Devices are presented in a basic `LazyColumn` with a `DeviceCard` component.
- The card contains an avatar (initials), device name, and a simple RSSI threshold check (`rssi > -70`) to arbitrarily declare a device "Connected" or "Available".

**Issues:**
- **Lack of Details:** Fails to display critical mesh metadata (Mesh ID, exact Signal Quality, Transport mechanism like BLE vs Wi-Fi Direct).
- **Missing Trust Indicators:** No visual cues regarding encryption status or trust levels.
- **Bland Interactivity:** The cards do not have expansion states or rich tap ripples.

### 3. Screen Layout & Navigation
**Current State:**
- Standard `TopAppBar` with a back button.
- No Search functionality to find a specific peer in a crowded mesh.
- No filtering (e.g., sort by signal strength vs. sort by trust).

**Issues:**
- Unoptimized for large mesh networks (100+ nodes). Users will struggle to find specific peers without search/filter controls.
- Missing a cohesive "Empty State" or "Error State" if Bluetooth/WiFi are disabled or permissions are missing (currently delegates completely to a generic `PermissionHandler`).

## Proposed Resolution

To deliver an enterprise-grade Mesh visualization:
1. **Interactive Mesh Canvas:** Replace the static radar with an interactive, node-based Canvas visualization (using Compose Canvas) drawing lines between connected peers.
2. **Modern Device Cards:** Introduce expandable Material 3 cards showcasing connection type (BLE/WiFi), signal bars, and encryption lock badges.
3. **Refined TopAppBar:** Integrate a `SearchBar` and filtering dropdowns into the top navigation.
4. **Graceful Empty States:** Ensure the UI provides high-quality illustrations and actionable buttons when the network is empty or offline.
