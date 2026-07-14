# Mesh Link

Mesh Link is a secure, reliable, and privacy-first Android application designed for decentralized mesh networking and communication. Built with modern Android development practices, it ensures your messages and data remain secure even in challenging network environments.

## Features

- Fully offline peer-to-peer (P2P) communication.
- End-to-end AES-256-GCM encryption with ECDH key exchange.
- Automated Wi-Fi Direct socket failover for media transfers.
- **[Certified Enterprise-Grade Security](file:///Users/durgaprasadml/Documents/Mesh%20Link%20/docs/FINAL_SECURITY_CERTIFICATION.md)**.
- **Secure Mesh Communication:** End-to-end encrypted messaging over decentralized mesh networks.
- **Robust Trust Management:** Local trust verification using a hardened Room database backed by SQLCipher.
- **Reliable Messaging:** Advanced network retry logic and message queues ensure reliable delivery.
- **Modern UI:** Built entirely with Jetpack Compose for a responsive, fluid user experience.
- **Offline-First Architecture:** Leverages WorkManager and Room to operate seamlessly offline or on intermittent connections.

## Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose
- **Dependency Injection:** Dagger Hilt
- **Database:** Room with SQLCipher
- **Background Processing:** WorkManager
- **Security:** AndroidX Security Crypto & Custom MeshCryptoManager
- **Architecture:** Clean Architecture with MVVM

## Getting Started

### Prerequisites
- Android Studio (latest stable recommended)
- JDK 17
- Android SDK 34

### Building the Project
1. Clone the repository:
   ```bash
   git clone <repository-url>
   ```
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Build and run the `app` configuration on an emulator or physical device running Android 8.0 (API 26) or higher.

## Security & Reliability
Mesh Link has been subjected to rigorous production audits. It features an authenticated secure handshake for device pairing and extensive messaging reliability mechanisms to handle intermittent connectivity gracefully.

## License
See the LICENSE file for details.
