# Incident Response Plan

## Scope
Defines the engineering response to critical, fleet-wide bugs (e.g. Mass Device Outage, Severe Routing Loops) discovered post-GA deployment.

## Escalation Matrix
1. **Detection:** Reported via Enterprise MDM channels or Play Console ANR spikes.
2. **Containment:** 
   - Pause any active staged rollouts on the Play Store.
   - If the bug is a catastrophic infinite routing loop, advise users to turn OFF Bluetooth to manually isolate the node.
3. **Eradication & Recovery:**
   - Cut a hotfix branch (`hotfix/v2.0.1`) from `main`.
   - Patch the routing limit or memory leak.
   - Because `Room` Database schemas cannot be safely downgraded, the hotfix **MUST** be a patch-forward (incrementing `versionCode`).
4. **Post-Mortem:** Document the failure inside a Root Cause Analysis (RCA) artifact for future sprint planning.
