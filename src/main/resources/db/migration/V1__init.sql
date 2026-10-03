-- Heimdall Phase 1 baseline schema.
-- Source of truth for shape: docs/02-domain-model.md ("Logical tables").
-- Design rules followed: UUID PKs, org_id on every tenant-scoped table,
-- created_at timestamptz, JSONB for tool/evidence payloads, gen_random_uuid()
-- instead of uuid-ossp so this stays CloudNativePG-compatible.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── Identity & tenancy ─────────────────────────────────────────────
CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    github_install_id BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id),
    email           TEXT NOT NULL,
    display_name    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, email)
);

CREATE TABLE memberships (
    org_id          UUID NOT NULL REFERENCES organizations(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    role            TEXT NOT NULL CHECK (role IN ('admin','reviewer','viewer')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (org_id, user_id)
);

-- ── Git provider ───────────────────────────────────────────────────
CREATE TABLE repositories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id),
    provider        TEXT NOT NULL DEFAULT 'github',
    native_id       TEXT NOT NULL,
    full_name       TEXT NOT NULL,
    default_branch  TEXT NOT NULL DEFAULT 'main',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, provider, native_id)
);

CREATE TABLE pull_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID NOT NULL REFERENCES repositories(id),
    number          INTEGER NOT NULL,
    native_id       TEXT NOT NULL,
    title           TEXT,
    author_login    TEXT,
    base_sha        TEXT NOT NULL,
    head_sha        TEXT NOT NULL,
    state           TEXT NOT NULL DEFAULT 'open',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (repository_id, number)
);

CREATE TABLE commits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID NOT NULL REFERENCES repositories(id),
    sha             TEXT NOT NULL,
    pull_request_id UUID REFERENCES pull_requests(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (repository_id, sha)
);

-- ── Skills ─────────────────────────────────────────────────────────
CREATE TABLE skill_packs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id        TEXT NOT NULL,
    version         TEXT NOT NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('language','project')),
    definition      JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (skill_id, version)
);

CREATE TABLE project_skills (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID NOT NULL REFERENCES repositories(id),
    status          TEXT NOT NULL CHECK (status IN ('draft','active','archived')) DEFAULT 'draft',
    definition      JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_project_skills_active ON project_skills (repository_id) WHERE status = 'active';

-- ── Analysis / jobs ────────────────────────────────────────────────
CREATE TABLE analysis_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id),
    repository_id   UUID NOT NULL REFERENCES repositories(id),
    pull_request_id UUID NOT NULL REFERENCES pull_requests(id),
    pr_number       INTEGER NOT NULL,
    head_sha        TEXT NOT NULL,
    parent_run_id   UUID REFERENCES analysis_runs(id),
    state           TEXT NOT NULL CHECK (state IN ('queued','running','succeeded','failed','cancelled')) DEFAULT 'queued',
    workspace_ref   TEXT,
    skill_snapshot  JSONB,
    model_ids       JSONB,
    trace_id        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    UNIQUE (repository_id, pr_number, head_sha)
);

CREATE TABLE analysis_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES analysis_runs(id),
    state           TEXT NOT NULL CHECK (state IN ('queued','running','succeeded','failed','poison')) DEFAULT 'queued',
    locked_at       TIMESTAMPTZ,
    locked_by       TEXT,
    attempts        INTEGER NOT NULL DEFAULT 0,
    max_attempts    INTEGER NOT NULL DEFAULT 5,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_analysis_jobs_claimable ON analysis_jobs (state, created_at) WHERE state = 'queued';

-- ── Findings / evidence / verification ────────────────────────────
CREATE TABLE findings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              UUID NOT NULL REFERENCES analysis_runs(id),
    fingerprint         TEXT NOT NULL,
    agent_id            TEXT NOT NULL,
    claim_class         TEXT NOT NULL,
    title               TEXT NOT NULL,
    claim               TEXT NOT NULL,
    severity            TEXT NOT NULL CHECK (severity IN ('critical','high','medium','low','info')),
    confidence          NUMERIC(4,3),
    locations           JSONB,
    skill_ids           JSONB,
    tool_refs           JSONB,
    contributes_to_score BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_findings_run ON findings (run_id);
CREATE INDEX ix_findings_fingerprint ON findings (fingerprint);

