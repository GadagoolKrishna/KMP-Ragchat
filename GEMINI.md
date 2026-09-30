# RagChat SDK - Engineering Guidelines & Architectural Constraints

Project: **RagChat SDK** (Enterprise-grade Kotlin Multiplatform SDK for Android & iOS)
Scope: Provides chat UI, on-device RAG (document ingestion, chunking, embeddings, retrieval), and pluggable LLM orchestration (Gemini Nano, Gemma via LiteRT/MediaPipe on Android; Apple Foundation Models and Gemma/on-device runtimes on iOS; cloud fallback APIs on both).

---

## 1. Toolchain & Multiplatform Targets
- **Language & Runtime**: Kotlin 2.x with Kotlin Multiplatform (KMP).
- **Supported Targets**:
  - `androidTarget`
  - `iosArm64`
  - `iosSimulatorArm64`
  - `iosX64` (retained only if required for Intel macOS developer environments; otherwise pruned).
- **Core Multiplatform Libraries**:
  - Concurrency: `kotlinx.coroutines` and `kotlinx.coroutines.flow.Flow`
  - Serialization: `kotlinx.serialization` (JSON / binary protobuf where applicable)
  - Time: `kotlinx.datetime`
  - Networking: Ktor Client with platform engines (`OkHttp` / `Darwin`)
  - File I/O: Okio (`okio.FileSystem`, `okio.Path`)
- **Strict Rule**: Zero JVM-only or Android-only APIs in `commonMain` (no `java.*`, `javax.*`, `android.*`).

---

## 2. Architecture & Layering (`commonMain`)
All core domain logic, abstractions, and coordination live strictly in `commonMain`:
- **SPIs (Service Provider Interfaces)**:
  - LLM Runtime abstraction (`LlmEngine`, `EmbeddingModel`)
  - Vector Store / Storage engine (`VectorStore`, `DocumentStore`)
  - Keyring / Keystore wrapper (`SecureKeyStore`)
  - Auth provider (`AuthProvider`)
  - Platform file system access bridges
- **Core Subsystems in `commonMain`**:
  - Document ingestion pipelines & chunkers (Token-aware, Recursive, Fixed-window)
  - Retrieval algorithms (Cosine similarity, Hybrid lexical/BM25 + dense vector ranking, rerankers)
  - Execution Router (On-device vs. Cloud orchestration based on model availability, battery, latency, and context window)
  - Policy Engine & Safety checks
  - PII Redaction engine (Regex-based token replacement/masking before prompt assembly or persistence)
  - Audit hash chain (Immutable hash-chained logging of execution events without storing sensitive data)
  - Prompt template builder & tokenizer constraints

---

## 3. Dependency Injection & Platform Adapters
- **Interface-First Injection**: Platform-specific implementations (e.g. MediaPipe LLM Inference, LiteRT, Apple Foundation Models, iOS Keychain, Android EncryptedSharedPreferences/Keystore) must be passed into `commonMain` components via constructor injection / interfaces defined in `commonMain`.
- **Minimal `expect` / `actual`**:
  - Confined exclusively to low-level primitives:
    - Current monotonic / epoch timestamps
    - Cryptographically secure UUID generation
    - App-private file paths and sandboxed directory discovery
  - Business logic, platform AI bindings, and persistent data engines must NOT use `expect`/`actual`; they must use interfaces and SPI implementations.

---

## 4. Public API Ergonomics & Swift Interop
- **Dual-Platform Idiomatic API**:
  - Kotlin / Android consumers use standard Coroutines (`suspend`), `StateFlow`, `SharedFlow`, and Kotlin sealed hierarchies.
  - iOS consumers must interact with a Swift-friendly facade:
    - Bridge Coroutine `Flow` to Swift `AsyncSequence` / Combine publishers.
    - Export Kotlin `sealed class` / `sealed interface` as native Swift enums with associated values using SKIE (or equivalent modern KMP Swift compiler bridge).
    - Provide clean Swift package / XCFramework packaging with Swift-style naming conventions.

---

## 5. Binary Compatibility & Public API Governance
- **Explicit API Mode**: Enabled in Gradle (`explicitApi()`) to force explicit visibility modifiers (`public`, `internal`, `private`) and explicit return types on all declarations.
- **Android / JVM API Validation**: Enforce `binary-compatibility-validator` (BCV) to generate `.api` dumps and track binary compatibility across pull requests.
- **iOS API Validation**: Enforce automated XCFramework API diff checks (e.g., using `swift-api-digester` or header diffing) in CI to prevent breaking Swift surface changes.

---

## 6. Privacy, Redaction & Security Controls
- **Logging Prohibitions**:
  - **NEVER** log raw prompt texts, document content, chunk payloads, embedding vectors, model completions, or personally identifiable information (PII).
  - Use structured, metadata-only audit records (token counts, latency, status codes, rule violation tags).
- **PII Redaction**:
  - All prompt inputs and indexed documents must pass through the regex-based PII redaction engine before dispatching to model endpoints or persisting to logs.
- **No Embedded Credentials**:
  - Absolutely no hardcoded API keys, secrets, or fallback bearer tokens in SDK source or configuration.
  - Require consumers to supply an `AuthProvider` implementation that fetches tokens dynamically.

---

## 7. Storage, Persistence & Data Protection
- **Encrypted at Rest**:
  - All persisted data (vector indices, chunk metadata, chat history caches) must be encrypted using platform hardware-backed keys (Android Keystore / iOS Keychain).
- **App-Private Sandboxing**:
  - All data files must reside inside app-private internal directories.
- **Backup Exclusion**:
  - All database/storage files and caches must be explicitly excluded from OS cloud backups:
    - Android: Marked with `backup_rules` / excluded from Auto Backup (`android:fullBackupContent`).
    - iOS: Storage URLs flagged with `isExcludedFromBackupKey = true`.

---

## 8. AI Runtime Documentation & Version Anchoring
- **Pre-Implementation Verification**:
  - Verify official documentation and release notes before writing bindings for Google (LiteRT, MediaPipe LLM, Gemini Nano) or Apple (Foundation Models, CoreML) AI APIs.
- **Architecture Decision Records (ADRs)**:
  - Document runtime versions, API deprecation timelines, hardware compatibility tiers, and platform trade-offs under `docs/decisions/ADR-xxx.md`.

---

## 9. Testing Strategy & Contract Verification
- **Behavioral Tests (`commonTest`)**:
  - All core logic (chunking algorithms, vector similarity, router rules, PII filters, audit chain hashing) must be tested in `commonTest` and executed across all configured targets (Android + iOS Simulator).
- **Contract Tests**:
  - Platform adapters (vector store persistence, secure key storage, model inference bridges) must provide shared abstract contract test suites in `commonTest` that concrete platform targets inherit and run against platform implementations.

---

## 10. Definition of Done (DoD)
A feature or change is done only when:
1. Builds cleanly for both Android (`aar`) and iOS (`XCFramework` / iOS Simulator tests).
2. All unit and contract tests pass across both targets.
3. All public APIs feature complete KDoc documentation.
4. An ADR is created/updated for architectural or dependency decisions.
5. Static analysis and linting (ktlint/detekt, compiler warnings-as-errors) are clean.
