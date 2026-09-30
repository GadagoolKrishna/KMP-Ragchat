# ADR-003: Storage, Persistence & Platform Encryption Architecture

## Context
RagChat SDK requires multiplatform storage across Android and iOS supporting:
1. Structured metadata, vector embeddings (quantized int8), document chunks, lexical FTS matching, ingestion jobs, and tamper-evident audit logs.
2. Scope isolation per user/workspace on all persistent records.
3. Envelope encryption and encryption at rest with hardware-backed key providers.
4. Fast purge, crypto-shredding, and exclusion from OS cloud backups.

## Decision

### 1. SQL Engine: SQLDelight Multiplatform Driver Strategy
- **Shared Schema**: `RagChatDatabase` (`RagChat.sq`) generates multiplatform types and query interfaces across all KMP targets.
- **Scope Partitioning**: Every database table features `scope_id TEXT NOT NULL` as its leading composite primary key component or index.
- **Android Driver**: `AndroidSqliteDriver` with PRAGMA foreign keys enabled and SQLCipher encryption support.
- **iOS Driver**: `NativeSqliteDriver` on iOS targets. For iOS file-level encryption, `NSFileProtectionComplete` and hardware Secure Enclave envelope encryption are applied, excluding files from iCloud backups via `NSURLIsExcludedFromBackupKey`.

### 2. Scalar Quantization
- Symmetric int8 scalar quantization is utilized for dense vector storage in the `embeddings` table, compressing raw 32-bit floats into signed bytes (`[-128, 127]`) alongside an invariant scaling factor `scale = max(|v|) / 127.0f`.
- Achieves 4x reduction in disk and RAM footprint while maintaining cosine similarity fidelity within a tolerance of `< 0.05`.

### 3. Key Management & Envelope Encryption
- **Android**: `AndroidKeystoreKeyProvider` creates AES-256-GCM master keys backed by hardware StrongBox Keymaster where available, falling back to TEE. Raw document payloads use streaming AES-GCM envelope encryption with per-file Data Encryption Keys (DEKs).
- **iOS**: `IosKeychainKeyProvider` enforces `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
- **Backup Exclusion**:
  - Android: `res/xml/backup_rules.xml` excludes `database`, `ragchat_storage`, and shared preferences from Auto Backup.
  - iOS: Sandboxed storage URLs set `NSURLIsExcludedFromBackupKey = true`.

### 4. Contract Testing
- Abstract contract test suites (`StorageContractSuite`, `EncryptionAtRestContractSuite`) in `:sdk-testing` verify scope isolation, cascade purges, crypto-shredding, and quantization roundtrips across all platform storage adapters.

## Consequences
- Clean separation between common domain models, SQLDelight database operations, and platform hardware adapters.
- Compliance with enterprise privacy and data isolation mandates.
