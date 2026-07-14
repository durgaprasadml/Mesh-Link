# Release Engineering Guide

## Versioning Strategy
Mesh Link strictly adheres to **Semantic Versioning (SemVer 2.0.0)**: `MAJOR.MINOR.PATCH`
- **MAJOR:** Incompatible API or Mesh Protocol changes (e.g., altering the ECDH negotiation structure).
- **MINOR:** New backward-compatible functionality (e.g., adding Video support).
- **PATCH:** Backward-compatible bug fixes and performance enhancements (e.g., RC optimizations).

## Release Flow (Git Flow)
1. Features are merged into `develop`.
2. A `release/vX.Y.Z` branch is cut for stabilization (like the RC1-RC5 phases).
3. The release branch is merged into `main` and tagged `vX.Y.Z`.
4. Hotfixes are branched directly off `main` into `hotfix/vX.Y.Z+1` and merged back to both `main` and `develop`.

## Rollback Strategy
Due to the Room Database migration requirements, automatic rollbacks are structurally prohibited by Android. If a catastrophic bug is discovered in Production, a rapid **Patch Forward** is the required mitigation strategy.
