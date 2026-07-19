# Chat UI & Conversation Experience Audit (Phase 1)

## Overview
This document evaluates the existing UI/UX architecture of the `ChatsListScreen` and `ChatDetailScreen` to establish a baseline for the Phase G4 redesign.

## 1. Chat List Screen
**Current State:**
- The list uses a blocky `Card` based layout (`ChatCard`) for each conversation, relying on `surfaceVariant` backgrounds which can look heavy in large lists.
- Avatar is a simple circular `Box` with an `Icons.Default.Person`.
- The timestamp and unread badge are squished to the far right.

**Issues:**
- **Visual Weight:** Wrapping every chat in a card creates too much visual noise. A flat, edge-to-edge layout with a standard Material 3 list item is preferred.
- **Search:** There is no search functionality directly built into the Chat List screen (the Home screen has one now, but if navigating directly to `ChatsListScreen`, it's missing).
- **Avatars:** Lacks dynamically colored, large profile avatars (currently defaulting to a generic person icon).
- **Swiping:** Missing swipe-to-reveal actions (like Archive or Delete) which are standard in modern messaging apps.

## 2. Conversation Screen (Chat Detail)
**Current State:**
- The top app bar shows the user's name and connection state in text format.
- `MessageBubble` handles text, image, voice, location, and SOS messages all within a single massive composable.
- Input composer is a standard `OutlinedTextField` docked to the bottom.

**Issues:**
- **Message Bubbles:** Bubbles use aggressive corner rounding (`16.dp` on 3 corners, `4.dp` on the tail) but feel generic. They lack distinct depth or shadow for light mode, and dark mode contrast is purely solid colors.
- **Composer:** The input bar uses an `OutlinedTextField`. It doesn't dynamically grow well with large text inputs and the attach icon is separated awkwardly.
- **Date Separators:** Completely absent. The chat history runs together without any sticky day/date dividers.
- **Status Indicators:** The delivery ticks (Pending, Sent, Delivered, Seen) are functional but very tiny (`14.dp`) and easily missed.
- **Monolithic File:** The `ChatDetailScreen.kt` is over 700 lines long, making it difficult to maintain. Bubbles and Composer should be extracted into their own components.

## Proposed Resolution

To deliver a world-class messaging experience:
1. **Chat List**: Transition to a flat layout (no cards) using `ListItem` structures. Implement custom Swipe-to-Reveal behaviors using `SwipeToDismissBox`.
2. **Conversation Header**: Introduce a rich Material 3 `TopAppBar` with a rounded avatar and a subtle connection status indicator embedded directly in the header.
3. **Message Bubbles**: Extract `MessageBubble` into its own component file. Implement softer padding, distinct delivery ticks, and proper timestamp alignment inside the bubble.
4. **Date Separators**: Introduce a floating, sticky date header in the `LazyColumn` for grouping messages by day.
5. **Modern Composer**: Rebuild the input area into a pill-shaped, auto-expanding text field that seamlessly swaps between the 'Send' and 'Mic' icons based on input state.
