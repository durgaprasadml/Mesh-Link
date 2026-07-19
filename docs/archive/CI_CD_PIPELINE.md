# CI/CD Pipeline Architecture

## Overview
Mesh Link utilizes a modern, zero-touch CI/CD pipeline philosophy adaptable to GitHub Actions, GitLab CI, or Jenkins.

## Pipeline Stages
1. **Linting & Formatting:**
   - Execute `./gradlew ktlintCheck` and `./gradlew detekt` to strictly enforce Kotlin idioms.
2. **Testing Validation:**
   - Unit Tests: `./gradlew testDebugUnitTest` ensures routing logic remains deterministic.
   - Connected Tests: `./gradlew connectedDebugAndroidTest` runs Compose UI tests on emulators.
3. **Security Auditing:**
   - Local execution of Dependency Check (OWASP) ensures no vulnerable transitive dependencies are shipped.
4. **Artifact Generation:**
   - Generates the App Bundle (`./gradlew bundleRelease`) for the Play Store.
   - Generates the universal APK (`./gradlew assembleRelease`) for GitHub Releases and Enterprise sideloading.
5. **Release Tagging:**
   - Automated Semantic Versioning pushes based on Git Tags (e.g., `v2.0.0`).
