# 10 — Evaluation harness

Research question:

> Can an evidence-based multi-agent verification layer reduce false-positive and unsupported findings in AI-assisted pull-request analysis?

This is a **product feature**. See [adr/0010-eval-harness.md](adr/0010-eval-harness.md).

## What we measure

On a labeled set of pull requests (gold):

| Metric | Definition |
| --- | --- |
| Findings emitted | Candidate count from proposing agents |
| True issues | Gold `correct` |
| False positives | Gold `incorrect` or `irrelevant` that were still shown |
| Precision | true / (true + false positives) among **shown scoring** findings |
| Guard reject rate | REJECTED / candidates |
| Guard unverified rate | UNVERIFIED / candidates |
| Verified recall | VERIFIED ∩ gold correct / gold correct |

Two modes on the same candidates:

1. **Without verification** — treat all candidates as if they would be published (today’s AI reviewer).
2. **With verification** — publish/score only VERIFIED; UNVERIFIED go to inbox (counted separately).

Example table (illustrative, not results):

```text
                    Without       With
                    verification  verification
AI findings             120           120
True issues              82            84
False positives          38            10
Precision               68%           89%
```

The POC docs do not invent real percentages. The coding phase must run the harness on fixtures plus opt-in production labels.

## Gold labels

Sources:

1. Human Console feedback: `correct` | `incorrect` | `irrelevant` on a fingerprint.
2. Curated fixtures: synthetic diffs with known bugs (parameterized vs concatenated SQL, etc.).
3. Later: imported SARIF from trusted SAST as weak labels.

Schema: `eval_labels (org_id, repo_id, fingerprint, label, source, labeled_by, sha)`.

A finding is matched to gold by fingerprint, then fuzzy fingerprint.

## Fixtures (to author in coding phase)

Minimum set for the Guard:

| Fixture | Expected Guard |
| --- | --- |
| Python parameterized SQL | REJECTED / FALSE_POSITIVE on SQLi claim |
| Python string-concat SQL | VERIFIED critical |
| Java ObjectInputStream on Kafka payload | VERIFIED critical |
| Java logger without throwable | VERIFIED low/medium quality, not critical |
| Go ignored `err` | VERIFIED medium |
| Hardcoded AWS key in src | VERIFIED secret BLOCK |
| Test fixture AWS key under `src/test` | policy-dependent, often FALSE_POSITIVE |
| Hallucinated “race” with no shared state | REJECTED or UNVERIFIED |

Store fixtures as tiny git repos in `eval/fixtures/` later. This docs POC only specifies them.

## Console panel

Route `/eval`:

- Select date range / repo
- Table: with vs without Guard
- Confusion-style counts
- Drill-in to disagreements (candidate VERIFIED but human `incorrect`) — these are Guard bugs and must create audit + skill patches

Access: `admin` (org). Aggregate without file contents in the default view.

## Protocol

1. Freeze skill versions and model id.
2. Run analysis with Guard on (normal).
3. Offline replay: recompute precision as if Guard were identity (all candidates published).
4. Report both. Do not tune weights on the eval set used for the thesis table; keep a holdout.

## What eval is not

- Not a substitute for the Verdict Engine tests (those are unit tests of the pure function).
- Not permission to scrape private repos into a public paper. Papers use fixtures + anonymized aggregates.

## API

See OpenAPI `GET /v1/eval/summary`. Implementation later.
