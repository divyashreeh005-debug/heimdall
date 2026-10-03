# 05 — Git Provider SPI

Heimdall analyzes **pull/merge requests on any git host**. The POC implements **GitHub App** only. Other hosts are stubs with the same interface.

See [adr/0001-product-name-and-github-first.md](adr/0001-product-name-and-github-first.md).

## Design

```text
ingestion / notification / workspace
            │
            ▼
      GitProvider  (Java interface, this doc)
            │
    ┌───────┼────────────┬─────────────┐
    ▼       ▼            ▼             ▼
 GitHub   GitLab      Bitbucket     Gitea/Forgejo
 (POC)   (stub)        (stub)         (stub)
```

Domain objects are provider-neutral. Native identifiers are stored in `native_id` columns, never used outside the adapter.

## Capability matrix

| Capability | GitHub POC | GitLab | Bitbucket | Gitea |
| --- | --- | --- | --- | --- |
| verifyWebhook | HMAC SHA-256 | stub | stub | stub |
| normalizeEvent | PR opened/sync/reopened | stub | stub | stub |
| getPullRequest | REST/GraphQL | stub | stub | stub |
| listFiles / getDiff | yes | stub | stub | stub |
| listCommits | yes | stub | stub | stub |
| createCheck / updateCheck | Checks API | commit status analog | stub | stub |
| postComment | issue comment + later review comments | stub | stub | stub |
| requestReviewers | later | stub | stub | stub |
| getCodeowners | file fetch | stub | stub | stub |
| getBlame | later | stub | stub | stub |
| listOpenPulls | later (interaction analysis) | stub | stub | stub |
| cloneUrl | installation token | stub | stub | stub |

Stubs throw `ProviderNotImplemented`. The API process refuses to register a repository whose `provider != github` in the POC.

## Java-shaped interface (contract only)

```text
interface GitProvider {
  ProviderId id();  // github | gitlab | bitbucket | gitea

  void verifyWebhook(WebhookRequest raw) throws UnauthenticatedWebhook;
  NormalizedEvent normalizeEvent(WebhookRequest raw);

  PullRequestView getPullRequest(RepoRef repo, int number);
  List<ChangedFile> listFiles(RepoRef repo, int number, String headSha);
  String getDiff(RepoRef repo, int number, String baseSha, String headSha);
  List<CommitView> listCommits(RepoRef repo, int number);

  CheckRef createCheck(RepoRef repo, CheckCreate cmd);
  void updateCheck(RepoRef repo, CheckRef ref, CheckUpdate cmd);

  CommentRef postComment(RepoRef repo, int number, Markdown body);

  void requestReviewers(RepoRef repo, int number, List<ReviewerRef> reviewers); // later

  Optional<String> getCodeowners(RepoRef repo, String ref);
  CloneSpec cloneSpec(RepoRef repo, String sha);

  List<PullRequestView> listOpenPulls(RepoRef repo); // later
}
```

`RepoRef`: `org_id`, `provider`, `native_id` (GitHub: `owner/repo` plus numeric id).

## GitHub App (POC)

### Why App, not PAT

- Installation tokens are short-lived.
- Permissions are least-privilege and visible to the org.
- Checks API is first-class.
- Per-org install maps to Heimdall Organization.

### Permissions (least privilege)

| Permission | Access | Why |
| --- | --- | --- |
| Contents | Read | Clone, blame, CODEOWNERS |
| Pull requests | Read & write | Metadata, comments, request reviewers later |
| Checks | Read & write | `heimdall/verdict` |
| Metadata | Read | Repository identity |
| Webhooks | — | Delivered to Heimdall |

Not requested: administration, actions secrets, merge queue control, commit status unless Checks unavailable.

### Events subscribed

- `pull_request` : opened, synchronize, reopened, ready_for_review (ignore `edited` body-only unless policy says otherwise)
- `pull_request_review` : later (feedback)
- Installation created/deleted: org lifecycle

Draft PRs: analyze if policy `analyze_drafts: true` (default false) to save GPU.

### Webhook authenticity

1. `X-Hub-Signature-256` = `sha256=` HMAC of raw body with App webhook secret.
2. Reject if clock skew vs `X-GitHub-Event` delivery is unused; use **replay window** on `received_at` stored per `X-GitHub-Delivery` (not a GitHub timestamp). Duplicate delivery id → 202 no-op.
3. Body size cap (e.g. 5 MB).
4. Do not parse JSON before HMAC verify.

### Check `heimdall/verdict`

| Decision | Check conclusion | Check title |
| --- | --- | --- |
| MERGE | `success` | Safe to merge (score N) |
| REVIEW | `neutral` (default) or `failure` if policy `review_fails_check` | Needs review (score N) |
| BLOCK | `failure` | Blocked (score N) |

Details URL: Console PR page. Output: markdown summary (truncated) plus text.

Comments are narrative; Checks are control. See [adr/0009-advisory-check-no-automerge.md](adr/0009-advisory-check-no-automerge.md).

### Authentication to GitHub

- App JWT (RS256 with App PEM) → installation access token.
- Token cached until 1 minute before expiry.
- Clone: `https://x-access-token:<token>@github.com/org/repo.git` inside sandbox with token injected as a mount secret, not logged.

### Rate limits

Record `X-RateLimit-Remaining`. On 403/429: job retry with backoff; Check stays `in_progress` until a deadline, then `failure` with `error: github_rate_limit`.

## Normalized event

```yaml
provider: github
delivery_id: string
kind: pull_request.synchronize
repo:
  native_id: "123"
  full_name: acme/payments
pull:
  number: 142
  title: untrusted
  draft: false
  base_sha: abc
  head_sha: def
  author: octocat
action: synchronize
```

## Clone policy

- Shallow clone `--depth` configurable (default 50) at `head_sha`.
- Sparse checkout later for monorepos; not POC.
- Submodules: off by default (`clone.submodules: false`) — supply-chain.
- LFS: off by default.
- Workspace wiped after retention (`workspace.retain_hours`, default 24).

## Stub providers

Each stub documents event mapping for a future adapter:

| Host | Webhook secret | Status analog |
| --- | --- | --- |
| GitLab | `X-Gitlab-Token` | External status / commit status |
| Bitbucket Cloud | HMAC | Build status |
| Gitea/Forgejo | HMAC | Commit status |

No stub implements clone. Do not schedule GitLab work until GitHub path is proven in the coding phase.

## Test doubles

Provider SPI tests (later) use a `FakeGitProvider` with fixtures under `docs/skills/project/example-payments/` conceptual diffs. Contract tests assert HMAC failure paths per adapter.
