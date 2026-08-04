## 2026-07-28 - [Performance & Testing]
**Learning:** Breaking mocked test dependencies when applying sequence collection updates leads to cascading failures in CI. Native initializations via JNI bindings must remain exactly inside component initialization logic if tied to instance context or if JNI strictly expects such lifecycle.
**Action:** When refactoring codebase layers (like collections in pipeline engines), skip invasive re-writing of existing `app/src/test` suites unless specifically asked. Only modify source layers and execute tests safely.
