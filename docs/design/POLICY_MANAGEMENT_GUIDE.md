# Policy Management Guide

## Enforcing Operational Rules

### `PolicyManager`
- Acts as the central gatekeeper for feature execution.
- If a user attempts to send a 50MB video file, the UI layer queries `PolicyManager.isMediaTransferAllowed()`.
- If the MDM has set `disable_media_transfers = true`, the action is blocked at the source, preventing unnecessary UI states or networking attempts.
