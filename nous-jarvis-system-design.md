# NOUS JARVIS — Comprehensive System Design Specification
## Unified Frontend (Android Multi-Module) & Backend (High-Performance Zero-Knowledge Cloud) Architecture

> *"The mind that perceives, the hand that executes."* — The NOUS Blueprint

---

## 📋 Table of Contents
1. [Executive Architectural Blueprint](#1-executive-architectural-blueprint)
2. [Frontend Android Architecture (14-Module Stack)](#2-frontend-android-architecture-14-module-stack)
3. [Backend Server Architecture (Distributed Cloud Node)](#3-backend-server-architecture-distributed-cloud-node)
4. [Real-time Communication Protocols (Bi-directional Stream Gateway)](#4-real-time-communication-protocols-bi-directional-stream-gateway)
5. [Zero-Knowledge Security & Cryptographic Handshakes](#5-zero-knowledge-security--cryptographic-handshakes)
6. [Actionable Implementation Roadmap](#6-actionable-implementation-roadmap)

---

## 1. Executive Architectural Blueprint

NOUS is not a simple wrapper or chat playground. It is a production-grade, highly responsive, privacy-first AI companion (JARVIS-like assistant) designed to execute system-level operations, reasoning tasks, and long-term memory retrieval under a strictly enforced **Federated Local-Cloud model**. 

```
                                  +---------------------------------------+
                                  |            EXTERNAL WORLD             |
                                  |   (SaaS APIs, Web Search, IoT, etc.)   |
                                  +------------------^--------------------+
                                                     |
                                                     | HTTPS / Scrapers
                                                     v
+----------------------------------------------------+----------------------------------------------------+
|                                              BACKEND NODE                                               |
|                                                                                                         |
|  +---------------------------+   +---------------------------+   +-----------------------------------+  |
|  |     WEB AGENT RUNTIME     |   |    ZERO-KNOWLEDGE SYNC    |   |         COGNITIVE GRAPH           |  |
|  | (Headless Browser/Puppet) |<--|   (AES-256-GCM Decrypt)   |-->|   (Neo4j / pgvector Memory)       |  |
|  +---------------------------+   +---------------------------+   +-----------------------------------+  |
|                ^                               ^                                   ^                    |
|                |                               | Secure E2EE Payload               | GraphQL / Cypher   |
|                +-------------------------------+-----------------------------------+                    |
|                                                | gRPC / WebSockets (TLS 1.3)                                    |
+------------------------------------------------+--------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------------+
|                                        FRONTEND (ANDROID STACK)                                         |
|                                                                                                         |
|  +---------------------------------------------------------------------------------------------------+  |
|  |                            LAYER 0 TO 5 CASCADE BRAIN (INTENT ROUTER)                              |  |
|  |       [Layer 0: Intent] -> [Layer 1: Rules] -> [Layer 2: Skills] -> [Layer 3: Local LLM]          |  |
|  |                           -> [Layer 4: Cloud LLM] -> [Layer 5: Agentic]                            |  |
|  +---------------------------------------------------------------------------------------------------+  |
|           |                           |                           |                           |         |
|           v                           v                           v                           v         |
|  +------------------+        +------------------+        +------------------+        +------------------+  |
|  |   MEMORY STACK   |        |   VOICE STACK    |        |   VISION STACK   |        |   AGENT STACK    |  |
|  | (Room/SQLCipher) |        | (Whisper JNI /   |        |  (SSD TFLite /   |        | (LADB & Access.  |  |
|  |  [Ebbinghaus CL] |        |  Piper Neural)   |        |    Omni-SLM)     |        |   Automation)    |  |
|  +------------------+        +------------------+        +------------------+        +------------------+  |
+---------------------------------------------------------------------------------------------------------+
```

### Core Design Philosophy
1. **Cascade Execution Pattern**: Run cheap, on-device heuristics first (sub-5ms intent classifier, sub-10ms rule-based regex execution, sub-20ms Kotlin hardcoded local skills). Escalate to heavy on-device LLMs (llama.cpp JNI) and cloud processing ONLY when local lightweight layers fail or explicit user parameters command it.
2. **Strict Zero-Knowledge Data Sovereignty**: All personal episodic, semantic, and emotional memory is stored on-device in a robust SQLCipher-encrypted Room database. When synchronized with the Backend for multi-device coordination or heavy graph-RAG compilation, payloads are encrypted end-to-end (E2EE) with keys derived strictly from user biometric entropy and hardware keystores (StrongBox/TEE). The backend acts as an illiterate custodian.
3. **Hardware-In-The-Loop Execution**: The backend processes high-computation reasoning steps, large visual embeddings, and SaaS integrations. The Android client executes physical hardware states (telephony, Bluetooth, sound levels, system-wide clicks, typing, and notifications) using standard frameworks coupled with a persistent rollback-safe system operation undo stack.

---

## 2. Frontend Android Architecture (14-Module Stack)

The frontend is structured into highly cohesive, loosely coupled Gradle modules complying with Clean Architecture rules.

```
                         :app (Hilt Configuration, Custom Watchdog, Main Hook)
                                           |
                    +----------------------+----------------------+
                    |                                             |
         :feature:screens (Compose UI UI)               :feature:brain (Core Router)
                    |                                             |
                    +----------------------+----------------------+
                                           |
        +------------------+---------------+------------------+------------------+
        |                  |               |                  |                  |
 :feature:memory    :feature:voice   :feature:vision    :feature:agent     :feature:system
 (Room/SQLCipher)   (Whisper/Piper)   (TFLite/MLKit)     (LADB/Access)      (19 Handlers)
        |                  |               |                  |                  |
        +------------------+---------------+------------------+------------------+
                                           |
                                      :core:common 
                            (Result, AppError, Dispatchers)
                                           |
                    +----------------------+----------------------+
                    |                                             |
           :core:security (Keystore)                    :core:network (OkHttp SPKI)
```

### Detailed Module Profiles

#### 1. `:feature:brain` (The Intelligent Router)
- **Role**: Coordinates the 6-layer intent pipeline.
- **Implementation**: Normalizes text inputs to NFC, parses command prefixes (`!local`, `!cloud`), executes `ConstitutionalGuardrails` to filter 13 harmful patterns, and streams inputs down to classified intents.
- **Cascade Fallback Logic**:
  - `Layer 0`: Regex-Keyword-Pattern-ML Intent Classifier. Short-circuits to rule or skill immediately.
  - `Layer 1`: declarative `@Rule` annotated trigger-state handlers (e.g., toggle torch, adjust volume) bypassing LLM.
  - `Layer 2`: Hilt multibound `@Handles` skills executing local permission-checked commands.
  - `Layer 3`: Local LLM layer (JNI `llama.cpp` using Q4_K_M quantization).
  - `Layer 4`: Cloud LLM Gateway with real-time PII scrubbing (masks Aadhaar, phone numbers, OTPs, emails).
  - `Layer 5`: Plan-Execute-Observe loop using tool parameters (`RecallMemory`, `FetchUrl`, `WebSearch`, `Calculate`).

#### 2. `:feature:memory` (The Hippocampus)
- **Role**: Encrypted long-term memory engine and localized Knowledge Graph.
- **Implementation**: Powered by Room + SQLCipher (AES-256-CBC). Contains tables for episodic events, semantic facts, emotional associations, and knowledge graph edges.
- **Algorithmic Specialization**: Implements the **Ebbinghaus Forgetting Curve**. During "Dream Mode" (triggered via WorkManager when charging and idle), unused memories decay in confidence while frequently accessed memories reinforce. FTS5 indexes text for instant BM25 matching, which is combined with INT8-quantized semantic vector search via Reciprocal Rank Fusion (RRF).

#### 3. `:feature:voice` (The Acoustic Stack)
- **Role**: Real-time spoken dialogue, hotword parsing, and neural synthesis.
- **Implementation**:
  - **Wake Word**: 3-tier cascade: Hardware DSP (`SoundTriggerManager` for sub-0.5% battery drain) -> TFLite 소프트웨어 Confirmation (`openWakeWord`) -> Pipeline wake.
  - **STT**: `Whisper.cpp` JNI with looking-ahead delta context streaming to avoid re-generating full-text tokens on partial audio chunks.
  - **TTS**: High-prosody neural synthesis mapping 9 active emotions (Happy, Calm, Urgent, Witty, Thoughtful, etc.) modulating base speech rate, volume, and conversational fillers ("hmm", "give me a sec").
  - **Speaker ID**: Cosine similarity check over enrolled MFCC + ECAPA-TDNN vector footprints to isolate owner commands.

#### 4. `:feature:vision` (NOUS Eyes)
- **Role**: Real-time object recognition, spatial mapping, high-density OCR, and visual episodic memory.
- **Implementation**: On-device SSD TFLite models cycle in a ring buffer capturing context frames. Frame differencing gates inference when movement is stagnant (saving 70% camera battery). Includes dedicated UPI QR parser for instant transaction intent generation. Maps captured entities ("person", "auto-rickshaw", "laptop") directly into Module 3's Knowledge Graph nodes.

#### 5. `:feature:agent` & `:feature:system` (System Execution)
- **Role**: Clicks, keystrokes, and system actions.
- **Implementation**: Two-tier approach: LADB (Local Android Debug Bridge running over local loopback mDNS) for rapid UI interactions + `AccessibilityService` as a standard fallback. Features `BankingAppGuard` (strict non-overridable blacklist preventing any automation on HDFC, SBI, Google Pay, Zerodha, etc.), 19 reversible hardware state handlers, and a Transactional Undo Stack that survives app crashes.

---

## 3. Backend Server Architecture (Distributed Cloud Node)

To support heavy computation, advanced tool execution, and secure data orchestration, the NOUS Backend is designed as an asynchronous, containerized microservices stack.

```
                         +-----------------------+
                         |     API GATEWAY       |
                         |   (Envoy Proxy)       |
                         +-----------+-----------+
                                     |
             +-----------------------+-----------------------+
             | gRPC / WebSocket                              | gRPC
             v                                               v
+------------+------------+                      +-----------+-----------+
|    STREAMING ORCHESTRATOR|                      |    COGNITIVE KNOWLEDGE|
|       (Go / Rust)       |                      |    GRAPH ENGINE       |
+------------+------------+                      | (Python FastAPI/Neo4j)|
             |                                   +-----------+-----------+
             | AMQP (RabbitMQ)                               |
             v                                               v
+------------+------------+                      +-----------+-----------+
|    WEB EXECUTION WORKER |                      |    VECTOR INDEX       |
|    (Node.js / Puppet)   |                      | (pgvector / Qdrant)   |
+-------------------------+                      +-----------------------+
```

### Critical Backend Microservices

#### 1. Streaming Orchestration Gateway (Go / Rust)
- **Purpose**: Establishes high-throughput, low-latency gRPC and WebSocket tunnels with the Android client.
- **Features**: Processes streaming raw audio and visual feeds, coordinates parallel tasks, and manages token-by-token server-sent event (SSE) distribution to the client.

#### 2. Cognitive Knowledge Graph Engine (Python FastAPI / Neo4j / pgvector)
- **Purpose**: Hosts a powerful distributed Graph database to process massive Graph-RAG queries, semantic node merging, and multi-hop logical deductions.
- **Data Model**: Mirrors the client-side knowledge graph schema (Nodes: Person, Event, Object, Concept; Edges: USES, KNOWS, VISITED, LIKES). 
- **Inference Pipeline**: Takes user embeddings, queries the high-dimensional vector space (`pgvector` or `Qdrant` with 1536-dim embeddings), executes a 2-hop graph traversal to pull relational context, and compiles the consolidated prompt.

#### 3. Secure API Proxy & Token Scrubber
- **Purpose**: Acts as an intermediary between NOUS clients and commercial LLM API endpoints (OpenAI, Anthropic, Gemini, Groq).
- **Security**: Ensures client devices do not store sensitive developer API keys. Performs secondary prompt-injection filtering and PII anonymization in flight.

#### 4. Headless Web Execution Engine (Node.js / Puppeteer-Pool)
- **Purpose**: Executes complex agentic web tasks that are impossible on mobile interfaces (e.g., deep web scraping, flight checkout automation, high-concurrency SaaS interactions).
- **Execution Flow**: Receives structured JSON execution schemas from the client's Layer 5 Agent, spins up a secure, ephemeral Docker container running Puppeteer, performs the clicks/form-submissions, records the visual trace, and returns the result state.

---

## 4. Real-time Communication Protocols

Communication between the Android client and the Backend node uses highly optimized, low-latency messaging frameworks over TLS 1.3.

### 1. gRPC Bidirectional Audio Streaming Schema

```protobuf
syntax = "proto3";

package nous.comms.v1;

service VoiceStreamService {
  rpc StreamDialogue(stream DialogueRequest) returns (stream DialogueResponse);
}

message DialogueRequest {
  string correlation_id = 1;
  oneof payload {
    AudioChunk audio_chunk = 2;
    string text_chunk = 3;
    ClientContext context = 4;
  }
}

message AudioChunk {
  bytes raw_pcm = 1;         // 16kHz, 16-bit, mono PCM
  int64 sequence_number = 2;
}

message ClientContext {
  string user_id = 1;
  string active_persona = 2;
  float latitude = 3;
  float longitude = 4;
  float battery_level = 5;
}

message DialogueResponse {
  string correlation_id = 1;
  oneof payload {
    string text_token = 2;   // Streaming text tokens
    bytes audio_token = 3;   // Compressed Opus audio frame for TTS
    SystemCommand command = 4; // Commands to execute on device
  }
}

message SystemCommand {
  string command_type = 1;   // "SET_BRIGHTNESS", "DIAL_PHONE", "AUTOMATE_UI"
  string parameters_json = 2;
}
```

### 2. WebSocket Orchestration Flow (JSON-RPC 2.0)

For visual processing, screenshot streams, and automation control, a WebSocket channel is opened.

#### Visual Frame Submission:
```json
{
  "jsonrpc": "2.0",
  "method": "vision.process_frame",
  "params": {
    "correlationId": "c4d116bc-04be-41f2-98ba-d2f0991c0e3a",
    "timestamp": 1781523529000,
    "imageJpegBase64": "/9j/4AAQSkZJRg...",
    "tasks": ["OCR", "OBJECT_DETECTION"]
  },
  "id": 101
}
```

#### Automation Execution Command:
```json
{
  "jsonrpc": "2.0",
  "method": "agent.execute_web_task",
  "params": {
    "correlationId": "c4d116bc-04be-41f2-98ba-d2f0991c0e3a",
    "targetUrl": "https://irctc.co.in",
    "instructions": [
      {"action": "type", "selector": "#username", "value": "[SECURE_VAULT_ITEM]"},
      {"action": "click", "selector": "#search_btn"}
    ]
  },
  "id": 102
}
```

---

## 5. Zero-Knowledge Security & Cryptographic Handshakes

Since the user's memory is private, we must guarantee that the Backend server can **NEVER** inspect the synchronized knowledge graph, semantic facts, or conversation transcripts. This is achieved through a **Zero-Knowledge End-to-End Encrypted (E2EE) Sync** protocol.

```
       FRONTEND CLIENT                                    BACKEND SERVER
              |                                                  |
1. Generate Local MasterKey                                      |
   (via Android Keystore TEE)                                    |
              |                                                  |
2. Derive SyncKey via HKDF-SHA256                                |
   (using MasterKey + salt)                                      |
              |                                                  |
3. Encrypt SQLite/Memory dump                                    |
   using AES-256-GCM + SyncKey                                   |
              |                                                  |
4. Generate Encrypted Payload + Auth Tag                         |
              |--------- POST /api/v1/sync/backup -------------->|
              |   (Includes encrypted payload & metadata,        |
              |    but NO cryptographic key)                     | 5. Store blind blob in DB
              |                                                  |    (Unreadable ciphertext)
              |                                                  |
```

### Cryptographic Rules:
1. **Zero-Knowledge Key Derivation**: The Android client creates an AES-256 master key in `AndroidKeystore`. Utilizing `HKDF-SHA256` (HMAC-based Key Derivation Function), it derives a separate `SyncKey` and `StorageKey`. The `SyncKey` never leaves the client device.
2. **Deterministic Encryption for Graph Nodes**: To allow search indexing on the encrypted backend database without exposing plaintext, property fields (such as entity names or tags) utilize **Deterministic Authenticated Encryption** (SIV-mode AES). The backend can compile index mappings ("Node A" has the same encrypted label as "Node B" -> draw edge) without knowing that "Node A" actually represents "Mom".
3. **Payload Compression and Chunking**: Database dumps are compressed using GZIP, divided into 4MB chunks, and encrypted with AES-GCM (with unique 96-bit nonces). The server only sees anonymous binary blobs linked to a cryptographically hashed user ID (`SHA-256(userId + serverSalt)`).
4. **Post-Quantum Forward Secrecy**: Ephemeral key exchanges over gRPC gree-light with **ML-KEM (Kyber-1024)** coupled with classical **ECDH (X25519)**, ensuring data intercepted in transit is resistant to decryption by future quantum computers.

---

## 6. Actionable Implementation Roadmap

The implementation is broken down into structured phases designed to scale from the current functional on-device codebase to the complete frontend-backend system.

### Phase 1: Establish gRPC/WebSocket Networking Foundations (Current Focus)
- Set up bidirectional OkHttp streaming clients with certificate pinning.
- Deploy the Dockerized Go API Gateway with basic authentication.
- Test connection latency, keeping heartbeat loops under 250ms.

### Phase 2: Implement the E2EE Zero-Knowledge Sync Stack
- Incorporate HKDF-SHA256 key generation inside `:core:security`.
- Build the SQLite encryption engine using SQLCipher.
- Deploy pgvector schemas on the backend, testing deterministic search queries over encrypted data.

### Phase 3: Wire Voice and Vision Pipelines
- Initialize Whisper.cpp JNI and test on low-end devices.
- Connect SSD TFLite visual frame buffer and pipe objects to the local Room knowledge graph.
- Implement streaming audio to Go gRPC endpoint with fallback to local speech engines when network speeds degrade.

### Phase 4: Deploy Headless Agentic Automation
- Develop Node.js Puppeteer workers.
- Integrate Layer 5 Orchestrator to generate standard JSON-RPC web execution instructions.
- Test hands-free WhatsApp and system control via wireless LADB pairing on Android 11+.

---

### Verification and Compliance
All implementations will be incrementally verified via local JVM unit tests (JUnit 5 + MockK) and verified for build stability using `compile_applet` commands. No code will be written without strict edge-to-edge safety padding and standard material tokens, keeping code quality to the absolute highest standards.
