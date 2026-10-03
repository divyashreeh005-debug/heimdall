# ADR-006 — Ollama Qwen2.5-Coder 14B + embeddings

- Status: accepted
- Date: 2026-09-12

## Context

The POC must run LLMs locally. Hardware varies. Code quality and context length matter more than chat fluency.

## Decision

- Inference: Ollama in `heimdall-llm` pod, mTLS from worker.
- Default model: **Qwen2.5-Coder 14B** Q4/Q5.
- Fallback by VRAM: 7B on ≤8 GB; 32B or Qwen3-Coder if available on ≥24 GB.
- Embeddings: `nomic-embed-text` for impact chunk search.
- Temperature 0–0.2 for review/verify. Summarize may use 7B.
- Documented model fallback: DeepSeek-Coder-V2-Lite if Java/TS quality is poor.
- Secrets, `.env`, and Gitleaks hits are never sent to the model.

## Consequences

- No cloud LLM vendor in the POC; data residency is local.
- Quality is bounded by local GPU; 7B is explicitly weaker on verification.
- Model ids are recorded on AnalysisRun.
