# 11 — Heimdall Console (information architecture)

**Sees beyond the code.** **With great code comes great responsibility.**

Frontend stack (implementation later): React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui, TanStack Query, React Router. JWT bearer. No Next.js.

The Console is a read-mostly operator UI. It does not compute scores.

## Routes

| Path | Role | Purpose |
| --- | --- | --- |
| `/login` | public | Credential / later OIDC |
| `/` | viewer+ | Overview |
| `/pull-requests` | viewer+ | List |
| `/pull-requests/:id` | viewer+ | Detail |
| `/pull-requests/:id/impact` | viewer+ | Impact graph |
| `/pull-requests/:id/heatmap` | viewer+ | Risk heatmap |
| `/pull-requests/:id/evolution` | viewer+ | Risk over commits |
| `/inbox` | reviewer+ | UNVERIFIED queue |
| `/skills` | viewer+ | Language packs (read) |
| `/skills/project/:repoId` | admin write | Project Skill editor |
| `/policy` | admin | Policy YAML |
| `/audit` | viewer+ | Audit trail |
| `/eval` | admin | Precision panel |
| `/settings` | admin | GitHub install, users |

## Overview

Cards/table of open PRs: number, title, repo, score, band, verdict, verified/rejected/unverified counts, last run time.

Filters: repo, verdict, band, author.

## PR detail

Header: title, SHAs, Check link, verdict badge, score `/100`.

Tabs:

1. **Summary** — Decision explanation, required actions, reviewers.
2. **Findings** — Grouped by verification status. Each row: severity, claim, location, evidence drawer, fingerprint, feedback buttons (correct / incorrect / irrelevant).
3. **Risk** — Breakdown bars (bug, security, testing, impact, size, dependency, policy). Weight version shown.
4. **Verification** — Guard methods and reasons; rejected claims with “why the AI was wrong.”
5. **Tests** — Pass/fail, sandbox log excerpt (redacted).
6. **Impact** — Component list; link to graph.
7. **Skills** — Skill versions used for this run (immutable snapshot).
8. **Audit** — Events for this PR.
9. **History** — Risk evolution.

Rejected findings are visible but visually secondary so developers learn without noise in the score.

## Impact graph

Nodes: files/symbols/services from `ImpactEdge`. Edges: imports, HTTP, schema, config. Highlight changed nodes. Empty state: “index too small / languages unsupported.”

MVP if time remains; stub route allowed with “not in MVP” empty state.

## Risk heatmap

Files or modules colored by contribution to score (verified findings density + impact). Not LLM heat.

## Verification inbox

Queue of UNVERIFIED high/critical across repos. Actions: confirm (promotes to VERIFIED **only if** reviewer is `reviewer+` and a reason is given — still cannot create critical security VERIFIED without a tool hook unless role `admin` and policy `human_critical_verify: true`, default false). Dismiss → FALSE_POSITIVE + suppression.

Default: humans cannot mint critical VERIFIED without tools. They can keep UNVERIFIED and block merge via REVIEW.

## Skills UI

- Language: render markdown packs from the API (product-shipped).
- Project: form + raw YAML/markdown for glossary, test command, protected paths. Save creates a version.

## Policy editor

YAML editor with schema validation against `example-policy.yaml`. Diff vs previous version. Saving does not retroactively change completed runs.

## Eval panel

See [10-evaluation.md](10-evaluation.md).

## UX principles

- Verdict color: MERGE green, REVIEW amber, BLOCK red. Do not use color alone (icons + text).
- Every important finding shows evidence or an explicit “no execution evidence.”
- Do not hide rejected AI claims; collapse them.
- No auto-merge button.

## Component sketch (not implementation)

- `AppShell` — nav, org switcher (single org POC)
- `VerdictBadge`, `ScoreMeter`, `FindingTable`, `EvidenceDrawer`
- `ImpactGraph` (later, e.g. xyflow)
- `PolicyEditor` (monaco or textarea + validate)

## Accessibility

WCAG 2.2 AA intent: keyboard inbox triage, contrast on badges, skip link. Not a full a11y audit in docs POC.

## API usage

TanStack Query keys: `['pr', id]`, `['prs', filters]`, `['inbox']`. Types generated later from OpenAPI. Errors: 401 redirect login, 403 toast, 404 empty.
