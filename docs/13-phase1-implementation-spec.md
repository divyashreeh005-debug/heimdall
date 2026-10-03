# 13 — Phase 1 implementation spec (skeleton state)

This document expands the phase-by-phase plan into what actually exists in
this repository: which classes implement which responsibility, what is real
versus stubbed, and what remains before each phase's exit criteria
(docs/12-roadmap.md) are genuinely met. Read alongside docs/01 through
docs/12 — this file does not repeat their contracts, only maps them to code.

Status legend: **done** = real logic matching the docs; **wired** = the
seam exists and compiles/runs, but the logic inside is a placeholder;
**missing** = not started.

## Phase 1.1 — Foundations

| Piece | Status | Where |
| --- | --- | --- |
| Schema | done | `src/main/resources/db/migration/V1__init.sql` — every table from docs/02-domain-model.md's "Logical tables", plus the four read-model views |
| Job queue (`SKIP LOCKED`) | done | `jobs/JobQueueService.java` — the exact SQL from docs/02-domain-model.md |
| JWT issue/verify | done | `identity/JwtService.java`, `identity/JwtAuthFilter.java` |
| RBAC roles | done | `identity/Role.java` (admin/reviewer/viewer), enforced via Spring Security authorities |
| Two process roles, one artifact | done | `HeimdallApplication.java` + `application-worker.yml`; `orchestrator/OrchestratorScheduler.java` is `@Profile("worker")` only |
| mTLS between pods | **missing** | Not implementable inside a Java skeleton — belongs in `docker-compose.yml`/Quadlet TLS termination and cert provisioning; `docs/07-security.md` has the target shape |
| Org/App-installation bootstrap | **missing** | `ops/dev-seed.sql` stands in for real GitHub App install flow |

**To close this phase for real:** add the GitHub App installation callback
that creates the `organizations` row and exchanges installation tokens, and
put actual TLS certs in front of the four services.

## Phase 1.2 — Ingestion

| Piece | Status | Where |
| --- | --- | --- |
| HMAC verification | done | `ingestion/HmacVerifier.java` — constant-time compare, unit-tested in `HmacVerifierTest` |
| Delivery-id dedup | done | `ingestion/WebhookIngestionService.recordDelivery` + `webhook_deliveries` table |
| Idempotent enqueue | done | `insertAnalysisRunIfAbsent` relies on the `(repository_id, pr_number, head_sha)` unique constraint |
| Git provider SPI | **wired** | `gitprovider/` package exists but is empty — docs/05-git-provider-spi.md's `GitProvider` interface and the GitHub adapter still need writing; `WebhookIngestionService` currently talks to the DB directly instead of through the SPI |

**To close this phase for real:** extract a `GitProvider` interface (per
docs/05) and move the repository/PR upsert logic behind it, so GitLab/
Bitbucket adapters in Phase 2 don't touch `ingestion` at all.

## Phase 1.3 — Orchestration + workspace

| Piece | Status | Where |
| --- | --- | --- |
| DAG runner | done | `orchestrator/OrchestratorService.runJob` — Analyze → Find → Verify → Score → Explain → Decision → Notify → Audit, in order |
| Clone + diff | done | `workspace/AnalysisWorkspaceService.prepare` (shells out to `git`) |
| File-hash cache (incremental) | **wired** | `AnalysisWorkspaceService.unchangedSince` exists but `OrchestratorService` doesn't call it yet — `analysis_runs.parent_run_id` is in the schema but never populated |
| Sandbox (no network) | **missing** | No sandboxed test execution; `VerdictInputs.requiredTestsFailed()` is hardcoded `false` in `OrchestratorService` |
| Ollama reachability | done | `agents/OllamaClient.java` calls `/api/generate` and `/api/embeddings` |

**To close this phase for real:** run the diff'd repo's test suite inside a
network-disabled rootless Podman container and feed the pass/fail count
into `VerdictInputs`; wire `parent_run_id` lookup so re-pushes skip
unchanged files.

## Phase 1.4 — Skills + agents

| Piece | Status | Where |
| --- | --- | --- |
| Language skill loader | done | `skills/SkillLoader.java` + `src/main/resources/skills/language/*.yaml` (java, python, typescript bundled; go/sql/terraform fall back to a generic default prompt) |
| Bug / Security / Test / Impact agents | done | `agents/BugAgent.java`, `SecurityAgent.java`, `TestAgent.java`, `ImpactAgent.java`, sharing `AbstractLlmAgent` |
| Project Skill (draft onboarding) | **missing** | `skills/ProjectSkill.java` is a bare record; no loader, no onboarding flow, no `project_skills` writes |
| Untrusted-input delimiting | done | `common/UntrustedText.java`, used in `AbstractLlmAgent.analyze` |

