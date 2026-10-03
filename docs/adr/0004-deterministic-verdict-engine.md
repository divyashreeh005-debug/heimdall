# ADR-004 — Deterministic Verdict Engine

- Status: accepted
- Date: 2026-09-12

## Context

A single LLM “risk: 73” is not explainable and is not reproducible. Policy owners need weights they can change without retraining.

## Decision

- The Verdict Engine is pure functions over verified findings, test summary, impact, size, dependency, and policy.
- No LLM inside scoring.
- Default weights are documented defaults, not universal truth; stored as versioned policy.
- Decision prose is generated *after* score and verdict; it cannot change them.
- Hard BLOCK rules can override a low score.

## Consequences

- Identical inputs + policy version ⇒ identical score (within integer rounding).
- A/B of weights is a policy experiment, not a model experiment.
- “Counterfactual analysis” later can reuse the same function with hypothetically fixed findings.
