# Home Shell & UI Audit (Phase 1)

## Overview
This document evaluates the existing UI/UX architecture of the Home screen and primary application shell to establish a baseline for the Phase G3 redesign.

## Current Experience Analysis

### 1. Home Screen Layout
**Current State:**
- Simple TopAppBar with basic title and three action icons (Wifi, Broadcast, Settings).
- A `ConnectionBanner` at the top indicating connection state.
- `LazyColumn` for displaying recent chats.
- Floating Action Button to initiate new chats (Nearby Devices).

**Issues:**
- **Navigation Model:** Currently lacks a unified navigation strategy (no bottom navigation or drawer), pushing everything into the app bar or separate screens.
- **Header Design:** The TopAppBar feels basic. Missing a prominent search bar and a personalized user profile avatar.
- **Dashboard Absence:** A messaging app focused on Mesh/P2P should surface critical system data (nearby peers, storage, pending transfers) as quick-glance dashboard cards rather than hiding them or omitting them completely.

### 2. Search Experience
**Current State:**
- Non-existent on the Home screen.

**Issues:**
- Users cannot search for specific chats, messages, or nearby devices directly from the main view.

### 3. Empty States
**Current State:**
- Displays a basic, unstyled centered text snippet: "No recent chats. Tap + to find nearby devices."

**Issues:**
- Lacks visual appeal (missing illustrations).
- Lacks a direct, actionable, primary CTA button inside the empty state itself.

### 4. Status Indicators
**Current State:**
- A simple `ConnectionBanner` (Connected/Searching/No Devices) using three basic colors (Green, Yellow, Red).

**Issues:**
- Doesn't articulate *how* the user is connected (BLE vs. WiFi Direct).
- Lacks information regarding encryption status or battery optimization.

### 5. Motion Design
**Current State:**
- Very basic default compose animations.

**Issues:**
- Missing Material Motion patterns (Fade Through, Shared Axis) for transitioning between the home screen and secondary flows.

## Proposed Resolution

To build an enterprise-level experience:
1. **App Shell**: Implement a `ModalNavigationDrawer` or `NavigationBar` (Bottom Nav) to house primary destinations (Home, Analytics, Settings, SOS).
2. **Search**: Implement `SearchBar` (Material 3) at the top of the Home screen for quick querying.
3. **Dashboard UI**: Introduce a horizontal scrolling or grid section at the top of the Home layout containing Material 3 Cards for (Nearby, Broadcasts, Pending, SOS).
4. **Recent Chats**: Move below the dashboard, acting as the primary scrollable content.
5. **Empty States**: Create a reusable `EmptyState` component with custom illustrations/icons, descriptions, and primary CTA buttons.
