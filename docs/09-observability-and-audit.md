# 09 — Observability and audit

A multi-agent system without traces is not operable. This document defines telemetry, SLOs, and the audit trail.

## Correlation

Every request and job carries:

| Id | Where |
| --- | --- |
| `trace_id` | W3C traceparent from API; generated if webhook |
| `analysis_run_id` | Domain |
| `org_id` | Domain |
| `delivery_id` | GitHub webhook |

Logs, spans, and audit events include these fields.

## OpenTelemetry

- SDK in Spring Boot (API + worker).
- Traces: OTLP exporter (collector later; log exporter acceptable in POC).
- Span tree (worker): `job.claim` → `workspace.prepare` → `skills.load` → `agent.bug` / `agent.security` / `agent.test` / `agent.impact` → `verification` → `verdict` → `decision` → `publish`.
- Attributes: `agent.id`, `skill.ids`, `model.id`, `pr.number`, `head_sha`, `tokens.prompt`, `tokens.completion`.
- **Do not** put file contents or secrets in span attributes.

Metrics (minimum):

| Metric | Type |
| --- | --- |
| `heimdall.analysis.running` | gauge |
| `heimdall.analysis.duration` | histogram (seconds) |
| `heimdall.analysis.outcome` | counter (succeeded/failed) |
| `heimdall.verdict` | counter (MERGE/REVIEW/BLOCK) |
| `heimdall.findings` | counter (status, agent, severity) |
| `heimdall.llm.tokens` | counter |
| `heimdall.github.ratelimit_remaining` | gauge |
| `heimdall.job.queue_wait` | histogram |

Health: `/actuator/health` liveness and readiness (DB, disk). Do not fail readiness if Ollama is cold-loading; worker readiness **does** require Ollama once the llm pod has been up > N minutes.

## SLOs (targets, not contracts)

| SLO | Target | Notes |
| --- | --- | --- |
| Analysis p95, medium PR (≤20 files, GPU 14B) | < 15 minutes | Budget in policy |
| Analysis p95, small PR (≤5 files) | < 8 minutes | |
| Webhook ingest p99 | < 500 ms to 202 | Enqueue only |
| Console API p95 | < 300 ms | Read models |
| Failed runs | < 5% excluding GitHub 429 | |

Error budget policy: if p95 exceeds SLO for 24h, disable Bug LLM (tools-only) via policy flag `llm.bug.enabled` rather than silently dropping verification.

## Structured logging

JSON logs. Levels:

- info: state transitions
- warn: retries, UNVERIFIED critical
- error: run failed

Forbidden at info: PR body, diffs, prompts, tokens of secrets.

Debug profile `llm.log_prompts=true` is **dev only**.

## Audit trail

Table `audit_events`, append-only. Application role has INSERT + SELECT, no UPDATE/DELETE.

Event types:

| Type | When |
| --- | --- |
| `webhook.accepted` | HMAC ok |
| `webhook.rejected` | HMAC fail |
| `analysis.queued` | job created |
| `analysis.started` | lease |
| `analysis.completed` | terminal |
| `finding.verified` | guard |
| `verdict.computed` | engine |
| `decision.published` | Check + comment |
| `feedback.labeled` | human |
| `policy.updated` | admin |
| `skill.project.updated` | admin |
| `suppression.created` | reviewer/admin |

Payload: ids and hashes, not blobs. Retention: 400 days default (policy).

Console **Audit** tab is this stream filtered by `pull_request_id` or `org_id`.

## Alerting (later wiring, defined now)

- Job lease expired > 3 times for same run
- Ollama down 5 minutes
- Postgres disk > 80%
- Check publish failures
- Spike in BLOCK rate (optional, org-level)

## Privacy

Telemetry is operator-local in the POC (no SaaS APM required). If an operator exports OTLP off-box, they accept code-adjacent metadata (paths, not contents).
