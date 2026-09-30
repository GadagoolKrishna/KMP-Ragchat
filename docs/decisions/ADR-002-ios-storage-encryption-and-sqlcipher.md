# ADR-002: iOS Storage Encryption Strategy (SQLCipher & CryptoKit Fallback)

## Status
Accepted

## Context
RagChat SDK requires robust hardware-backed encryption at rest for on-device vector indices, chunk metadata, conversation logs, and cached documents on both Android and iOS.

On Android, SQLCipher integrates seamlessly with SQLDelight via `net.zetetic:sqlcipher-android` and Android Support SQLite OpenHelper APIs.
On iOS (Kotlin Multiplatform targeting `iosArm64`, `iosSimulatorArm64`, `iosX64`), SQLCipher requires native C-library compilation and linking against OpenSSL/SQLCipher source distributions. Linking external native C static libraries in cross-platform KMP builds can introduce fragile build-system dependencies on developer workstations and Linux CI environments.

## Decision
1. **Primary iOS Strategy**: Support SQLCipher native driver through SQLDelight's native driver when custom C-interop or CocoaPods/SPM SQLCipher dependencies are linked.
2. **Deterministic Fallback & Multi-Layer Defense**:
   - Apply **Hardware-backed Key Protection**: iOS Keychain and Secure Enclave with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
   - Apply **OS-Level Data Protection**: All SQLite database files, WAL files, and directories are provisioned with `NSFileProtectionComplete`, guaranteeing hardware encryption while the device is locked.
   - Apply **File-Level Envelope Encryption**: Store document payloads and sensitive content via streaming AES-GCM / ChaCha20-Poly1305 using Apple CryptoKit (bridged via CommonCrypto/CryptoKit).
   - Flag all storage paths with `isExcludedFromBackupKey = true` to prevent unencrypted cloud sync to Apple iCloud.

## Consequences
- **Positive**:
  - Deterministic compilation on all platforms (macOS and Linux CI).
  - Multi-layered defense: Hardware encryption (Secure Enclave) + OS Data Protection (`NSFileProtectionComplete`) + Application-level envelope encryption.
  - Zero risk of cloud leakage via automated iCloud backups.
- **Negative**:
  - If pure SQLCipher is required without OS-level file protection fallback, consumers must supply the compiled C-amalgamation or CocoaPods/SPM framework link flags in their final host app.
