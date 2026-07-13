# About Mesh Link

## The Vision
Mesh Link was created with a single core philosophy: **communication should be secure, reliable, and decentralized**. In an era where centralized infrastructure can be a single point of failure or surveillance, Mesh Link provides a robust alternative. 

By utilizing mesh networking principles, devices connect directly with one another, forming a resilient web of communication that doesn't rely solely on traditional internet backbones.

## Security First
Security isn't an afterthought in Mesh Link; it's the foundation. 
- **Local Storage:** All local data is encrypted at rest using SQLCipher.
- **Handshakes:** Devices authenticate using a secure, cryptographic handshake protocol to prevent spoofing and man-in-the-middle attacks.
- **Trust Scores:** The app maintains dynamic trust scores for nodes in the mesh, isolating bad actors automatically through the `TrustDao` and local trust management.

## Architecture
Mesh Link uses a modular, clean architecture to ensure the codebase remains maintainable, scalable, and testable.
- **Data Layer:** Handles local persistence (Room + SQLCipher) and network/mesh transmission.
- **Domain Layer:** Contains core business logic, including the metrics management and cryptography (`MeshCryptoManager`).
- **UI Layer:** Implemented entirely in Jetpack Compose, reacting to state changes from ViewModels.

## Community & Contributions
We believe in open, transparent security. We welcome contributions from the community. Whether it's hardening the cryptography, improving battery efficiency in the mesh algorithms, or refining the Compose UI, your help is appreciated!
