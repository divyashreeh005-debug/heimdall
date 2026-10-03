---
kind: language
id: go
version: 1.0.0
title: Go
languages: [go]
filename_globs:
  - "**/*.go"
priority_sections: [security, correctness, reliability]
tools:
  - id: govulncheck
    when: "go.mod changed or always if module"
  - id: staticcheck
  - id: gosec
  - id: semgrep
    extra_rulesets: ["p/golang"]
  - id: gitleaks
  - id: trivy
    when: "go.sum or go.mod changed"
test_hints:
  - go test ./...
---

# Goal

Review Go as a senior engineer. Prefer real bugs (ignored errors, races, leaks) over style.

# Review order

1. Security
2. Correctness / error handling
3. Concurrency
4. Context
5. Memory
6. Testing

# Correctness

- Ignored `error` returns (`json.Unmarshal`, `db.Query`, `os.Open`, `_ =`).
- `errors.New` without context at call sites that have ids; prefer `%w`.
- Unexported sentinels crossing package boundaries.
- `panic` in library/handler code for non-programmer errors.

# Security

| claim_class | Pattern |
| --- | --- |
| sql_injection | fmt-built SQL |
| command_injection | `exec.Command("sh", "-c", user)` |
| path_traversal | user-controlled path |
| ssrf | user URL |
| secret | hardcoded credentials |
| integer_overflow | unchecked conversions from request |

# Reliability

- Goroutine leak: `go` without cancel/done.
- Unbounded `go` in a loop over external input.
- Map data race (no mutex / `sync.Map`).
- Mutex held across network I/O.
- `context.Background()` in handlers (drops deadline).
- Not checking `ctx.Err()` after blocking work.
- DB/HTTP without `Context` variants.

# Performance / memory

- `make([]T, req.Count)` from user input without clamp.
- Append in loop over unbounded input without cap.

# Testing

- Table-driven tests for boundaries.
- `t.Helper` on helpers.
- `t.Cleanup` for goroutines started in tests.
- `t.Parallel` when safe.

# Tool bindings

- `gosec`, `staticcheck`, `govulncheck` when module present.
- Semgrep Go pack.
- Gitleaks, Trivy on `go.mod`/`go.sum` changes.

# Verification strategies

| claim_class | Prove VERIFIED | Reject / FALSE_POSITIVE |
| --- | --- | --- |
| ignored_error | AST: call with error return assigned `_` or not assigned | err checked; wrapped |
| sql_injection | query string concatenated/fmt from request | `QueryContext` with `$1` placeholders |
| goroutine_leak | `go f()` with no ctx/stop | ctx cancellation or errgroup |
| map_race | concurrent map write without sync (may remain UNVERIFIED without race detector) | mutex/`sync.Map`; or UNVERIFIED not critical |
| uncapped_alloc | make/slice from request size no clamp | clamp or 400 |
| mutex_io | lock spanning HTTP/SQL | lock reduced |

`go test -race` in sandbox when Project Skill allows time; if skipped, map_race stays UNVERIFIED unless AST is obvious.

# Severity mapping

- ignored error on JSON/DB in HTTP handler → `high` / `critical` if it yields 200 + nil deref
- sql_injection, command_injection, secret → `critical`
- uncapped alloc from user → `high`
- missing `t.Parallel` → `info`

# Output constraints

- Do not demand generics refactors.
- Channel direction nits are `low`.