CREATE TABLE evidence (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id      UUID NOT NULL REFERENCES findings(id),
    kind            TEXT NOT NULL CHECK (kind IN
        ('code_location','ast_trace','static_analysis','test_result','dependency',
         'reproduction','secret_fingerprint','policy')),
    tool            TEXT,
    summary         TEXT NOT NULL,
    uri             TEXT,
    payload         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_evidence_finding ON evidence (finding_id);
CREATE INDEX ix_evidence_payload_gin ON evidence USING GIN (payload);

CREATE TABLE verifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id      UUID NOT NULL UNIQUE REFERENCES findings(id),
    status          TEXT NOT NULL CHECK (status IN ('VERIFIED','UNVERIFIED','REJECTED','FALSE_POSITIVE')),
    confidence      NUMERIC(4,3),
    reason          TEXT NOT NULL,
    methods         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Verdict / decision ─────────────────────────────────────────────
CREATE TABLE risk_snapshots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL UNIQUE REFERENCES analysis_runs(id),
    score           NUMERIC(6,3) NOT NULL,
    band            TEXT NOT NULL,
    breakdown       JSONB NOT NULL,
    inputs_hash     TEXT NOT NULL,
    policy_version  TEXT NOT NULL,
    weight_version  TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE decisions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL UNIQUE REFERENCES analysis_runs(id),
    verdict         TEXT NOT NULL CHECK (verdict IN ('MERGE','REVIEW','BLOCK')),
    check_conclusion TEXT NOT NULL CHECK (check_conclusion IN ('success','neutral','failure')),
    explanation     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Policy ─────────────────────────────────────────────────────────
CREATE TABLE policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id),
    version         TEXT NOT NULL,
    document        JSONB NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, version)
);

-- ── Impact graph (Phase 1: shallow import graph) ───────────────────
CREATE TABLE impact_edges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES analysis_runs(id),
    from_ref        TEXT NOT NULL,
    to_ref          TEXT NOT NULL,
    kind            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_impact_edges_run ON impact_edges (run_id);

-- ── Feedback / suppression (eval + Phase 1b) ───────────────────────
CREATE TABLE finding_feedback (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fingerprint     TEXT NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id),
    label           TEXT NOT NULL CHECK (label IN ('correct','incorrect','unsure')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE suppressions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id),
    repository_id   UUID REFERENCES repositories(id),
    fingerprint     TEXT NOT NULL,
    reason          TEXT,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE eval_labels (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fixture_name    TEXT NOT NULL,
    fingerprint     TEXT NOT NULL,
    expected_status TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Audit (append-only) ─────────────────────────────────────────────
CREATE TABLE audit_events (
    id              BIGSERIAL PRIMARY KEY,
    org_id          UUID NOT NULL REFERENCES organizations(id),
    run_id          UUID REFERENCES analysis_runs(id),
    actor_type      TEXT NOT NULL CHECK (actor_type IN ('system','user','github')),
    actor_id        TEXT,
    event_type      TEXT NOT NULL,
    payload         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_events_run ON audit_events (run_id);

-- ── Webhook idempotency (ingestion module) ─────────────────────────
CREATE TABLE webhook_deliveries (
    delivery_id     TEXT PRIMARY KEY,
    event_type      TEXT NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Read models (Console) ──────────────────────────────────────────
-- docs/02-domain-model.md: "Not separate databases. SQL views or query services."
CREATE VIEW pr_overview AS
SELECT pr.id                AS pull_request_id,
       pr.repository_id,
       pr.number,
       pr.title,
       ar.id                AS latest_run_id,
       ar.head_sha,
       ar.state              AS run_state,
       d.verdict,
       rs.score,
       rs.band,
       (SELECT count(*) FROM findings f WHERE f.run_id = ar.id) AS finding_count
FROM pull_requests pr
JOIN LATERAL (
    SELECT * FROM analysis_runs r
    WHERE r.pull_request_id = pr.id
    ORDER BY r.created_at DESC
    LIMIT 1
) ar ON true
LEFT JOIN decisions d ON d.run_id = ar.id
LEFT JOIN risk_snapshots rs ON rs.run_id = ar.id;

CREATE VIEW finding_inbox AS
SELECT f.*, v.status AS verification_status, v.reason AS verification_reason
FROM findings f
JOIN verifications v ON v.finding_id = f.id
WHERE v.status = 'UNVERIFIED' AND f.severity IN ('critical','high');

CREATE VIEW risk_evolution AS
SELECT ar.repository_id, ar.pull_request_id, ar.head_sha, ar.created_at, rs.score, rs.band
FROM analysis_runs ar
JOIN risk_snapshots rs ON rs.run_id = ar.id
ORDER BY ar.pull_request_id, ar.created_at;

CREATE VIEW audit_feed AS
SELECT ae.*, ar.pull_request_id
FROM audit_events ae
LEFT JOIN analysis_runs ar ON ar.id = ae.run_id
ORDER BY ae.created_at DESC;
