# Release Governance Policy

## Release Cadence
1. **Major Releases (v3.0.0):** Occur annually. Involve massive architectural shifts (e.g., adding Satellite or Mesh-over-Internet bridging).
2. **Minor Releases (v2.1.0):** Occur quarterly. Add non-breaking features like UI refinements or new offline-map integrations.
3. **Patch Releases (v2.0.1):** Occur strictly as needed to resolve high-severity bugs (Crashes, ANRs, Routing Loops).

## Rollout Strategy
- **Play Store:** All releases utilize Staged Rollouts (e.g., 5% -> 20% -> 50% -> 100%) to isolate catastrophic bugs before full fleet deployment.
- **Enterprise / Sideloading:** Releases are uploaded to GitHub Releases simultaneously for immediate MDM deployment.
