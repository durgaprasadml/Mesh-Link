# Mesh-Link Production Release Engineering Checklist

Use this checklist for every official release candidate (RC) and final production build.

---

## 1. Pre-Release Verification
- [ ] Codebase clean with no uncommitted draft edits.
- [ ] `versionCode` updated in `app/build.gradle.kts`.
- [ ] `versionName` updated in `app/build.gradle.kts`.
- [ ] `CHANGELOG.md` updated with release notes and migration notices.

## 2. Automated Quality Gates & Static Analysis
- [ ] Detekt static analysis passes clean (`./gradlew detekt`).
- [ ] Android Lint passes with 0 critical errors (`./gradlew lint`).
- [ ] Unit test suite passes 100% (`./gradlew test`).
- [ ] Instrumentation tests pass (`./gradlew connectedAndroidTest`).
- [ ] Performance benchmarks verified (`./gradlew :benchmark:connectedCheck`).

## 3. Dependency & Security Hardening Verification
- [ ] Gradle Dependency Verification passes (`gradle/verification-metadata.xml`).
- [ ] Dependency Audit clean with 0 known vulnerabilities (`DependencyAudit.md`).
- [ ] Runtime security checks validated (`SecurityHardening.kt`).
- [ ] Log audit verified: `VERBOSE`/`DEBUG` disabled in release build and payload contents redacted.

## 4. Build Generation & Signing Verification
- [ ] Release APK compiled (`./gradlew assembleRelease`).
- [ ] Release Android App Bundle compiled (`./gradlew bundleRelease`).
- [ ] Release signing validated with official keystore credentials (never debug keys).
- [ ] ProGuard/R8 mapping file archived (`app/build/outputs/mapping/release/mapping.txt`).
- [ ] SHA-256 artifact checksums generated and recorded.

## 5. Play Store Distribution Verification
- [ ] `PlayStoreReadinessReport.md` audited and approved.
- [ ] `network_security_config.xml` HTTPS cleartext traffic enforcement confirmed.
- [ ] `OPEN_SOURCE_LICENSES.md` attribution current.
- [ ] `PRODUCTION_DEPLOYMENT_REPORT.md` generated.
- [ ] Artifacts uploaded to Google Play Console / Enterprise Distribution Channel.
