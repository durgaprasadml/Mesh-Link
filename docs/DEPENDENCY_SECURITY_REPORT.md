# Dependency Security Report

Mesh Link minimizes external dependencies to reduce supply-chain risk.

## Critical Dependencies
1. **Zetetic SQLCipher (`net.zetetic:sqlcipher-android`)**
   - *Status*: Up-to-date (v4.6.1). 
   - *Risk*: Low. Industry standard for SQLite encryption.
2. **AndroidX Security (`androidx.security:security-crypto`)**
   - *Status*: Up-to-date.
   - *Risk*: Low. Backed by Google Tink primitives.
3. **Firebase Crashlytics**
   - *Status*: Up-to-date.
   - *Risk*: Medium. Potential privacy risk. Mitigated by strict log sanitization rules implemented in `MeshLogger`.

## Transitive Dependency Scan
Gradle dependencies were evaluated using standard OWASP dependency checks. No known CVEs affect the current build configuration.
