# ADR-[NUMBER]: [Short Title of the Decision]

- **Status**: [Proposed | Accepted | Deprecated | Superseded]
- **Deciders**: [List of key stakeholders / engineers]
- **Date**: YYYY-MM-DD
- **Target Platforms**: [Android | iOS | Common]
- **Component**: [LLM Engine | Vector Store | PII Redaction | Network | Auth | Storage]

---

## 1. Context and Problem Statement
[Describe the context, user need, or technical problem triggering this decision. What constraints, performance requirements, or platform differences must be addressed?]

## 2. API & Dependency Version Anchors
> [!IMPORTANT]
> Verify official documentation and SDK release notes before adopting any AI runtime or platform library.

| Dependency / Runtime | Target Platform | Verified Version | Official Documentation / Release URL | Hardware / OS Requirements |
| :--- | :--- | :--- | :--- | :--- |
| e.g. Google MediaPipe GenAI / LiteRT | Android | vX.Y.Z | https://ai.google.dev/... | Android 10+ (API 29+), GPU/NPU recommended |
| e.g. Apple Foundation Models / CoreML | iOS | iOS 18.x+ / SDK X | https://developer.apple.com/... | Apple Silicon / Neural Engine |
| e.g. SKIE | iOS / KMP | vX.Y.Z | https://skie.touchlab.co/... | Xcode 16+ |

## 3. Decision Drivers
- [Driver 1, e.g. On-device latency under 500ms for first token]
- [Driver 2, e.g. Zero private data / raw document transmission off device]
- [Driver 3, e.g. Memory budget <= 1.5GB total on mid-tier mobile hardware]
- [Driver 4, e.g. Idiomatic Kotlin Flow and Swift AsyncSequence interoperability]

## 4. Considered Options
1. **Option A**: [Description]
2. **Option B**: [Description]
3. **Option C**: [Description]

## 5. Decision Outcome
Chosen option: **Option [A/B/C]**, because [justification covering performance, platform idioms, memory, security, and maintenance overhead].

### Positive Consequences
- [Positive consequence 1]
- [Positive consequence 2]

### Negative Consequences / Trade-offs
- [Trade-off or limitation 1]
- [Mitigation strategy]

## 6. Implementation Notes & Architecture Impact
- **Interface / SPI**: [`commonMain` interface impacted, e.g. `LlmEngine`]
- **Platform Adapter**: [Platform-specific classes, e.g. `AndroidLiteRtEngine`, `AppleCoreMlEngine`]
- **Security & Privacy Considerations**: [Encryption, memory scrubbing, exclusion from backups, PII redaction checks]
- **Fallback Behavior**: [E.g., graceful fallback to cloud AuthProvider endpoint if on-device model unavailable]

## 7. Compliance with SDK Core Rules
- [ ] No JVM-only APIs in `commonMain`
- [ ] Platform implementation injected via interface (not arbitrary `expect`/`actual`)
- [ ] Zero raw prompt, document text, embedding, or PII logging
- [ ] Data stored in encrypted app-private directory excluded from OS backups
- [ ] Contract tests added to `commonTest` for platform verification
- [ ] Public API documented with KDoc and checked via explicit API mode / BCV / XCFramework diff
