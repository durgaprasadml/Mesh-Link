# Maintenance Guide

This document outlines long-term maintenance strategies for Mesh Link V1.0.

## Codebase Principles
1. **Clean Architecture Strictness**: Never allow the Domain layer to import `android.*` or `androidx.*` (except standard annotations like `@Inject`).
2. **Dependency Updates**: Run `./gradlew dependencyUpdates` monthly. Prioritize updating `net.zetetic:sqlcipher-android` and Google `Tink` / `androidx.security`.
3. **Database Migrations**: When updating `MessageEntity` or `ChatEntity`, you MUST write a Room Migration and increment the schema version. The `sqlcipher` keys must be passed to the migration block.

## Future Enhancements (V1.1+)
- Consider integrating Android's `Awareness API` for smarter battery management based on user physical state (e.g. driving vs sitting).
- Migrate legacy `BluetoothAdapter` discovery calls to the newer Android 15 `CompanionDeviceManager` APIs if they become mandated.
