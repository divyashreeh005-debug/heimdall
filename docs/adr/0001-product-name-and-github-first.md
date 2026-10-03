# ADR-001 — Product name Heimdall; GitHub-first Provider SPI

- Status: accepted
- Date: 2026-09-12
- Supersedes: working name Meridian (docs POC draft)

## Context

The originating essay used “MergeMind” and GitLab as the only host. An earlier architecture draft used “Meridian.” The product must not sound like another AI review bot. The name should signal a **guardian at the gate who sees more than the last changed line**.

Heimdall, the all-seeing watcher of the Bifröst in Norse myth and the Marvel Thor films, matches that role: he does not guess; he observes, then allows or refuses passage.

The POC must integrate a git host. GitHub is the requested POC adapter, with a path to any git-based VCS later.

## Decision

- Product name: **Heimdall**.
- Taglines:
  - **Sees beyond the code.**
  - **With great code comes great responsibility.**
- Console: Heimdall Console. Engine: Verdict Engine.
- Do not use MergeMind or Meridian in any deliverable.
- Alternatives Quorum, VerdictForge, and Meridian are retired except as historical notes.
- Git access goes through a **Provider SPI**. The only implemented adapter in the first code MVP is **GitHub App**. GitLab, Bitbucket, Gitea/Forgejo are documented stubs.

## Consequences

- Branding, Check name (`heimdall/verdict`), package namespaces, pods, metrics, and event addresses use `heimdall`.
- Domain language says “pull request” in GitHub-facing docs and “merge request” only as a synonym in vision text.
- Webhook HMAC, Checks API, and GitHub App permissions become security requirements.
- GitLab-specific features (approvals API, merge trains) are out of the POC.
- Marvel/Norse metaphor is branding only. Claims stay technical: verification, impact, explainable risk. Do not claim supernatural omniscience.
