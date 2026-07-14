# RC2 Compose Recomposition Report

## Overview
This report outlines UI-layer rendering optimizations using Jetpack Compose.

## Optimizations
1. **Stability Markers:**
   - Added `@androidx.compose.runtime.Immutable` to UI state classes (`ChatDetailUiState`, `ChatsListUiState`) to prevent unnecessary recompositions.
2. **List Keys:**
   - Confirmed explicit `key` parameters in all `LazyColumn` iterations to optimize diffing during scroll.

## Expected Metrics
- **Frame Rate:** Consistent 60fps/120fps scrolling.
- **Skipped Recompositions:** Increased significantly in Android Studio Layout Inspector.
