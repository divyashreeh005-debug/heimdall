# 04 — Skills system

Skills are versioned, reviewable artifacts. They are not hidden prompts. See [adr/0005-language-and-project-skills.md](adr/0005-language-and-project-skills.md).

The machine-readable spec is [skills/spec.md](skills/spec.md). Example packs live under `docs/skills/language/` and `docs/skills/project/`.

## Why two kinds

| Kind | Scope | Author | Changes when |
| --- | --- | --- | --- |
| Language Skill | An ecosystem (Java, Python, Go, TypeScript, SQL, Terraform, Kubernetes) | Heimdall platform | Language knowledge or tool bindings change |
| Project Skill | One repository | Generated, then humans | The repo’s build, owners, or architecture change |

Agents receive:

```text
system = platform constitution + Language Skill(s) matching the diff + Project Skill
data   = delimited PR metadata + selected file excerpts
```

PR text never becomes a skill.

## Selection

1. Linguist / extension / filename → language ids on changed files.
2. Load each matching Language Skill at the pin recorded for the org (default: latest stable).
3. Load the repository’s Project Skill (`active` or `draft`).
4. If no language matches, run Security (secrets, lockfiles) + Impact + Tests only; skip Bug LLM.

Multiple languages load multiple packs. Token budget: packs are included in full if they fit the budget envelope; otherwise the orchestrator includes `priority` sections (`security`, `correctness`) first.

## Lifecycle — Language Skill

- Stored in the Heimdall repo (`docs/skills/language/` now; `skills/` module later).
- Semver: MAJOR = breaking checklist/schema, MINOR = new rules, PATCH = wording.
- AnalysisRun records `skill_id@version`.
- Eval harness must be re-run on MAJOR.

## Lifecycle — Project Skill

Onboarding (documented, not built in this phase):

```text
GitHub App install
  → list default branch
  → detect languages, build files, testers, CODEOWNERS, protected paths
  → draft Project Skill
  → Console confirmation (admin)
  → active
```

Until confirmed, analyses run with `draft` and tag findings `project_skill=draft`.

Admins edit Project Skill in Console; each save is a new version; next `head_sha` uses it.

## Trust

| Artifact | Trust |
| --- | --- |
| Platform constitution | Trusted |
| Language Skill in Heimdall product git | Trusted after review |
| Project Skill after admin confirm | Trusted for this org |
| Files named `SKILL.md` inside the customer repo | **Untrusted** unless org policy `skills.allow_repo_overlay: true` |

Customer-repo overlays are later. POC: only Console/DB Project Skill + product Language Skills. This blocks prompt injection via a PR that edits `.heimdall/SKILL.md`.

## Contents

### Language Skill

- `id`, `version`, `languages[]`, `filename_globs[]`
- Review order (correctness → security → …)
- Must-check vulnerability classes
- Tool bindings (Semgrep ruleset, Bandit, ESLint, SpotBugs, govulncheck, Gitleaks extra patterns)
- Test-runner hints (generic; Project Skill overrides)
- Verification strategies (how the Guard should try to prove a class)
- Output constraints (severity mapping)

### Project Skill

- Build and test commands
- Sandbox network policy
- Module map / architecture layers
- Protected paths
- Public API surfaces (OpenAPI files, proto, exported packages)
- CODEOWNERS mapping (or pointer)
- Domain glossary
- Known false-positive fingerprints
- Policy overlays (coverage floor, migration rules)

## Load rule for LLM context

Trusted constitution: [skills/constitution.md](skills/constitution.md).

```text
<constitution>...</constitution>
<language_skill id="java" version="1.0.0">...</language_skill>
<project_skill repo="acme/payments" version="3">...</project_skill>
<untrusted kind="pull_request">...</untrusted>
<untrusted kind="diff_excerpt">...</untrusted>
```

The model is instructed that only tags other than `untrusted` are instructions.

## Mapping from existing checklists

The following Cursor skill files informed the example packs:

- Java: command execution, deserialization, XXE, interrupt handling, Optional misuse
- Python: injection, subprocess, mutable defaults, async pitfalls
- Go: ignored errors, goroutine leaks, context, uncapped allocation

Those checklists are rewritten as Heimdall Language Skills with tool bindings and verification strategies, not copied as chat-only review templates.
