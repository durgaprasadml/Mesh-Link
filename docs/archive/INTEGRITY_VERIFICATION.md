# Integrity Verification

## Checksum Algorithms
The engine uses **SHA-256** for all file integrity validation.

## Workflow
1. **Pre-Transmission**: `FileMetadataManager` calculates the SHA-256 hash of the complete file and includes it in the `MEDIA_META` packet.
2. **Transmission**: The file is sliced and transmitted. Base64 encoding inherently protects against non-printable byte corruption, but dropped chunks are tracked via missing indices in the `TransferCache`.
3. **Post-Assembly**: Once all chunks arrive, `TransferCache` stitches the `*.chk` fragments into a contiguous binary file in `mesh_media/`. `IntegrityVerifier` immediately computes the SHA-256 of the assembled file.
4. **Validation**: If the computed hash matches the expected hash from the META packet, the file is committed to the database and exposed to the UI. If it fails, the file is securely deleted from storage, and a NACK/failure is bubbled up to the sender to prevent corrupted data from poisoning the system.
