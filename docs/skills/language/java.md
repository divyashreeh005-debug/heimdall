---
kind: language
id: java
version: 1.0.0
title: Java
languages: [java]
filename_globs:
  - "**/*.java"
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
  - ./gradlew test
---

# Goal

Review Java as a senior engineer. Identify real issues. Do not invent abstractions or style-only nits as high severity.

# Review order

1. Security
2. Correctness
3. Reliability
4. Performance
5. Maintainability
6. Testing

# Correctness

- Logic errors, wrong API usage, broken control flow, missing edge cases.
- Unsafe `map.get(key).method()` and `optional.get()` without presence checks.
- `Optional` as a field or parameter (return type only).
- Unvalidated external input from REST/gRPC/Kafka used before a null/shape guard.
- Empty catch blocks; logging `e.getMessage()` without the throwable.
- `InterruptedException` swallowed without `Thread.currentThread().interrupt()`.
- Resources not in try-with-resources (`InputStream`, JDBC, HTTP clients).

# Security

Claim classes to consider (must attempt tool corroboration):

| claim_class | Pattern |
| --- | --- |
| command_injection | `Runtime.exec`, `ProcessBuilder`, shell helpers with concatenated user input |
| insecure_deserialization | `ObjectInputStream` on untrusted bytes; XStream/Kryo without allowlist |
| xxe | `DocumentBuilder` / `SAXParser` / `XMLInputFactory` without `FEATURE_SECURE_PROCESSING` and external entities disabled |
| sql_injection | string-built SQL; prefer `PreparedStatement` / named parameters |
| spels_injection | unsanitized SpEL |
| path_traversal | user-controlled `Paths.get` / `new File` |
| ssrf | user-controlled URL to `RestTemplate` / `HttpClient` |
| secret | hardcoded passwords, tokens, PEM in sources |

# Reliability

- Missing timeouts on HTTP/DB.
- `@Async` / servlet threads doing blocking I/O without bounds.
- Interrupted flag not restored.
- Swallowed exceptions.

# Performance

Only when meaningful:

- Coarse `synchronized` on hot paths.
- String `+` in loops (`StringBuilder`).
- Reflection in tight loops.
- Unbounded buffering of request bodies.

# Testing

- Missing tests for interrupt restore, deserialization reject path, validation 400s.
- Prefer testing error paths that match findings.

# Tool bindings

- Semgrep `p/java` and `p/security-audit` on changed and imported files.
- SpotBugs if the Project Skill can compile.
- Gitleaks always.
- Trivy/OSV on changed Maven/Gradle lockfiles.

# Verification strategies

| claim_class | Prove VERIFIED | Reject / FALSE_POSITIVE |
| --- | --- | --- |
| sql_injection | AST: SQL composed with `+` / `String.format` / `MessageFormat` from a parameter that taint-lite traces to request/binding | `PreparedStatement` with placeholders only; Querydsl/jOOQ bound params |
| insecure_deserialization | `ObjectInputStream.readObject` on Kafka/HTTP/JMS payload without `ObjectInputFilter` | JSON/Protobuf parser; filter allowlist proven |
| xxe | Parser factory without secure features, input from untrusted source | `XMLConstants.FEATURE_SECURE_PROCESSING` and DTD disabled |
| command_injection | Concatenated command string from user | `ProcessBuilder` with argument list and allowlisted binary |
| interrupt_swallowed | catch `InterruptedException` without `interrupt()` | flag restored or method throws |
| logger_stack_dropped | `logger.error("..." + e.getMessage())` | SLF4J `logger.error(msg, e)` — severity low, not critical |

Critical VERIFIED requires a non-LLM row from this table (AST or tool).

# Severity mapping

- insecure_deserialization, command_injection, xxe on untrusted input, sql_injection, verified secret in main → `critical`
- interrupt swallowed on worker/control loop → `high`
- logger stack dropped → `low` / `medium`
- Optional.get NPE risk → `medium` unless on a public listener → `high`

# Output constraints

- Cite `path:startLine-endLine`.
- Do not flag `if (logger.isErrorEnabled())` as a bug (nit at most).
- Do not recommend rewriting working JDBC to a new framework.
