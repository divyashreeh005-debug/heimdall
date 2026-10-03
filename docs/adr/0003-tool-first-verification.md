# ADR-003 — Tool-first verification; LLM cannot VERIFIED-critical alone

- Status: accepted
- Date: 2026-09-12

## Context

AI reviewers hallucinate. The product’s research claim is that an independent verification layer improves precision. If the verifier is the same class of model as the proposer, the claim collapses.

## Decision

- A Verification Guard runs after proposing agents and cannot see their chain-of-thought; it sees claims + workspace evidence.
- Statuses: `VERIFIED`, `UNVERIFIED`, `REJECTED`, `FALSE_POSITIVE`.
- `VERIFIED` + `severity=critical` requires at least one non-LLM evidence kind (AST, static analysis, test, dependency, reproduction).
- LLM-only agreement may at most yield `UNVERIFIED`.
- Tools win conflicts: parameterized query AST beats an LLM “SQL injection” claim.

## Consequences

- Critical BLOCK cannot be justified by prose.
- Verification may be slower; tool outputs must live in the shared workspace.
- Eval harness compares precision with the guard on vs off.
