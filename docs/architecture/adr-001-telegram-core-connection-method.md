# Architectural Decision Record: Telegram Core Connection Method

## Status
Approved

## Context & Problem Statement
For the LeadGen Bot platform, we need to evaluate the most reliable, stable, and proxy-aware connection method for managing multiple, warmed-up Telegram accounts simultaneously.
Specifically, the platform requires:
- Dynamic IP-isolation (assigning a dedicated IPv4/IPv6 SOCKS5/HTTP proxy per Telegram session) to prevent multi-account ip-ban chaining (`FEAT-ACC-02`).
- High stability and isolation to prevent JVM crashes caused by native memory or thread issues in Telegram library bindings.
- Horizontal scaling potential as the number of warmed-up accounts and campaigns grows.
- Emulation of human-like behavior (pauses, typing status) and rate-limiting (`FEAT-ACC-04`, `FEAT-ACC-05`).

We compare two core architectural designs for our Telegram integration:
1. **TDLib Java Bindings via JNI/JNA**
2. **External Microservice Bridge** (Node.js/`GramJS` or Python/`Pyrogram` over gRPC/REST)

---

## Evaluation & Comparison

### 1. TDLib Java Bindings via JNI/JNA

* **Pros:**
  - Direct execution within the Java/Spring Boot process; no separate process or microservice to orchestrate.
  - Official/semi-official support: TDLib is maintained by the Telegram team and is highly compliant with official client behaviors.
  - Low latency for simple queries since calls happen in-process.

* **Cons:**
  - **JVM Safety & Stability:** TDLib is written in C++. JNI/JNA bindings are notorious for segmentation faults. A single unhandled exception or memory leak in the native TDLib layer will crash the entire JVM, taking down the Spring Boot backend dashboard, API endpoints, and other independent processes.
  - **Proxy-Aware Connection Management:** Assigning isolated SOCKS5/HTTP proxies *per individual session* is extremely difficult or unsupported natively under a single Java process running a single TDLib instance with shared process-level network properties, unless spawning highly complex native instances with custom thread boundaries.
  - **Horizontal Scaling:** Scaling up requires scaling the JVM itself, which is resource-heavy. We cannot scale the Telegram connection pool independently of the dashboard web application.
  - **Build/Deployment Overhead:** Heavy dependency on platform-specific pre-compiled native binaries (`.so`, `.dll`, `.dylib`). This severely complicates dockerization, cross-platform local development, and CI/CD pipelines.

---

### 2. External Microservice Bridge (Node.js/GramJS or Python/Pyrogram)

* **Pros:**
  - **Process Isolation (JVM Safety):** Running Telegram interactions in a dedicated microservice prevents any third-party library crashes from affecting the main Spring Boot web dashboard. If a Python or Node worker crashes, the JVM remains healthy, and the Spring Boot backend can simply retry the request.
  - **Superior Proxy Support:** Both `Pyrogram` (Python) and `GramJS` (Node.js) have first-class, lightweight, and extremely robust support for configuring dedicated SOCKS5/HTTP proxies on a per-session/per-client object level. Python's async ecosystem (via `Pyrogram` / `Telethon`) handles isolated proxy sessions elegantly with separate event loops or workers.
  - **Horizontal Scaling:** The bridge can be containerized separately and scaled horizontally (e.g., using Kubernetes or Docker Compose) to handle thousands of sessions. The Spring Boot backend interacts with it via light gRPC or REST/WebSocket APIs, facilitating an asynchronous, queue-driven architecture.
  - **Rapid Development & Rich Ecosystem:** Python (`Pyrogram`) and Node.js (`GramJS`) have highly mature, active, and battle-tested developer communities specifically focused on outreach, automation, and session-state management.

* **Cons:**
  - Additional operational overhead: Requires deploying and monitoring a separate service.
  - Slight network overhead for IPC/gRPC/REST communication compared to in-process JNI.

---

## Architectural Decision

We will implement the **External Microservice Bridge (Python with Pyrogram over gRPC/REST)**.

### Rationale:
1. **Uncompromising Stability (JVM Isolation):** Protecting the main Spring Boot JVM from native C++ segmentation faults and memory corruption is a non-negotiable stability requirement.
2. **Robust Multi-Proxy Binding:** Pyrogram provides out-of-the-box, reliable, per-client proxy configuration. It enables us to cleanly bind different SOCKS5/HTTP proxies to each authenticated session, mitigating any risk of IP-ban chaining across the account pool.
3. **Decoupled Scaling:** High-volume campaign dispatches can be scaled separately by adding more Python worker containers without inflating Spring Boot application resource limits.
4. **Maintenance Simplicity:** Eliminates the painful requirement of compiling and maintaining native C++ library binaries across different developer machines and deployment environments.

---

## Transition & Sequence Plan
1. **Spring Boot (Backend):** Functions as the campaign orchestrator, prompt manager (Spring AI), and rate limiter. It persists sessions, proxy mappings, and campaign state in PostgreSQL.
2. **Python Bridge:** Serves as the stateless/state-managed executing agent. Spring Boot will trigger a session onboarding or campaign dispatch request via REST/gRPC to the Python bridge, passing the encrypted session credential, assigned proxy config, and payload.
3. **Queue Mechanism:** Spring Boot will utilize a Redis-backed queue to control flow, spacing, rate-limiting, and human-behavior pauses (120-300 sec) before feeding tasks to the Python Bridge.

---

## Handoff & Next Steps

### Concrete Next Owner:
**Role: BARCAN-TAG-01 - Backend Engineer**

### Target Deliverables:
- Design the API contract (REST or gRPC) between Spring Boot and the Python/Pyrogram Bridge.
- Implement the baseline database schema (using the reserved Flyway migration `V20260726002829871`) for account session states, assigned proxy configurations, and campaign logs.
- Bootstrap a basic Python Pyrogram microservice with SOCKS5/HTTP proxy-aware client initialization tests.
