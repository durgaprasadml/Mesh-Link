# GitHub Actions Deployment Guide

Mesh Link is configured for automated CI/CD via GitHub Actions (or any standard CI pipeline).

## Required Secrets
To build and deploy the Release AAB, ensure the following secrets are injected into the CI environment:
- `KEYSTORE_FILE_BASE64`: The production `.jks` file encoded in base64.
- `KEYSTORE_PASSWORD`: Password for the Keystore.
- `KEY_ALIAS`: Alias of the signing key.
- `KEY_PASSWORD`: Password for the specific key alias.
- `PLAY_STORE_JSON`: Google Play Developer API service account JSON for fastlane/Gradle play publisher deployment.

## Build Steps
A standard CI workflow should execute the following:
1. `base64 --decode <<< $KEYSTORE_FILE_BASE64 > release.jks`
2. `./gradlew lintRelease`
3. `./gradlew test`
4. `./gradlew bundleRelease`
5. Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console.
