---
kind: language
id: sql
version: 1.0.0
title: SQL and migrations
languages: [sql]
filename_globs:
  - "**/*.sql"
  - "**/db/migration/**"
  - "**/flyway/**"
  - "**/liquibase/**"
priority_sections: [security, correctness]
tools:
  - id: gitleaks
  - id: semgrep
    extra_rulesets: ["p/sql"]
---

# Goal

Review schema and query changes for breakage, data loss, and injection sinks in raw SQL files.

# Review order

1. Destructive migrations
2. Locking / performance
3. Grants and RLS
4. Injection (dynamic SQL)

# Correctness

- `DROP` / `TRUNCATE` / `DELETE` without `WHERE` in migrations.
- Type changes that fail on existing data.
- Missing down migration only if Project Skill requires it (document, do not BLOCK by default).

# Security

- `GRANT ALL` to public.
- Dynamic SQL (`EXECUTE format` with unsanitized identifiers).

# Verification strategies

| claim_class | VERIFIED | FALSE_POSITIVE |
| --- | --- | --- |
| destructive_migration | AST/text: DROP TABLE/COLUMN without guard | drop of unused staging object named in Project Skill glossary |
| sql_injection | EXECUTE with concatenated session user input | format with `%I` identifier quoting only |

# Severity mapping

Destructive on prod-shaped tables → `critical` (policy often hard-blocks).

# Output constraints

Do not rewrite dialects (Postgres vs MySQL) unless the Project Skill specifies the engine.
