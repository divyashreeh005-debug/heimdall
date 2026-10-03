---
kind: language
id: terraform
version: 1.0.0
title: Terraform
languages: [terraform]
filename_globs:
  - "**/*.tf"
  - "**/*.tfvars"
priority_sections: [security]
tools:
  - id: tflint
    when: available
  - id: trivy
  - id: gitleaks
---

# Goal

Review infrastructure changes for public exposure, IAM blast radius, and secrets in tfvars.

# Review order

1. Secrets in VCS
2. Public network / 0.0.0.0/0
3. IAM wildcard
4. State/backend changes
5. Destroy of stateful resources

# Security

| claim_class | Pattern |
| --- | --- |
| public_ingress | `0.0.0.0/0` on SSH/DB |
| iam_admin | `Action = "*"` / `Resource = "*"` |
| secret_in_tfvars | plaintext keys |
| public_bucket | ACL public-read |

# Verification strategies

Trivy/tfsec-style rules win. LLM may only propose.

# Severity mapping

Public DB/SSH and verified secrets → `critical`. IAM wildcard → `high` / `critical` per resource.

# Output constraints

Do not demand a different cloud. Respect Project Skill module layout.
