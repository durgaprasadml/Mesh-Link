# Incident Response Guide (Severity 1)

## Philosophy
In a decentralized application, a "Severity 1 Outage" is defined as a catastrophic bug in the APK that corrupts data or causes a Boot Loop.

## Mitigation Steps
1. **Identify the Scope:** Does the crash occur on startup (Boot Loop) or during a specific action?
2. **Halt Rollout:** Pause Google Play staged rollouts immediately.
3. **Patch Forward:** Because SQLite migrations prohibit database downgrades, the engineering team MUST push an emergency hotfix (e.g. `v2.0.1`) that mitigates the crash while preserving the user's encrypted database state.
4. **Distribute Hotfix:** MDMs can force-push the updated APK to affected field fleets.