**To close this phase for real:** build the onboarding job that runs once
per repository, produces a `draft` `ProjectSkill` from a language/dependency
scan, and persists it — see docs/04-skills-system.md.

## Phase 1.5 — Verification + scoring

| Piece | Status | Where |
| --- | --- | --- |
| Verification Guard | **wired** | `verification/VerificationGuard.java` implements exactly one tool-first check (SQL-concatenation pattern match) — enough to satisfy the Phase 1 exit criteria's worked example, not the full claim-class matrix in docs/03-agent-contracts.md |
| Verdict Engine | done | `verdict/VerdictEngine.java` — transcribes the full weight table from docs/06-risk-and-decision.md; `VerdictEngineTest` reproduces the doc's "UPI retry" worked example |
| Decision tree | done | `decision/DecisionService.java` — the exact `if/else` chain from docs/06 |
| Policy document | **wired** | `policy/PolicyDocument.defaultPolicy()` hardcodes docs/policies/example-policy.yaml's values; `PolicyLoader` doesn't yet parse the `policies.document` JSONB column, so per-org overrides don't take effect |

**To close this phase for real:** add verification methods for the other
claim classes agents can emit (secrets, dependency CVEs, test-result
corroboration), and make `PolicyLoader` parse the stored YAML/JSON instead
of always returning the default.

## Phase 1.6 — GitHub feedback loop

| Piece | Status | Where |
| --- | --- | --- |
| `heimdall/verdict` Check | **wired** | `notification/GitHubCheckPublisher.java` posts the right payload, but there is no installation-token exchange — `OrchestratorService.publishToGitHub` only fires if `GITHUB_DEV_TOKEN` is set |
| Summary comment | **wired** | Same caveat — `notification/CommentService.java` |
| Audit events | done | `audit/AuditService.java`, called at every DAG stage in `OrchestratorService` |

**To close this phase for real:** implement the GitHub App JWT → 
installation-token exchange (sign a short-lived JWT with the App's private
key, `POST /app/installations/{id}/access_tokens`) and remove the
`GITHUB_DEV_TOKEN` shortcut.

## Phase 1.7 — Console

| Piece | Status | Where |
| --- | --- | --- |
| Login | **wired** | `console/src/pages/Login.tsx` + `consoleapi/AuthController.devLogin` — email lookup, not real GitHub OAuth |
| PR list | done | `PrList.tsx` reads `GET /api/v1/repositories/{id}/pull-requests` → `pr_overview` view |
| PR detail (findings, risk, audit) | done | `PrDetail.tsx` reads `GET /api/v1/pull-requests/{id}` |
| Risk breakdown chart | **missing** | `api.riskEvolution` is wired in the client but no page renders it yet (Phase 1b: "Risk evolution chart") |

## Phase 1 exit criteria — current gap

docs/12-roadmap.md's exit check requires an end-to-end run against a real
sample repo. Given the gaps above, the two blockers are:
1. No GitHub App installation-token exchange, so `GET /webhooks/github` can
   receive and enqueue, but the Check/comment never actually posts without
   `GITHUB_DEV_TOKEN` set by hand.
2. `VerdictInputs.requiredTestsFailed()` is hardcoded, so the "tests failed
   → BLOCK, tests pass after fix → MERGE" half of the worked example can't
   be observed end-to-end yet — only the scoring math itself is verified
   (see `VerdictEngineTest`).

Everything else on the Phase 1 must-have list has working code behind it.

## Phase 1b — status

| Feature | Status |
| --- | --- |
| Reviewer recommendation (CODEOWNERS + blame) | missing |
| Risk evolution chart | API wired (`riskEvolution`), no UI |
| Impact graph + heatmap | `impact_edges` table exists, nothing writes to it |
| AI test suggestions | missing |
| Trivy/OSV on changed lockfiles | missing |
| Policy YAML editor in Console | missing — `PolicyLoader` needs the JSONB-parsing work above first |
| Eval summary API + panel | `eval_labels` table exists, no harness or endpoint |

## Suggested build order from here

1. GitHub App installation-token exchange (unblocks 1.6 end-to-end).
2. Sandbox test execution (unblocks the BLOCK→MERGE half of the exit check).
3. `GitProvider` SPI extraction (cheap now, expensive to retrofit once
   ingestion logic grows).
4. `PolicyLoader` real parsing (unblocks the Phase 1b policy editor).
5. Everything else in Phase 1b, in the order listed in docs/12-roadmap.md.
