---
kind: language
id: kubernetes
version: 1.0.0
title: Kubernetes manifests
languages: [kubernetes]
filename_globs:
  - "**/k8s/**/*.yaml"
  - "**/k8s/**/*.yml"
  - "**/kubernetes/**/*.yaml"
  - "**/helm/**/*.yaml"
priority_sections: [security]
tools:
  - id: trivy
  - id: kubesec
    when: available
  - id: gitleaks
---

# Goal

Review Kubernetes and Helm YAML for privilege, exposure, and secret mounts. Only files matching the globs or Project Skill `k8s_roots` are in scope (generic YAML is not).

# Review order

1. Privileged / hostPath / hostNetwork
2. Secrets in env literals
3. Public Services / Ingress without TLS
4. Image tags (`latest`)
5. Resource limits missing on untrusted workloads

# Security

| claim_class | Pattern |
| --- | --- |
| privileged_pod | `privileged: true`, `hostPID`, `hostNetwork` |
| secret_in_manifest | `value:` passwords, kubeconfigs |
| public_service | `NodePort`/`LoadBalancer` on DB ports |
| latest_tag | `:latest` in prod overlays |

# Verification strategies

Trivy config/k8s rules win. Cross-check Project Skill `protected_paths` for `deploy/prod`.

# Severity mapping

Privileged in prod overlay and secrets in manifests → `critical`. `:latest` → `medium`.

# Output constraints

Do not demand Helm if the repo uses raw manifests (or vice versa).
