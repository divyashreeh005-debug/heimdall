# ADR-007 — Podman pods, mTLS, JWT, GitHub HMAC

- Status: accepted
- Date: 2026-09-12

## Context

Deployment is Podman pods, not Kubernetes. Service-to-service auth, user auth, and webhook authenticity are different problems.

## Decision

- Four pods: control, data, worker, llm. Quadlet units.
- **mTLS** between control, worker, data (Postgres TLS), and llm. Internal CA.
- **JWT RS256** for Console users. Short access TTL, refresh, roles.
- **GitHub HMAC** (`X-Hub-Signature-256`) + timestamp replay window + `X-GitHub-Delivery` idempotency for webhooks. Webhooks do not use JWT.
- GitHub App PEM is a Podman secret. No long-lived PATs as the design center.

## Consequences

- Ingress terminates public TLS; internal traffic is mTLS.
- Compromise of the Console JWT key does not allow forging GitHub webhooks.
- Operators must manage an internal CA and Quadlet secrets.
