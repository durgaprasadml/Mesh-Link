# RC2 Media Chunking Report

## Overview
This report analyzes media transfer and chunking memory profiles.

## Optimizations
1. **List Capacities:**
   - Pre-allocated target list size `ArrayList<MeshPacket>(totalChunks + 1)` in `MediaTransferManager.kt`.
2. **Buffer Limits:**
   - Avoided continuous auto-resizing of `ArrayList` during media conversion.

## Expected Metrics
- **Heap Thrashing:** Reduced during generation of packet lists for multi-megabyte files.
