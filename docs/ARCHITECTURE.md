# Mesh Link Architecture

## Core Design Principles
Mesh Link adheres strictly to Clean Architecture and SOLID principles, ensuring high testability, separation of concerns, and modularity.

1. **Domain Layer**: Contains all enterprise business logic (`UseCases`, `Models`). Completely framework agnostic.
2. **Data Layer**: Implements `Repositories`. Handles mapping between Domain models and Database/Network entities. Includes Room DAOs and BLE Managers.
3. **UI Layer**: Built entirely in Jetpack Compose, driven by MVVM and Uni-directional Data Flow (UDF). State is exposed via `StateFlow`.

## Dependency Injection
**Hilt (Dagger)** manages all object lifecycles and scopes:
- `@Singleton`: Database instances, Core Managers (Crypto, Preferences).
- `@ViewModelScoped`: Repositories and UseCases needed for active screens.

## Coroutines & Flow
- All background work (I/O, BLE scanning, DB transactions) operates on injected `CoroutineDispatchers` (defaults to `Dispatchers.IO`).
- Real-time updates (Message streams, nearby devices) are propagated natively via `Flow` and `StateFlow` to the Compose UI.
