# Accessibility (a11y) & Internationalization Report

## Accessibility Metrics
1. **TalkBack:** Content descriptions for all icon buttons (Play, Record, Send) are strictly defined in Compose `Modifier.semantics`.
2. **Dynamic Text:** `sp` (scale-independent pixels) used universally to adapt to OS font size sliders.
3. **Contrast:** Deep contrast ratios in both Light and Dark themes ensure WCAG 2.1 AA compliance natively via Material 3.

## Internationalization (i18n)
1. **UTF-8 & Emoji:** Standard JVM UTF-8 byte serialization for `MeshPacket` payloads ensures Emoji and multi-byte character (RTL, Arabic, Kanji, etc.) traversal across the mesh without corruption.
2. **Time Zones:** Internal DB stores UTC milliseconds (`lastMessageAt`). The UI layer seamlessly transforms to localized timezone formats.

## Status: **PASS**
