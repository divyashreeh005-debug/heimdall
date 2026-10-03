# Heimdall — Phase 1 skeleton

**Sees beyond the code. With great code comes great responsibility.**

This is a working skeleton implementing the Phase 1 coding MVP described in
`docs/12-roadmap.md`, built directly from the architecture and contract pack
in `docs/`. It runs end to end — webhook in, LLM agents propose findings,
a deterministic Verdict Engine scores risk, a Decision Service picks
MERGE/REVIEW/BLOCK, the Console shows the result — with the gaps called out
explicitly in `docs/13-phase1-implementation-spec.md` rather than hidden.

Read `docs/13-phase1-implementation-spec.md` first. It maps every phase to
the actual classes and says plainly what's real logic versus a wired
placeholder.

## What's here

```
pom.xml                      Spring Boot 3 / Java 21 / Spring Modulith backend
src/main/java/com/heimdall/  api + worker process roles, one artifact
src/main/resources/          config, Flyway migration, bundled language skills
src/test/java/                unit tests + a Spring Modulith boundary test
console/                     React 18 + TS + Vite + Tailwind Console
docker-compose.yml           postgres + ollama + api + worker + console
Containerfile.backend        multi-stage build for the Spring Boot jar
ops/                         dev seed data, deployment notes
docs/                        the original architecture/contract pack (unchanged)
```

## Run it locally

```bash
cp .env.example .env      # edit JWT_SECRET, GITHUB_WEBHOOK_SECRET, etc.
docker compose up --build

# first run only: pull the models into the ollama container
docker exec -it heimdall-ollama-1 ollama pull qwen2.5-coder:14b
docker exec -it heimdall-ollama-1 ollama pull nomic-embed-text

# seed a dev org/user so the Console has something to log into
psql "$DB_URL" -f ops/dev-seed.sql
```

Then:
- API: `http://localhost:8080` (OpenAPI UI at `/swagger-ui.html`)
- Console: `http://localhost:5173`, log in as `dev@heimdall.local`
- Postgres: `localhost:5432`
- Ollama: `localhost:11434`

Point a GitHub App's webhook at `POST http://localhost:8080/webhooks/github`
(use a tunnel like `ngrok` for a real repo) with the same secret as
`GITHUB_WEBHOOK_SECRET`. Without `GITHUB_DEV_TOKEN` set, runs will execute
and show up in the Console, but the GitHub Check/comment won't post — see
the Phase 1.6 gap in the implementation spec.

## Run the backend without Docker

```bash
mvn -q package
java -jar target/heimdall.jar                                    # api role
java -jar target/heimdall.jar --spring.profiles.active=worker    # worker role
```

Requires a local PostgreSQL 16 and Ollama reachable at the URLs in
`application.yml` (override with `DB_URL`, `OLLAMA_BASE_URL`, etc.).

## Run the console without Docker

```bash
cd console
npm install
npm run dev
```

## Tests

```bash
mvn test
```

Includes `VerdictEngineTest`, which reproduces the "UPI retry" worked
example from `docs/06-risk-and-decision.md` (70 HIGH before the fix, LOW
after), `HmacVerifierTest`, and `ModularityTest`, which fails the build if a
module reaches into another module's internals instead of its public API —
the actual enforcement of `docs/01-c4-architecture.md`'s Level 3 boundaries.

## Product names

Per `docs/adr/0001-product-name-and-github-first.md`: **Heimdall**,
**Heimdall Console**, **Verdict Engine**. Do not use "MergeMind" or
"Meridian" in deliverables.
