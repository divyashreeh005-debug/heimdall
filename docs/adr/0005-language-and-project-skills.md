# ADR-005 — Language Skills + Project Skills as versioned artifacts

- Status: accepted
- Date: 2026-09-12

## Context

Prompt folklore does not survive review, diffs, or eval. Repository-aware analysis needs both ecosystem knowledge (how to review Java) and local knowledge (how this service is tested).

## Decision

- **Language Skills**: versioned packs (`java@1.x`) with checklists, tool bindings, verification strategies.
- **Project Skills**: per-repository packs generated at onboarding, human-confirmed, covering build/test, owners, protected paths, glossary, false-positive fingerprints.
- System prompt = platform constitution + selected language skills + project skill.
- Untrusted PR content is data, never skill text.
- AnalysisRun stores skill versions for reproducibility.

## Consequences

- Skills are reviewable markdown/YAML in git (platform) and in the database (project).
- Adding a language does not require a new agent type.
- Onboarding is a product surface, not an afterthought.
