# ADR-001: Kotlin Multiplatform Architecture & Pluggable AI Runtimes

- **Status**: Accepted
- **Deciders**: Architecture Team
- **Date**: 2026-09-30
- **Target Platforms**: Android & iOS
- **Component**: Core / Ingestion / Retrieval / Governance / Platform Bridges

---

## 1. Context and Problem Statement
RagChat SDK requires enterprise-grade on-device RAG capability, secure token handling, encrypted storage, and pluggable local/cloud LLM inference for both Android and iOS while maximizing code sharing and providing platform-idiomatic consumer APIs.

## 2. API & Dependency Version Anchors

| Dependency / Runtime | Target Platform | Verified Version | Official Documentation / Release URL | Hardware / OS Requirements |
| :--- | :--- | :--- | :--- | :--- |
| Kotlin Multiplatform | Android / iOS | 2.0.21 | https://kotlinlang.org/docs/multiplatform.html | Kotlin 2.x |
| SKIE | iOS / KMP | 0.9.3 | https://skie.touchlab.co/ | Xcode 15+ / iOS 15+ |
| Google LiteRT / MediaPipe | Android | v0.10.x | https://ai.google.dev/edge/litert | Android 10+ (API 29+) |
| Apple Foundation Models | iOS | iOS 18+ | https://developer.apple.com/documentation/ | iOS 18+, Apple Silicon |
| SQLDelight | Android / iOS | 2.0.2 | https://cashapp.github.io/sqldelight/ | SQLite 3.24+ |

## 3. Decision Drivers
- Zero JVM-only or Android-only APIs in `commonMain`.
- Interface-first injection for all hardware/OS adapters.
- Strict PII redaction and audit hash chain before model dispatch or persistence.
- Zero raw prompt/embedding/document logging.
- Touchlab SKIE for converting Kotlin `Flow` to Swift `AsyncSequence` and sealed classes to Swift enums.

## 4. Decision Outcome
Chosen option: Multi-module KMP architecture with convention plugins in `build-logic`, explicit API mode enabled, and XCFramework packaging for iOS.
