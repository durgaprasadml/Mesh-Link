# Media Stability

## Handling Large Files
- `MediaTransferManager` strictly chunks 300-byte B64 payloads, keeping below 512-byte MTU limits.
- Strict META/CHUNK ordering enforced.
- `TransferManager` integration restored for background file send robustness.