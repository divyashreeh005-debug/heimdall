---
kind: language
id: python
version: 1.0.0
title: Python
languages: [python]
filename_globs:
  - "**/*.py"
  - "**/*.pyi"
priority_sections: [security, correctness, reliability]
tools:
  - id: bandit
  - id: semgrep
    extra_rulesets: ["p/python", "p/owasp-top-ten"]
  - id: gitleaks
  - id: trivy
    when: "requirements*.txt or poetry.lock or uv.lock or Pipfile.lock changed"
test_hints:
  - pytest
  - python -m pytest
---

# Goal

Review Python as a senior engineer. Find real issues. Do not invent problems or unnecessary abstractions.

# Review order

1. Correctness
2. Security
3. Reliability
4. Performance
5. Maintainability
6. Pythonic style (nits only)
7. Testing

# Correctness

- Logic errors, wrong assumptions, missing edge cases, off-by-one.
- Incorrect exception handling, broken control flow, wrong API usage.
- Mutable default arguments and mutable class variables.
- Type confusion around `None`.

# Security

| claim_class | Pattern |
| --- | --- |
| sql_injection | query built with `+` / f-string / `%` / `.format` then executed |
| command_injection | `shell=True`, `os.system`, concatenated `subprocess` |
| path_traversal | user path joined without resolve/jail |
| insecure_deserialization | `pickle.loads`, `yaml.load` without `SafeLoader`, `eval`/`exec` on user data |
| secret | tokens in source |
| ssrf | user URL to `requests`/`httpx` |
| weak_crypto | md5/sha1 for security |

# Reliability

- Missing timeouts on `requests` / HTTPX / DB.
- File handles not in context managers.
- Swallowed exceptions, incomplete cleanup.
- Async: blocking I/O in async functions; tasks not cancelled; exceptions from tasks dropped.

# Performance

Only when meaningful: O(n²), N+1 queries, loading large objects, unbounded copies.

# Testing

Happy path, error path, edges, boundaries. Meaningful assertions. Mocks only at true boundaries.

# Tool bindings

- Bandit on changed `.py`.
- Semgrep Python + OWASP packs.
- Gitleaks.
- Trivy on lockfile changes.

# Verification strategies

| claim_class | Prove VERIFIED | Reject / FALSE_POSITIVE |
| --- | --- | --- |
| sql_injection | AST: `execute`/`executemany` with a concatenated/f-string query; taint from request/arg | `"... ?" / "%s"` with params tuple/dict; SQLAlchemy `bindparams`; Django ORM |
| command_injection | `shell=True` or `os.system` with non-literal | `subprocess.run([...], shell=False)` literals/allowlist |
| pickle | `pickle.loads` on network/file from user | pickle on trusted local cache with HMAC (still warn medium) |
| yaml | `yaml.load` without SafeLoader | `safe_load` |
| async_blocking | `time.sleep` / `requests.get` inside `async def` | `asyncio.sleep` / httpx async |

Reproduction (optional): Guard may add a sandbox pytest that asserts the unsafe call shape; do not exploit production.

# Severity mapping

- sql_injection, command_injection, pickle on untrusted, verified secret → `critical`
- missing HTTP timeout on egress → `medium` / `high` if in a request path
- mutable default → `low` / `medium`
- style-only → `info`, never scores

# Output constraints

- Ignore formatting unless it hides a bug.
- No “rewrite in Rust.”
- Mention positive patterns (parameterized SQL) when rejecting a hallucination.
