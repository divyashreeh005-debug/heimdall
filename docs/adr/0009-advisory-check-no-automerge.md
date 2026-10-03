# ADR-009 — Advisory + Check merge gate; no auto-merge

- Status: accepted
- Date: 2026-09-12

## Context

Auto-merging on an LLM-influenced score is an unsafe default. Comments alone are not a merge gate; humans ignore them. GitHub branch protection already understands Checks.

## Decision

- Heimdall never calls the merge API in the POC.
- Publish GitHub Check `heimdall/verdict`: success / neutral / failure for MERGE / REVIEW / BLOCK.
- Post a summary comment as narrative.
- Organizations enable “require status checks” on the Check name if they want enforcement.
- BLOCK override by humans is later and dual-control; not in POC.

## Consequences

- Enforcement is GitHub’s, not Heimdall’s.
- REVIEW does not fail the Check (neutral), so it will not block merge unless the org treats it as required-and-success-only. Document this for admins.
- For orgs that want REVIEW to block, policy can map REVIEW → Check failure.
