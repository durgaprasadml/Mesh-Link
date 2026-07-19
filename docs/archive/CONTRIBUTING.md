# Contributing to Mesh Link

We welcome contributions! To ensure code quality and stability, please adhere to the following guidelines:

## Pull Request Process
1. Ensure all Unit Tests pass (`./gradlew test`).
2. Ensure strict linting passes (`./gradlew lintRelease`).
3. If introducing a new UseCase, it MUST be covered by a Unit Test.
4. Update `CHANGELOG.md` with a summary of your changes.

## Code Style
- We strictly follow the standard Kotlin style guide.
- UI elements must be built in Jetpack Compose using Material 3.
- Avoid passing raw `ViewModel` instances to Composables. Pass state and hoisted callbacks.

## Issue Tracking
Please check the issue tracker before submitting a PR to ensure the feature isn't already being worked on.
