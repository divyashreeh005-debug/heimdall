# Skill pack specification

Media type: Markdown document with YAML front matter. Encoding UTF-8.

AnalysisRun stores `id` + `version` for each loaded pack.

## Language Skill front matter

```yaml
---
kind: language
id: java
version: 1.0.0
title: Java
languages: [java]
filename_globs:
  - "**/*.java"
  - "**/*.kt"          # optional related
priority_sections: [security, correctness, reliability]
tools:
  - id: semgrep
    extra_rulesets: ["p/java", "p/security-audit"]
  - id: spotbugs
    when: "pom.xml or build.gradle present"
  - id: gitleaks
  - id: trivy
    when: "pom.xml or gradle.lockfile changed"
test_hints:
  - mvn -q test
  - gradle test
---
```

Body sections (required headings):

1. `# Goal`
2. `# Review order`
3. `# Correctness`
4. `# Security`
5. `# Reliability`
6. `# Performance` (optional but recommended)
7. `# Testing`
8. `# Tool bindings`
9. `# Verification strategies`
10. `# Severity mapping`
11. `# Output constraints`

`Verification strategies` map `claim_class` → how the Guard proves or rejects (AST query, tool id, test idea). Without this section the Guard can only UNVERIFIED LLM claims.

## Project Skill front matter

```yaml
---
kind: project
id: acme-payments
version: 1
status: draft | active
repository: acme/payments
default_branch: main
languages: [java, typescript]
---
```

Required body headings:

1. `# Build and test`
2. `# Sandbox`
3. `# Architecture`
4. `# Protected paths`
5. `# Public APIs`
6. `# Owners`
7. `# Glossary`
8. `# Known false positives`
9. `# Ignore`

## Selection algorithm

```text
changed_files ← diff paths not matching ProjectSkill.ignore or linguist-generated
langs ← detect(changed_files)
packs ← LanguageSkill where languages ∩ langs ≠ ∅ or glob matches
project ← repository project skill (active else draft)
context ← constitution + packs + project
```

## Token budget trimming

If packed skills exceed `llm.budgets` headroom, keep in order:

1. Project Skill `# Build and test`, `# Sandbox`, `# Protected paths`, `# Public APIs`
2. Each Language Skill `# Security` then `# Correctness` then `# Verification strategies`
3. Remaining sections

## Schema validation (implementation later)

Reject load if `kind` missing, `version` not semver (language) or integer (project), or required headings absent. Analysis then fails `skills.load` (required node).
