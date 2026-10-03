# 12 — Roadmap (MVP vs later)

This phase is **architecture and contracts only**. Coding follows. Do not treat the 50-feature essay as the MVP.

## Phase 0 — this repository (done when docs land)

- Vision, C4, domain, agents, skills, SPI, risk, security, Podman, observability, eval, Console IA
- ADRs 001–010
- OpenAPI + AsyncAPI
- Example policy and skill packs

## Phase 1 — coding MVP (~1.5 months)

Must-have:

- GitHub App + webhook ingest (HMAC, idempotency)
- PostgreSQL schema + job queue
- Orchestrator DAG + shared workspace (clone, diff)
- Language Skills loader + Project Skill (draft onboarding)
- Agents: Bug, Security, Test, Impact
- Verification Guard (tool-first)
- Verdict Engine + Decision
- GitHub Check `heimdall/verdict` + summary comment
- Console: login, PR list, PR detail (summary, findings, risk, audit)
- Incremental re-run on new `head_sha` (file-hash cache)
- Audit events
- Sandbox for tests (no network)
- Ollama 14B/7B + embeddings
- mTLS + JWT as specified

## Phase 1b — if time remains

- Reviewer recommendation (CODEOWNERS + blame)
- Risk evolution chart
- Impact graph + heatmap
- AI test suggestions (not auto-committed)
- Trivy/OSV on changed lockfiles
- Policy YAML editor in Console
- Eval summary API + panel on fixtures

## Phase 2 — explicitly later

Do not advertise as MVP:

- GitLab / Bitbucket / Gitea adapters
- Redis
- Temporal extraction
- Auto-merge
- Dual-control override
- Pending-PR interaction analysis
- Counterfactual UI
- Multi-repo portfolio dashboard
- Historical MR learning / fine-tune
- OPA/Rego
- OIDC
- CloudNativePG HA
- SARIF upload to GitHub Code Scanning
- Repo-overlay skills
- Performance agent, architecture agent, documentation agent as new LLM roles
- Slack/Teams
- Monorepo sparse checkout
- Human minting of critical VERIFIED without tools

## Feature map from the original essay

| Essay item | Phase |
| --- | --- |
| AI code review | 1 Bug agent |
| Security analysis | 1 |
| Test analysis | 1 |
| AI test generation | 1b |
| Change impact | 1 |
| Dependency graph | 1 (import graph) / 2 richer |
| Merge risk score | 1 Verdict Engine |
| MERGE/REVIEW/BLOCK | 1 |
| Hallucination detection | 1 Guard |
| Finding verification | 1 |
| False-positive filtering | 1 |
| Evidence-based findings | 1 |
| Confidence score | 1 |
| Verification statuses | 1 |
| Smart reviewers | 1b |
| Summarization | 1 (comment) |
| Risk explanation | 1 |
| Automatic comments | 1 |
| Incremental re-analysis | 1 |
| Risk evolution | 1b |
| Regression detection | 1 (tests) |
| Breaking change detection | 1 Impact + API files / 2 stronger |
| Code quality | 1 Bug |
| Performance risk | 2 |
| Dependency risk | 1b |
| Secret detection | 1 |
| Policy enforcement | 1 hard rules / 1b editor |
| MR size risk | 1 |
| DB / API / config impact | 1 detectors (shallow) / 2 deep |
| Multi-agent + parallel | 1 |
| Evidence sharing | 1 workspace |
| Human-in-the-loop | 1 inbox (1b polish) |
| Merge gate | 1 Check |
| Repository-aware | 1 Project Skill |
| Historical MR analysis | 2 |
| Developer feedback | 1 labels |
| Dashboard | 1 |
| Heatmap / impact graph | 1b |
| MR comparison | 1b evolution |
| Multiple repositories | 1 data model / 2 portfolio UX |
| Webhook | 1 GitHub |
| CI/CD | 1 Check, not a GitHub Actions marketplace MVP |
| Audit trail | 1 |
| Explainable decision | 1 |
| Counterfactual | 2 |
| Pending MR interaction | 2 |

## Suggested additions (keep)

Worth more than extra agents — already in architecture:

1. GitHub required Check as the real gate
2. Finding fingerprints + suppression
3. Onboarding → Project Skill
4. Eval harness
5. SARIF interop (store now, upload later)
6. Sandbox policy
7. Model router + budgets / SLOs
8. Verification inbox
9. Architecture fitness via Project Skill
10. SPDX/OSV dependency risk (1b)

## Exit criteria for Phase 1

- A sample GitHub repo with a concat-SQL PR gets BLOCK + VERIFIED; a parameterized-SQL false AI claim is REJECTED and excluded from score.
- Console shows breakdown and audit for that run.
- Re-push of a fix produces a new run and a lower score.
- No raw secrets in DB, logs, or comments.
