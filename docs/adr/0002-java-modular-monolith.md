# ADR-002 — Java modular monolith + virtual threads; Temporal later

- Status: accepted
- Date: 2026-09-12

## Context

The originating essay proposed Python, FastAPI, and LangGraph. The implementation stack is Java Spring Boot. Multi-agent work needs parallelism, durability across worker crashes, and a single deployable for a 1.5-month coding phase after this docs POC.

## Decision

- Java 21, Spring Boot 3.x, Spring Modulith.
- One artifact, two process roles: `api` and `worker`.
- Orchestration: PostgreSQL-backed DAG executed with virtual threads.
- LLM adapter: Spring AI primary, LangChain4j documented fallback. Not LangGraph.
- Production path (later): extract the worker DAG into Temporal workflows. Not in POC.

## Consequences

- Orchestration is deterministic application code, not an LLM planner.
- Agent parallelism uses virtual threads over a shared workspace, not Python asyncio.
- Crash recovery is “reclaim expired job lease and rerun idempotent steps,” not Temporal history in the POC.
- Teams cannot drop in LangGraph graphs; agent contracts in `docs/03-agent-contracts.md` are the portability layer.
