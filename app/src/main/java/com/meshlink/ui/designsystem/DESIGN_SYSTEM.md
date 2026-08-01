# Mesh-Link 2026 Original Design System & Component Library Documentation

## 1. Brand Identity & Philosophy
- **Identity Name**: "Aether Grid / Tactile Mesh Protocol"
- **Target Experience**: Premium 2026 offline tactical emergency communication platform.
- **Core Principles**:
  1. *Tactile Telemetry*: Signal RSSI, Hop Count, and AES encryption status are displayed as first-class UI elements.
  2. *Visual Hierarchy Under Stress*: High-visibility emergency beacon states and distinct contrast for critical peer notifications.
  3. *Luminous Keylines*: 0.5dp translucent keyline borders separating elevated cards from deep dark obsidian backgrounds.

---

## 2. Design Tokens

### Color Tokens
- **Primary**: Luminous Signal Mint (`#00F59B` Dark, `#00A86B` Light)
- **Secondary**: Quantum Cyan (`#00E5FF` Dark, `#00899B` Light)
- **Emergency**: High-Vis SOS Crimson (`#FF2A4B`)
- **Warning / Scanning**: Tactical Amber (`#FFB300`)
- **Canvas / Background**: Deep Obsidian (`#070B12` Dark, `#000000` AMOLED, `#F8FAFC` Light)
- **Surface Modes**: Supports Light, Dark, and AMOLED Pure Pitch Black.

### Typography Scale
- **Display**: 36sp / 28sp Bold
- **Headline**: 24sp SemiBold
- **Title**: 20sp SemiBold
- **Subtitle**: 16sp Medium
- **Body**: 14sp Normal / Medium
- **Caption / Label**: 12sp
- **Monospace Telemetry Metrics**: Monospaced 11-14sp tracking for RSSI (`-68 dBm`), Hop Count (`1 HOP`), and AES Key status (`128-BIT`).

### 4dp Spacing Grid
- `XXS`: 2dp | `XS`: 4dp | `SM`: 8dp | `MD`: 12dp | `LG`: 16dp | `XL`: 24dp | `XXL`: 32dp
- `Section`: 40dp | `Hero`: 48dp | `Screen Insets`: 16dp / 24dp.

### Corner Shapes
- `Tiny`: 4dp | `Small`: 8dp | `Medium`: 12dp | `Large`: 16dp | `XL`: 24dp | `Pill`: 50% Circular.

### 8-Tier Elevation System
- `Flat` (0dp), `Raised` (2dp), `Floating` (4dp), `Overlay` (8dp), `Glass` (0dp + keyline glow border), `Hero` (6dp), `Emergency` (12dp), `Navigation` (4dp).

---

## 3. Motion System
- **Screen Transitions**: Decelerated 250ms Enter, Accelerated 200ms Exit.
- **Radar & Signal Motion**: 2000ms linear radar scan rotation, 1200ms signal pulse, 800ms emergency alert beacon pulse.
- **Spring Haptics**: High-stiffness tactile button compression (0.97f scale).

---

## 4. Interaction System & States
Supports 19 distinct component interaction states:
`Pressed`, `Focused`, `Hovered`, `Selected`, `Active`, `Inactive`, `Disabled`, `Loading`, `Success`, `Failure`, `Searching`, `Connected`, `Disconnected`, `Emergency`, `Broadcasting`, `Receiving`, `Sending`, `Encrypted`, `Offline`.

---

## 5. Responsive Token Framework
Supports 5 device tiers:
- `Small Phone` (< 360dp)
- `Large Phone` (360dp - 599dp)
- `Foldable` (600dp - 839dp)
- `Tablet` (840dp+)
- `Desktop Preview` (1200dp+)
- Automatically switches between `Bottom Navigation Bar` and `Navigation Rail` based on viewport width (>= 720dp or Landscape).

---

## 6. Accessibility Rules
- **WCAG AA Compliance**: Dynamic contrast calculation enforcing minimum 4.5:1 ratio for normal text and 3.0:1 for large text.
- **Touch Target Standard**: Mandatory minimum 48dp x 48dp target bounding box for interactive elements (`Modifier.meshTouchTarget()`).

---

## 7. Component Library Summary (40+ Components)
- **Buttons**: `MeshButton`, `EmergencyButton`, `MeshIconButton`, `MeshSegmentedControl`, `MeshFAB`, `FloatingDock`, `FloatingPanel`.
- **Cards**: `MeshCard`, `HeroCard`, `MetricCard`, `QuickActionTile`, `MediaCard`, `VoiceCard`, `FileCard`, `ImageCard`, `MeshGlassCard`.
- **Navigation**: `MeshNavigationBar`, `MeshNavigationItem`, `MeshNavigationRail`, `MeshTopBar`, `MeshTabBar`.
- **Inputs**: `MeshInputField`, `MeshSearchBar`, `MeshSearchField`.
- **Telemetry**: `SignalMeter`, `RSSIMeter`, `HopBadge`, `MeshStatusBadge`, `MeshStatusPill`.
- **Feedback & Loaders**: `ProgressRing`, `ProgressBar`, `SkeletonLoader`, `LoadingIndicator`, `EmptyState`, `NotificationBanner`.
- **Badges & Avatars**: `MeshBadge`, `StatusBadge`, `MeshAvatar`, `AvatarGroup`, `MeshChip`.
- **List & Chat**: `MeshListItem`, `ChatBubbleComponent`, `TimelineComponent`, `MeshDivider`, `SectionHeader`.
- **Dialogs & Sheets**: `MeshDialog`, `MeshBottomSheet`.
