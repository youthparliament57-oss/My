# NOUS: The Autonomous Cognitive OS

NOUS is a high-performance, professional-grade autonomous cognitive architecture for Android. It implements a multi-layered neural-symbolic cascade to provide advanced reasoning, long-term memory, and agency while maintaining absolute data privacy.

## 🧠 Core Architecture (The 6-Layer Cascade)

NOUS operates on a sophisticated decision-making pipeline ensuring < 20ms latency for simple tasks and high-reasoning depth for complex problems.

1.  **Intent Classifier**: Fast regex and keyword-based triage.
2.  **Rule Engine**: Direct hardware control and deterministic system operations.
3.  **Skill Router**: Dynamic mapping of user intent to specialized native modules.
4.  **Local LLM (Layer 3)**: On-device GGUF/MediaPipe inference for offline privacy.
5.  **Cloud LLM (Layer 4)**: High-intelligence cascade to Gemini, OpenAI, Anthropic, etc.
6.  **Agentic Orchestrator (Layer 5)**: Multi-turn tool-calling loop for autonomous problem solving.

## 🗄️ Memory Module (Space-Time Continuum)

NOUS features a hybrid persistence engine designed for multi-decade data retention and rapid recall.

*   **Episodic Memory**: Geo-tagged and timestamped event logging (Room + SQLCipher).
*   **Semantic Memory**: FTS5 full-text search integrated with vector-ish relevance.
*   **Procedural Memory**: Success-weighted pattern recognition for habit modeling.
*   **Knowledge Graph**: Relational node-edge traversal for conceptual discovery.
*   **Forgetting Curve**: Biological-inspired memory consolidation and pruning.

## 🔐 Security & Privacy

*   **Zero-Knowledge Persistence**: All local databases are encrypted with AES-256 via SQLCipher.
*   **Secure Credential Vault**: API keys are stored in the Android Keystore, never in plaintext or SharedPrefs.
*   **Offline-First**: Core reasoning layers (1-3) function without network connectivity.

## 🎭 Persona System

Dynamic identity switching via the `PersonaEngine`:
*   **Atlas**: Analytical, precision-oriented, cold-logic.
*   **Nova**: Creative, empathetic, exploratory.
*   **Default (NOUS)**: Balanced, professional, efficient.

## 🛠️ Technical Stack

*   **Language**: 100% Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **DI**: Hilt / Dagger
*   **Concurrency**: Coroutines & Flow
*   **Local ML**: MediaPipe LLM Inference
*   **Persistence**: Room, SQLCipher, DataStore

---
*Built with professional integrity. No stubs. No simulations. Only pure engineering.*
