# 00 — Vision and positioning

**Sees beyond the code.**  
**With great code comes great responsibility.**

Heimdall is named for the guardian who watches the Bifröst: the one who sees what approaches the gate, not only the last step across it. In this product that gate is the merge decision.

## One-line description

Heimdall is a multi-agent system that analyzes, verifies, and assesses the risk of GitHub pull requests, combining code, security, testing, repository impact, and execution evidence to provide explainable Merge, Review, or Block recommendations.

## Problem

Pull-request workflows stitch together disconnected activities: human review, static analysis, CI tests, impact guesswork, and a merge decision. AI reviewers can accelerate inspection, but they also produce unsupported claims and false positives. Teams still cannot answer a single question with evidence:

> Is this pull request safe to merge, what could it affect, which findings are real, and what must happen before merging?

## What Heimdall is

An evidence-verified, multi-agent platform for:

1. Specialized analysis of a pull request (bugs, security, tests, impact).
2. Independent verification of AI findings against repository and execution evidence.
3. Hallucination and false-positive filtering.
4. Repository-level change-impact analysis.
5. Explainable merge-risk scoring (Verdict Engine).
6. Risk-based merge decision support (MERGE / REVIEW / BLOCK).
7. Continuous re-analysis across commits.
8. Human-in-the-loop escalation for unverified or high-risk change.

## What Heimdall is not

Heimdall is not positioned as “an AI code-review bot.”

A typical AI reviewer:

```text
Pull Request → AI Review → Issues / Comments
```

Heimdall:

```text
Pull Request
  → Multi-agent analysis
  → Candidate findings
  → Independent verification
  → False-positive / hallucination filtering
  → Change-impact analysis
  → Deterministic risk calculation
  → Evidence-based decision
  → MERGE / REVIEW / BLOCK
  → GitHub Check + Console
```

## Operating principle

| Role | Actor |
| --- | --- |
| Propose | LLM agents, guided by Language Skills and Project Skills |
| Prove or reject | Deterministic tools, AST, tests, dependency data |
| Score | Verdict Engine (no LLM) |
| Narrate | Thin LLM over already-computed verdict |
| Own MERGE under uncertainty | Humans |

```text
Analyze → Find → Verify → Filter hallucinations → Impact → Score → Explain → MERGE | REVIEW | BLOCK
```

The LLM never writes the risk number and never unilaterally merges.

## Claims we make

Heimdall focuses on combining specialized AI analysis with independent evidence verification, repository-level impact analysis, and an explainable risk-based pull-request decision workflow.

## Claims we do not make

- “Existing tools only review changed files.” Modern AI reviewers can already be repository-aware.
- “No existing system uses AI for code review.” That is false.
- “The default risk weights are universally correct.” Weights are policy and must be evaluated per organization.
- “LLM output is truth.” It is a hypothesis until verified.
- “Heimdall auto-merges safely.” The POC is advisory plus a GitHub Check. Merge remains human or branch protection.

## Research question

Can an evidence-based multi-agent verification layer reduce false-positive and unsupported findings in AI-assisted pull-request analysis?

The evaluation harness in [10-evaluation.md](10-evaluation.md) is a product feature, not a thesis appendix. It compares precision with and without the Verification Guard on labeled pull requests.

## Differentiators that matter

1. **Verification Guard** — a downstream agent that cannot VERIFIED-critical a claim without tool, AST, or test evidence.
2. **Language Skills + Project Skills** — versioned, reviewable artifacts, not hidden prompt folklore.
3. **Provider SPI** — any git host in principle; GitHub App for the POC.
4. **GitHub Checks as the gate** — comments narrate; `heimdall/verdict` controls branch protection.
5. **Deterministic Verdict Engine** — explainable 0–100 score with a breakdown and hard policy BLOCKs.
6. **Shared analysis workspace** — parse and clone once; agents share evidence instead of inventing parallel views of the tree.
7. **Local Ollama** — code stays on the operator’s machines for the POC; data-residency is a design property, not a marketing slide.

## Audience

| Audience | Why they care |
| --- | --- |
| Staff / platform engineers | Merge gate they can audit, policy they can change, no mystery score |
| Security | Secrets, SAST, dependency, sandbox of untrusted tests |
| Engineering managers | Risk evolution, reviewer load, blocked vs review queues |
| Researchers / MCA | Precision lift from verification, labeled eval set |
| Developers | Fewer hallucinated comments, evidence on every important finding |

## Success for this documentation POC

- A new engineer can explain Heimdall versus an AI review bot in one page (this document).
- C4 diagrams, OpenAPI, and AsyncAPI describe the same system.
- The skill spec is detailed enough to author Java, Python, Go, and TypeScript packs.
- Security covers webhook authenticity, mTLS, JWT, sandbox, and prompt injection.
- Podman topology lists pods, ports, secrets, and trust boundaries.
- The roadmap does not present a 50-feature essay as the MVP.
