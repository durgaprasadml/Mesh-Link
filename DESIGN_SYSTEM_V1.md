# Mesh Link Design System V1

## Core Tokens
- **Typography:** `Inter` (Sans-Serif), scaling from `labelSmall` (11sp) to `displayLarge` (57sp).
- **Colors:** Fully integrated `MaterialTheme.colorScheme` supporting Dynamic Color extraction and strict Light/Dark contrast guidelines.
- **Spacing:** `MeshSpacing` primitives (`small` = 4.dp, `medium` = 12.dp, `large` = 20.dp, etc.).
- **Shapes:** `MeshShapes` primitives (`extraSmall` = 4.dp, `small` = 8.dp, `medium` = 12.dp, `large` = 16.dp, etc.).
- **Motion:** `MeshAnimations` utilizing Emphasized, Standard, and Decelerate Android standard easing specs.

## Components
All interactive and static UI surfaces (Cards, Dialogs, Snackbars, FABs, NavBars) derive purely from these primitives.

## Freeze Status
Version 1 is officially frozen as of 2026-07-14.
