# Contribution Guide

We welcome contributions to Mesh Link! To maintain high code quality and system stability, please follow this workflow.

## Branch Strategy

*   `main`: Stable branch. Always deployable.
*   `develop`: Integration branch for new features.
*   `feature/my-feature`: Feature branches (branch off `develop`).
*   `bugfix/issue-123`: Bugfix branches (branch off `develop` or `main`).

## Pull Request Process

1.  **Fork and Branch:** Create your feature branch.
2.  **Code:** Write your code, ensuring you follow the coding standards.
3.  **Test:** Add unit and integration tests for your changes. Ensure the Simulator passes all scenarios.
4.  **Document:** Update the markdown documentation in `docs/` if your changes affect architecture, API, or configuration.
5.  **Submit PR:** Open a Pull Request against the `develop` branch.

## Coding Standards

*   Use `ktlint`. Run `./gradlew ktlintCheck` before committing.
*   Write KDoc for all public APIs.
*   Keep functions small and focused.
*   Handle all potential exceptions, especially in the Transport and Security layers. Do not allow crashes to propagate to the host application.

## Testing Expectations

*   **Coverage:** New code must have at least 80% test coverage.
*   **Stress Tests:** If you modify the Routing or Transport layers, you must run the reliability suite locally to ensure no performance regressions.
*   **Security:** Cryptographic changes require explicit review from a core maintainer and must pass the Security Regression suite.

## Review Checklist

Reviewers will check for:
- [ ] Code compiles and tests pass.
- [ ] `ktlint` passes.
- [ ] Concurrency is handled correctly (no deadlocks, proper Coroutine scoping).
- [ ] Memory leaks (especially in packet queues and connection handling).
- [ ] Documentation is updated.
- [ ] No telemetry or cloud dependencies were added.
