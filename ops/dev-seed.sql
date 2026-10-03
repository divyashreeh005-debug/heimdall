-- Dev-only seed data so the Console's dev-login and PR list have something
-- to show immediately after `docker compose up`. Never run against a real
-- deployment - see ops/README.md.
INSERT INTO organizations (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Dev Org')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, org_id, email, display_name) VALUES
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'dev@heimdall.local', 'Dev Admin')
ON CONFLICT DO NOTHING;

INSERT INTO memberships (org_id, user_id, role) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'admin')
ON CONFLICT DO NOTHING;

INSERT INTO policies (org_id, version, document, active) VALUES
    ('00000000-0000-0000-0000-000000000001', '1', '{}'::jsonb, true)
ON CONFLICT DO NOTHING;
