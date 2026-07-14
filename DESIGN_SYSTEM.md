# Mesh Link Design System

## Overview

Mesh Link has transitioned to a unified **Material Design 3 (M3)** foundation, ensuring a consistent, accessible, and enterprise-grade user interface across all Android devices (Targeting Android 13–17).

This document serves as the single source of truth for UI design in Mesh Link, describing the core tokens (Colors, Spacing, Typography, and Shapes) and how they should be utilized in Jetpack Compose.

---

## 1. Tokens

The foundation of our UI is the `MeshTheme`. It encapsulates the color scheme, typography, shapes, spacing, and elevations. All screens must use these tokens; **hardcoded values are strictly prohibited.**

### Colors (`MeshColors`)

Our design system embraces dynamic coloring and supports Dark, Light, AMOLED Dark, and High Contrast themes. The semantic `MaterialTheme.colorScheme` should be used instead of hardcoded hex values.

*   `primary` / `onPrimary`: Main brand color (Neon Green) and text/icons placed on top.
*   `secondary` / `onSecondary`: Secondary branding (Blue accents).
*   `tertiary` / `onTertiary`: Success or supplementary accent colors.
*   `background` / `onBackground`: App background (Dark gray/black depending on theme) and default text.
*   `surface` / `onSurface`: Cards, dialogs, and elevated surfaces.
*   `surfaceVariant` / `onSurfaceVariant`: Alternative surfaces for subtle grouping (e.g., chat bubbles).
*   `error` / `onError`: Destructive actions and alerts (Red).

### Spacing (`MeshSpacing`)

Consistent spacing ensures a rhythm and hierarchy to the UI. Use `MeshTheme.spacing.*`.

*   `extraSmall`: 4.dp
*   `small`: 8.dp
*   `medium`: 12.dp
*   `mediumLarge`: 16.dp
*   `large`: 24.dp
*   `extraLarge`: 32.dp
*   `giant`: 48.dp

### Typography (`MeshTypography`)

Mesh Link uses a modern typography scale driven by the `Inter` font family.

*   `displayLarge` / `displayMedium` / `displaySmall`: Hero text, onboarding screens.
*   `headlineLarge` / `headlineMedium` / `headlineSmall`: Prominent titles and headers.
*   `titleLarge` / `titleMedium` / `titleSmall`: App bar titles, dialog titles, card titles.
*   `bodyLarge` / `bodyMedium` / `bodySmall`: Standard paragraph and descriptive text.
*   `labelLarge` / `labelMedium` / `labelSmall`: Button text, metadata, timestamps.

### Shapes (`MeshShapes`)

Rounded corners soften the UI and guide the user's eye. Use `MeshTheme.shapes.*`.

*   `extraSmall`: 4.dp
*   `small`: 8.dp
*   `medium`: 12.dp
*   `large`: 16.dp
*   `extraLarge`: 24.dp

---

## 2. Usage Examples in Jetpack Compose

Always access tokens via `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MeshTheme.*`.

### Theming a Screen

Wrap the top-level activity content in `MeshTheme` (already done in `MainActivity`).

### Applying Colors

```kotlin
// ✅ Correct
Text(
    text = "Hello, Mesh Link!",
    color = MaterialTheme.colorScheme.onBackground
)

Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
) { ... }

// ❌ Incorrect
Text(
    text = "Hello, Mesh Link!",
    color = Color(0xFFFFFFFF) // Hardcoded hex
)
```

### Applying Spacing

```kotlin
// ✅ Correct
Column(
    modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)
) {
    Text("Title")
    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
    Text("Subtitle")
}

// ❌ Incorrect
Column(
    modifier = Modifier.padding(16.dp) // Magic number
) { ... }
```

### Applying Typography

```kotlin
// ✅ Correct
Text(
    text = "Important Alert",
    style = MaterialTheme.typography.titleLarge
)

// ❌ Incorrect
Text(
    text = "Important Alert",
    fontSize = 22.sp, // Hardcoded size
    fontWeight = FontWeight.Bold // Hardcoded weight
)
```

---

## 3. Migration Guide

When migrating legacy screens or building new ones based on old designs, use the following mapping:

| Legacy Hardcoded Value | New M3 / MeshTheme Token |
| :--- | :--- |
| `Color(0xFF00FF88)` / `NeonGreen` | `MaterialTheme.colorScheme.primary` |
| `Color(0xFF121212)` / `DarkBackground` | `MaterialTheme.colorScheme.background` |
| `Color(0xFF1E1E1E)` / `SurfaceDark` | `MaterialTheme.colorScheme.surface` or `surfaceVariant` |
| `Color(0xFFFFFFFF)` / `TextPrimary` | `MaterialTheme.colorScheme.onBackground` or `onSurface` |
| `Color(0xFFDC2626)` / `Red` | `MaterialTheme.colorScheme.error` |
| `padding(8.dp)` | `padding(MeshTheme.spacing.small)` |
| `padding(16.dp)` | `padding(MeshTheme.spacing.mediumLarge)` |
| `padding(24.dp)` | `padding(MeshTheme.spacing.large)` |
| `RoundedCornerShape(16.dp)` | `MeshTheme.shapes.large` |

### General Rules

1.  **No `Color(...)` directly in UI code.** All colors must come from the active `MaterialTheme.colorScheme`.
2.  **No `.dp` directly in UI code** for margins or padding. Use `MeshTheme.spacing`. (Specific component dimensions like a button height of `54.dp` or an icon size of `24.dp` may remain as `.dp` if no semantic token applies, but padding/spacing must be semantic).
3.  **No `.sp` directly in UI code.** All text must use a `MaterialTheme.typography` style.

By adhering to this design system, Mesh Link ensures a future-proof, maintainable, and visually stunning user experience.
