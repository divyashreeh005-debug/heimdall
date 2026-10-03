# 06 — Risk model and decision

The Verdict Engine is a pure function. See [adr/0004-deterministic-verdict-engine.md](adr/0004-deterministic-verdict-engine.md). Weights are **policy**, not natural law.

## Inputs

| Input | Source | LLM involved? |
| --- | --- | --- |
| Verified findings | Verification Guard | Only if already tool-backed |
| UNVERIFIED high/critical count | Guard | Yes, but does not add critical weight |
| Test summary | Testing agent / sandbox | No |
| Impact band and edges | Impact agent | Ranking optional |
| Size metrics | diffstat | No |
| Dependency / secret / policy hits | tools | No |
| Policy document | org policy version | No |

## Score

Integer 0–100, clamped. Sum of category contributions after caps.

### Default weights (policy version `verdict.weights.v1`)

| Signal | Condition | Points | Cap |
| --- | --- | --- | --- |
| Critical verified security | ≥1 finding `security` VERIFIED critical | +40 | 40 |
| High verified security | each, if no critical | +15 | 30 |
| High verified bug | each VERIFIED high/critical bug | +25 first, +10 next | 35 |
| Failing required tests | ≥1 failed in required suite | +20 | 20 |
| High change impact | impact band high | +20 | 20 |
| Medium impact | band medium | +10 | 10 |
| Large PR | files > 40 or +lines > 1200 | +10 | 10 |
| Missing tests on risky paths | testing agent gaps on VERIFIED or impact-high files | +5 | 10 |
| Dependency high CVE | Trivy/OSV high/crit on **changed** lockfile | +15 | 25 |
| Secret VERIFIED | Gitleaks corroborated | +40 | 40 |
| Policy deny | hard rule | BLOCK; score at least 76 | — |

UNVERIFIED findings add **0** to the numeric score. They influence **decision** (see below).

REJECTED and FALSE_POSITIVE add 0.

### Bands

| Score | Band |
| --- | --- |
| 0–25 | LOW |
| 26–50 | MEDIUM |
| 51–75 | HIGH |
| 76–100 | CRITICAL |

### Breakdown object

```yaml
score: 70
band: HIGH
components:
  bug: 25
  security: 0
  testing: 20
  impact: 20
  size: 5
  dependency: 0
  policy: 0
weight_version: verdict.weights.v1
policy_version: 3
inputs_hash: sha256:...
```

`inputs_hash` makes the snapshot reproducible. Changing explanation text does not change the hash.

## Decision

```text
if hard_block_rule: BLOCK
else if score >= 76 or verified_critical_security_or_secret: BLOCK
else if score >= 51 or unverified_high_or_critical >= 1 or tests_failed: REVIEW
else if score >= 26: REVIEW
else: MERGE
```

Default mapping (policy can tighten):

| Verdict | Meaning | Check |
| --- | --- | --- |
| MERGE | Low risk, required checks in Heimdall passed | success |
| REVIEW | Moderate risk or unresolved uncertainty | neutral |
| BLOCK | Critical verified issues or policy violation | failure |

Unverified high-severity ⇒ **REVIEW**, never MERGE, never auto-BLOCK solely on LLM suspicion.

### Hard BLOCK rules (policy, not score)

Evaluated even if the additive score is low:

- Verified secret in a non-test path
- Verified RCE / SQLi / command injection / insecure deserialization (critical)
- Required test suite failed
- Protected path modified without `policy.protected_paths.allow` (default: BLOCK or REVIEW per policy; example policy uses BLOCK for `prod` overlays)
- License / policy deny on dependency if enabled

## Worked example (UPI retry)

Candidates:

- Bug: duplicate transaction on retry — Guard **VERIFIED** (high)
- Security: SQL injection — Guard **REJECTED** (parameterized)
- Tests: 182 passed, 2 failed (required)
- Impact: PaymentService → Checkout, Refund, Invoice (high)
- Size: 4 files, modest lines (+5)

Score: 25 + 0 + 20 + 20 + 5 = **70 HIGH** → tests failed ⇒ **BLOCK**.

After fix: tests 184/184, no verified high, impact medium → **18 LOW** → **MERGE**.

Rejected SQL injection appears in Console as rejected AI claim, not in the score.

## Explanation

Decision Agent receives the snapshot and writes markdown:

- Why this band
- Verified issues (max N)
- Rejected claims (count + one-liners)
- Required actions
- Recommended reviewers if present

Prose is stored on `Decision.explanation`. If generation fails, a template explanation is used; the verdict still publishes.

## Counterfactuals (later)

`score(inputs with finding X removed)` reuses the same function. Not MVP. Architecture: `VerdictEngine.explainDelta(before, after)`.

## Size risk details

```text
size_points =
  +5  if changed_files >= 15 or additions+deletions >= 400
  +10 if changed_files >= 40 or additions+deletions >= 1200
  +5  if binary or generated files slipped through (linguist)
```

Generated files should be excluded from size via `.gitattributes` linguist-generated and Project Skill `ignore_globs`.

## Incremental evolution

Each `head_sha` stores a RiskSnapshot. Console plots score over commits and diffs finding fingerprints: opened, fixed, surviving, newly introduced.

## Configuration

Orgs clone `docs/policies/example-policy.yaml`. Changing weights requires `admin` and creates a new policy version. Running analyses always pin the version.
