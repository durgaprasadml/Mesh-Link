# Mesh Link Testing Guide

This document outlines the testing architecture and conventions for Mesh Link, establishing a foundation for a robust, enterprise-grade automated testing suite.

## Testing Architecture

The project follows a layered testing approach:
1. **Unit Tests (Phase 1 - Current)**: Core business logic (Domain layer, Repositories, ViewModels). Fast, hermetic, and deterministic.
2. **Integration Tests (Upcoming)**: Database (Room) and interactions between multiple components.
3. **Instrumentation Tests (Upcoming)**: Real device testing (Compose UI, BLE interactions).

## Unit Testing Conventions

### Coroutines & Flows
- Use `runTest` from `kotlinx-coroutines-test` for suspending functions and flows.
- Use `Turbine` (`app.cash.turbine:turbine`) to test flows cleanly (`flow.test { ... }`).
- A `MainDispatcherRule` is provided and must be applied in any test invoking ViewModels to swap the Main dispatcher with a `TestDispatcher`.
- When testing classes that launch background coroutines (e.g. `BleRepositoryImpl`, `TrustManager`), inject scopes or provide a way to cancel them (`cancelScope()`) in `@After` blocks to avoid `UncaughtExceptionsBeforeTest` errors caused by leaking coroutines.

### Mocking
- Use `MockK` for all mocking. (`Mockito` has been deprecated and removed from this project).
- Mock only external dependencies (e.g., DAOs, managers, network layers).
- Do NOT mock data classes or domain entities (like `MessageEntity` or `UserEntity`). Instantiate real instances for tests.
- When creating fake objects or complex data models, use `TestDataFactory` in the `sharedTest` directory.

### Dependency Injection (Hilt)
- Unit tests typically do not need Hilt since they manually inject mocked dependencies into constructors.
- Keep Hilt annotations and rules for Integration/UI tests. Hilt testing dependencies are properly configured in `androidTestImplementation`.

### Shared Test Directory
- Shared utilities, such as `TestDataFactory` and `MainDispatcherRule`, are kept in `app/src/sharedTest/java/...`.
- This ensures these utilities are available to both local Unit Tests and Android Instrumentation Tests.

## Running Tests

To run the unit tests locally from the command line:

```bash
# Run all unit tests for the InternalDebug variant
./gradlew :app:testInternalDebugUnitTest
```

Reports are generated at:
`app/build/reports/tests/testInternalDebugUnitTest/index.html`

## Current Test Coverage
- **Domain**: `SendMessageUseCaseTest`
- **Data/Repository**: `MessagingRepositoryImplTest`, `UserRepositoryImplTest`, `BleRepositoryImplTest`, `TrustManagerTest`
- **Presentation**: `HomeViewModelTest`, `ChatsListViewModelTest`, `ChatDetailViewModelTest`, `SettingsViewModelTest`, `AuthViewModelTest`, `NearbyViewModelTest`
