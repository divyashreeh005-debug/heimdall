---
kind: project
id: example-payments
version: 1
status: draft
repository: acme/payments
default_branch: main
languages: [python, typescript]
---

# Build and test

- Language: Python 3.12 service + TypeScript React checkout.
- Install: `pip install -e ".[dev]"` in `backend/`; `pnpm install` in `web/`.
- Test: `pytest -q` from `backend/` (required). `pnpm test` in `web/` (required if `web/**` changes).
- Coverage artifact: `backend/coverage.xml` if pytest-cov runs; optional in POC.

# Sandbox

- Network: `none` (no PyPI/npm at test time; dependencies pre-baked into sandbox image later).
- Working directory for Python tests: `/work/repo/backend`.
- Timeout: 10 minutes.

# Architecture

Layers (do not import upward):

```text
web/ (React)
  → backend/api (FastAPI-equivalent HTTP)
    → backend/services (PaymentService, RefundService, InvoiceService)
      → backend/adapters (db, psp)
```

`PaymentService` is the high-impact hub. Callers: Checkout, Refund, Invoice, Subscription.

# Protected paths

- `deploy/prod/**` — BLOCK on change unless admin policy exception.
- `backend/adapters/psp/**` — high impact; always REVIEW if tests fail.

# Public APIs

- OpenAPI: `backend/openapi.yaml` (if present).
- HTTP routes under `backend/api/routes/`.
- Breaking: remove/rename fields on payment intent responses.

# Owners

- `backend/services/payment*` → `@payment-team`
- `backend/services/refund*` → `@payment-team`
- `web/**` → `@checkout-frontend`
- default → `@backend-team`

CODEOWNERS file in repo is authoritative when it exists; this section is fallback.

# Glossary

- **UPI retry**: replay of a payment charge; must be idempotent on `idempotency_key`.
- **Charge**: PSP debit. Must not run twice for one key.
- **Refund**: only after captured charge.

# Known false positives

- Fingerprint comments in `backend/tests/fixtures/sql_examples.py` — parameterized examples used to train reviewers; SQLi claims there are FALSE_POSITIVE.
- `web/src/vite-env.d.ts` unused symbols — ignore.

# Ignore

- `web/dist/**`
- `**/*.lock` except when dependency agent/Trivy runs
- `backend/.venv/**`
- generated protobuf if added later

# Policy overlays

- Required tests: yes.
- Idempotency on payment retry path: if `retry` / `upi` in PR title or files, Impact band at least medium.
