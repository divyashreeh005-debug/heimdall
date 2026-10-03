# ops/

- `dev-seed.sql` - run once against a freshly migrated dev database
  (`psql "$DB_URL" -f ops/dev-seed.sql`) so `POST /auth/dev-login` with
  `dev@heimdall.local` returns a token and the Console has an org to show.
- Podman Quadlet units (`.container` files matching
  docs/08-deployment-podman.md's four pods) are not included in this
  skeleton; `docker-compose.yml` at the repo root is the dev equivalent.
  Translate it with `podman generate systemd` or hand-write Quadlets before
  shipping to CloudNativePG-backed infra.
