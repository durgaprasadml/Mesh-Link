# Mesh Link UI/UX Guidelines

## Overview
This document outlines the standard UI/UX patterns, design tokens, and components for the Mesh Link Android app. By following these guidelines, you will ensure a highly polished, consistent, and maintainable application.

## Design Tokens (`MeshTheme`)

We use a centralized design token system instead of hardcoded values.

### 1. Spacing (`MeshTheme.spacing`)
- **Do NOT use hardcoded DP values** like `16.dp` or `8.dp` for padding, margins, or sizes.
- **DO use** semantic tokens:
  - `MeshTheme.spacing.extraSmall` (4dp)
  - `MeshTheme.spacing.small` (8dp)
  - `MeshTheme.spacing.mediumSmall` (12dp)
  - `MeshTheme.spacing.medium` (16dp)
  - `MeshTheme.spacing.mediumLarge` (24dp)
  - `MeshTheme.spacing.large` (32dp)
  - `MeshTheme.spacing.extraLarge` (48dp)
  - `MeshTheme.spacing.huge` (64dp)
  - `MeshTheme.spacing.extraHuge` (72dp)
  - `MeshTheme.spacing.giant` (96dp)
  - `MeshTheme.spacing.extraGiant` (128dp)

### 2. Shapes (`MeshTheme.shapes`)
- **Do NOT use hardcoded shapes** like `RoundedCornerShape(12.dp)`.
- **DO use** semantic shapes:
  - `MeshTheme.shapes.extraSmall` (4dp)
  - `MeshTheme.shapes.small` (8dp)
  - `MeshTheme.shapes.medium` (12dp)
  - `MeshTheme.shapes.large` (16dp)
  - `MeshTheme.shapes.extraLarge` (24dp)
  - `CircleShape` for circular avatars.

### 3. Elevations (`MeshTheme.elevation`)
- Level 0 to Level 5.

## Core Components

Always use the standardized components defined in `com.meshlink.ui.components.*` rather than building ad-hoc elements.

### EmptyState
Used when a list or screen has no data.
```kotlin
EmptyState(
    icon = Icons.Outlined.ChatBubbleOutline,
    title = "No Messages",
    description = "You haven't started any conversations yet."
)
```

### LoadingOverlay
Used for blocking or partially blocking loading states over a screen.
```kotlin
LoadingOverlay(
    isLoading = uiState.isLoading,
    modifier = Modifier.fillMaxSize()
)
```

### AnimatedErrorDialog
A polished, animated dialog for showing errors.
```kotlin
AnimatedErrorDialog(
    visible = showError,
    title = "Connection Failed",
    message = "Unable to connect to peer.",
    onDismiss = { showError = false }
)
```

## Layouts & Navigation

- **Scaffold + Responsive Navigation:** Always wrap screens in the app-level `AppNavigation` which dynamically uses `NavigationBar` (bottom bar) for compact screens and `NavigationRail` (side bar) for expanded screens.
- **Window Size Classes:** UI layouts should scale based on `WindowSizeClass` injected from `MainActivity`.

## Animations & Haptics

- **Haptic Feedback:** Use `LocalHapticFeedback.current.performHapticFeedback(...)` on long presses, list item clicks, and critical actions (like expanding cards or sending messages).
- **Transitions:** Make use of `animateContentSize()` on cards that expand/collapse.
- **Visual Feedback:** Leverage `AnimatedVisibility` for entry and exit animations of stateful UI components.

## Best Practices
1. **Never use `androidx.compose.material.*`.** Only use `androidx.compose.material3.*`.
2. **Ensure touch targets** are at least 48dp (usually handled by Compose automatically on default components, but verify custom components).
3. **Use semantic content descriptions** (`Modifier.semantics`) for accessibility, especially on custom composite elements like `MessageBubble`.
4. **Optimize Lazy Lists** with `key = { ... }` and `contentType = { ... }` to minimize recompositions.
