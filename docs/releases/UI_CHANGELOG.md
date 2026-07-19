# UI Changelog

## V1.0 - Release Candidate 1

**Cumulative UI Changes (Phases G1 - G10)**

### Added
- Comprehensive Material Design 3 Design System (`MeshTheme`).
- Centralized `MeshAnimations` for unified easing curves and transition timings.
- Haptic Feedback integration via `HapticManager`.
- Semantic accessibility groupings for Screen Readers (`Role.Button`, merged nodes).

### Changed
- Replaced legacy Material 2 `Scaffold` and `TopAppBar` with M3 variants.
- Converted hardcoded color hex values to dynamic `MaterialTheme.colorScheme` references.
- Switched hardcoded `sp` text sizes to `MaterialTheme.typography` styles.
- Updated all floating action buttons to modern M3 pill/rounded-square shapes.
- Refactored `ChatsListScreen` and `NearbyDevicesScreen` to utilize adaptive Lazy Grids for tablet support.

### Fixed
- Predictive back visual artifacts in bottom sheets.
- Dark theme text contrast readability on elevated surfaces.
- Touch target sizes expanding below 48dp on icon buttons.
- State-thrashing recomposition loops in discovery views by caching keys.

### Removed
- Legacy static drop shadows (`elevation = 4.dp`); replaced with tonal surface elevation.
- Deprecated Material icons (e.g., standard `Send` replaced with `AutoMirrored.Filled.Send`).
