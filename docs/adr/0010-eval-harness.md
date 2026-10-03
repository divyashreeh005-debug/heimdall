# ADR-010 — Eval harness as a product feature

- Status: accepted
- Date: 2026-09-12

## Context

The academic contribution is whether verification reduces false positives. If eval lives only in a thesis appendix, the product cannot regress-test the Guard.

## Decision

- `eval_labels` and a Console eval panel are in-scope for the architecture.
- Gold labels come from human feedback (correct / incorrect / irrelevant) plus curated fixtures.
- Metrics: precision, false-positive rate, verified/rejected/unverified mix, with Guard on vs off.
- Implementation of a large labeled corpus is later; the schema and API exist from day one of coding.

## Consequences

- FindingFeedback is not a vanity UI; it feeds eval.
- Model or skill changes should be comparable on the same gold set.
