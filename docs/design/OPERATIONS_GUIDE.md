# V1.0 Operations & Release Guide

## Git Release Strategy
For the V1.0 launch and all future version bumps, follow standard Git Flow:
1. **Branching**: Cut a `release/v1.0.0` branch from `develop`.
2. **Version Bump**: Update `versionCode` and `versionName` in `app/build.gradle.kts`.
3. **Hardening**: Execute `./gradlew lint test`. Fix any regressions.
4. **Merge**: Merge `release/v1.0.0` into `main`. Tag the commit as `v1.0.0`.
5. **Build**: Trigger the CI/CD pipeline on `main` to generate the signed `.aab`.

## Disaster Recovery
If a severe cryptographic bug or database migration failure is detected in production:
1. Immediately cut a `hotfix/v1.0.1` branch from `main`.
2. Do **NOT** attempt to write a complex database rollback. Instead, issue a patch that safely drops the corrupted tables and forces a clean re-sync via the mesh.
3. Fast-track the AAB to the Play Store Beta track for immediate verification.
