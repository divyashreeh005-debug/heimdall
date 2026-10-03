---
kind: language
id: typescript
version: 1.0.0
title: TypeScript and JavaScript
languages: [typescript, javascript]
filename_globs:
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.js"
  - "**/*.jsx"
  - "**/*.mjs"
priority_sections: [security, correctness]
tools:
  - id: eslint
    when: "eslint config present"
  - id: semgrep
    extra_rulesets: ["p/javascript", "p/typescript", "p/react"]
  - id: gitleaks
  - id: trivy
    when: "package-lock.json or yarn.lock or pnpm-lock.yaml changed"
test_hints:
  - npm test
  - pnpm test
  - yarn test
  - npx vitest run
  - npx jest
---

# Goal

Review TypeScript/JavaScript as a senior engineer. Prefer XSS, injection, authZ bugs, and broken contracts over formatting.

# Review order

1. Security
2. Correctness
3. Reliability (async, abort)
4. Performance
5. Testing

# Correctness

- Unhandled promise rejections; missing `await`.
- `any` / unchecked assertions hiding null.
- Wrong HTTP status / error swallowing in Express/Fastify/Next handlers (if present).
- React: stale closures, missing keys only if they cause correctness bugs, unsafe `dangerouslySetInnerHTML`.

# Security

| claim_class | Pattern |
| --- | --- |
| xss | `dangerouslySetInnerHTML`, `innerHTML`, unsanitized markdown |
| sql_injection | string-built SQL to `pg`/`mysql2`/`prisma.$queryRaw` without tagged template |
| command_injection | `child_process.exec` with user string |
| path_traversal | `fs` + user path |
| prototype_pollution | `Object.assign` / merge of user JSON into `{}` prototypes |
| ssrf | `fetch(userUrl)` |
| jwt_alg_none | verifying JWT without alg allowlist |
| secret | keys in source or `NEXT_PUBLIC_` secrets |
| csrf | cookie session mutations without CSRF on cookie-based apps |

# Reliability

- Fetch without timeout/AbortSignal.
- Missing error boundaries only if they hide data loss (info otherwise).

# Performance

N+1 fetches, unbounded `JSON.parse` of request bodies — only if on a public path.

# Testing

Vitest/Jest/Playwright as declared by Project Skill. Error-path tests for authZ.

# Tool bindings

- ESLint (security plugins if configured).
- Semgrep JS/TS/React.
- Gitleaks.
- Trivy on lockfile changes.

# Verification strategies

| claim_class | Prove VERIFIED | Reject / FALSE_POSITIVE |
| --- | --- | --- |
| xss | AST: `dangerouslySetInnerHTML` / `innerHTML` with non-literal from props/query | sanitizer (DOMPurify) with proven use; text children only |
| sql_injection | `$queryRaw` with interpolation; `client.query("..." + user)` | `Prisma.sql` / parameterized `$1` |
| command_injection | `exec`/`execSync` concatenated | `execFile` args array |
| jwt_alg_none | `jwt.verify` without algorithms option or `decode` used as auth | `algorithms: ['RS256']` |
| secret | gitleaks hit in non-test | test fixture under ignore + documented |

# Severity mapping

- xss on authenticated app data, sql_injection, command_injection, jwt_alg_none, verified secret → `critical` / `high`
- missing AbortSignal → `low` / `medium`
- `any` type → `info` unless it hides a security boundary

# Output constraints

- Do not demand Next.js if the project is Vite.
- Do not flag CSS-in-JS preference.
- Skip generated `dist/` (Project Skill ignore).
