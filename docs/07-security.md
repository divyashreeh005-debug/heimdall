# 07 — Security architecture

Threat model and controls for the POC. Code is later; these requirements are binding.

Related: [adr/0007-podman-mtls-jwt-hmac.md](adr/0007-podman-mtls-jwt-hmac.md), [adr/0009-advisory-check-no-automerge.md](adr/0009-advisory-check-no-automerge.md).

## Assets

- Source code of customer repositories (clone on worker)
- GitHub App PEM and webhook secret
- JWT signing key
- PostgreSQL contents (findings, audit, diffs metadata)
- Ollama prompts (code excerpts)
- Console sessions

## Trust zones

| Zone | Members | Trust |
| --- | --- | --- |
| Public internet | Browsers, GitHub webhook emitters | Untrusted |
| Edge | Ingress TLS | Terminates public TLS |
| Control | API, Console static | Authenticated users + verified webhooks |
| Data | PostgreSQL | Highest; TLS + network policy |
| Worker | Orchestrator | Trusted code, untrusted inputs |
| Sandbox | Nested rootless container | **Hostile** |
| LLM | Ollama | Trusted process, untrusted prompts |

## STRIDE summary

| Threat | Example | Mitigation |
| --- | --- | --- |
| Spoofing | Fake GitHub webhook | HMAC, unique delivery id |
| Tampering | Mutate job mid-run | mTLS, DB TLS, signed Check from API credentials |
| Repudiation | Silent BLOCK override | Append-only audit (no override in POC) |
| Information disclosure | Secret in LLM prompt or logs | Gitleaks first, redaction, fingerprint-only storage |
| Denial of service | Huge diff / prompt bomb | Size caps, token budgets, job timeouts, clone depth |
| Elevation | PR edits `.heimdall/SKILL.md` to jailbreak | Repo overlays untrusted; skills from DB/product git |

## Authentication and authorization

### Console users — JWT RS256

- Access token TTL: 15 minutes. Refresh: 8 hours, rotating, stored hashed.
- Claims: `sub`, `org_id`, `role` (`admin` | `reviewer` | `viewer`), `iss`, `exp`.
- Algorithm: RS256. Private key in Podman secret. Deny `alg=none`.
- Transport: `Authorization: Bearer`. Cookie optional later (`Secure; HttpOnly; SameSite=Lax`).
- POC identity: local users in PostgreSQL (password argon2id) or a single bootstrap admin. OIDC later.

RBAC:

| Role | Capabilities |
| --- | --- |
| viewer | Read PRs, findings, audit of own org |
| reviewer | viewer + label findings, dismiss (non-secret) |
| admin | reviewer + policy, Project Skill, users, GitHub install |

Webhooks **do not** use JWT.

### Service-to-service — mTLS

- Internal CA (step-ca or openssl).
- SAN: `api.heimdall.internal`, `worker.heimdall.internal`, `ollama.heimdall.internal`, `postgres.heimdall.internal`.
- Spring Boot SSL bundles; Postgres `sslmode=verify-full`.
- Ollama behind a TLS proxy or stunnel in the llm pod if Ollama itself lacks client-cert support. Documented default: nginx sidecar in `heimdall-llm` requiring client certs, proxy to `127.0.0.1:11434`.

### GitHub

- App PEM, installation tokens.
- Webhook: `X-Hub-Signature-256` over raw body, constant-time compare.
- Replay: persist `X-GitHub-Delivery`; duplicates 202.
- Body cap 5 MB.

## Sandbox (untrusted code)

Running PR tests is a supply-chain feature.

Baseline:

- Rootless Podman nested (or `podman run --userns=auto` from worker).
- Non-root user inside.
- **No network** by default. Project Skill may allowlist CIDRs/DNS (package registries) — admin-only, audited.
- Caps: CPU, memory, PIDs, wall clock (default 10 minutes).
- Read-only root FS; writable `/work` for clone.
- Seccomp default; no `--privileged`; no Docker socket.
- Drop `CAP_NET_RAW`, etc.
- After run: destroy container; retain logs without secrets (redact `gitleaks` lines).

Clone credentials: mount a tmpfs token file with mode 0400, not env, not argv.

## Prompt injection

1. Instruction hierarchy: constitution > Language Skill > Project Skill > **never** untrusted.
2. Delimit untrusted content in `<untrusted>` tags.
3. Tool allowlist per agent; no generic “run shell” tool for the LLM. Shell is orchestrator/sandbox only.
4. Ignore customer-repo `SKILL.md` unless policy overlay is enabled (POC: disabled).
5. Treat “ignore previous instructions” in a diff as data.
6. Verification Guard does not take proposer chain-of-thought.

## Secrets

- Gitleaks runs **before** any LLM file excerpt selection. Hits are redacted from excerpts (`***REDACTED_SECRET***`).
- Database stores `secret_fingerprint` = HMAC-SHA256(org_pepper, secret) + location. Pepper in Podman secret. Raw secret never persisted.
- Logs: structured JSON with denylist keys (`token`, `password`, `authorization`).
- `.env`, `*.pem`, `id_rsa` are not sent to Ollama.

## Data residency

POC inference is local Ollama. Code excerpts do not leave the operator network. GitHub still receives Check text and comments (titles of findings). Do not put secret snippets in comments.

## Merge safety

- No GitHub merge API in POC.
- Check failure is the gate if the org requires `heimdall/verdict`.
- Dual-control BLOCK override is later: two `admin` or `admin+reviewer`, reason required, audit event `decision.override`.

## Supply chain of Heimdall itself

- Pin base images by digest in Quadlet (implementation phase).
- SBOM of the API image later.
- Do not mount the worker’s Podman socket into the sandbox.

## Abuse and DoS

| Limit | Default |
| --- | --- |
| Concurrent runs per org | 2 |
| Diff files processed by LLM | 80 (largest source files by change, skip generated) |
| Excerpt lines per file | 400 |
| Prompt tokens per agent | policy |
| Webhook rate | 30/min/installation then 429 |

## Logging and privacy

- `analysis_run_id` and `trace_id` on every log line.
- PR bodies not logged. Diff not logged at info.
- Audit payloads store finding ids, not file blobs.

## Residual risks

- A 14B local model can still leak excerpted code to logs if operators raise log level to debug prompts — forbid debug prompt logging in production profile.
- Nested Podman may be unavailable on some hosts; fallback is a second worker pod with tighter network (`--network=none`) rather than “run tests on the worker itself.”
- GitHub App compromise allows repo read; PEM protection is the highest-priority secret.
