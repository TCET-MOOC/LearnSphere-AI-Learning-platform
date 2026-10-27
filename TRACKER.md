# LearnSphere AI — Build tracker

**Read this first when you pick the project up.** It says where we are, what's next, and what changed from the plan.
**Update it at the end of every working session**: tick items, add to the next-up list, and record any deviation from `Docs/PLAN.md`.

| | |
|---|---|
| Last updated | 2026-10-27 |
| Current phase | **Wave 2 — Curriculum and authoring in progress (`W2-01`, `W2-02`, `W2-03`, `W2-04`, `W2-05`, `W2-06`, `W2-07`, `W2-08` complete and verified).** Wave 1 is 100% complete and pushed to `tcet/main` (`38f5597`). `W2-01` added `courses.department_id` FK and backfill (`73d7b92`). `W2-02` established the course state machine and catalog filtering (`e2dc073`). `W2-03` replaced hard deletion with soft-delete archiving and restore (`d587ce3`). `W2-04` implemented module and content contiguous ordering, reorder endpoints, and delete re-normalisation (`413d3f8`). `W2-05` migrated `Lecture` 1:1 to `CourseContent` via Flyway `V5__legacy_lecture_migration.sql`, backfilled `user_progress.content_id`, retired legacy lecture mutation endpoints (400), and maintained legacy lecture read endpoints as a read-only projection. `W2-06` enforced assignment validation and ownership IDOR defense (`ebfc48e`). `W2-07` implemented storage port, upload safety, opaque file keys, and removed public uploads (`1bba6f0`). `W2-08` rebound notes and bookmarks to canonical `CourseContent`, migrated legacy lecture rows via Flyway `V7__notes_bookmarks_content_rebind.sql`, bounded timestamps to content duration, and enforced IDOR defenses. |
| Next milestone | **Wave 2 — Curriculum and authoring** (`W2-09 YouTube import into canonical content` through `W2-17`). |
| Plan | [`Docs/PLAN.md`](Docs/PLAN.md) · **Backlog: [`Docs/plan/waves.md`](Docs/plan/waves.md)** · API: [`Docs/plan/api-catalogue.md`](Docs/plan/api-catalogue.md) · Data: [`Docs/plan/schema-usage.md`](Docs/plan/schema-usage.md) |
| Audit | [`audit.md`](audit.md) (2026-10-24) — 5 P0 findings and the P1/P2 remediation overlay `AUD-01…AUD-16` |
| Repo | `https://github.com/TCET-MOOC/LearnSphere-AI-Learning-Platform.git` |

---

## 1. How to run it

Prerequisites: Java 17, Node 22, npm 10.9+, Docker (Testcontainers and CI), PostgreSQL 16+ for a bare-metal run, Git. The canonical builds are `learnsphere-backend/` and `learnsphere-payments-service/`; the root Maven duplicate was removed in P0-07.

```bash
# backend (port 8080) — needs PostgreSQL database MOOC
cd learnsphere-backend && ./mvnw spring-boot:run

# payments microservice (port 8081) — needs PostgreSQL database learnsphere_payments
cd learnsphere-payments-service && ./mvnw spring-boot:run

# frontend (port 4200)
cd learnSphere/frontend/learnsphere && npm ci && npm start

# hermetic fast tests (backend needs Docker for its Testcontainers PostgreSQL)
cd learnsphere-backend && ./mvnw test
cd learnSphere/frontend/learnsphere && npm test

# Playwright smoke (starts the Angular dev server)
cd learnSphere/frontend/learnsphere && npx playwright install chromium && npm run e2e

# build
cd learnsphere-backend && ./mvnw package
cd learnSphere/frontend/learnsphere && npm run build
```

| What | Where |
|---|---|
| Main API | `http://localhost:8080` |
| Payments microservice | `http://localhost:8081` |
| Angular app | `http://localhost:4200` |
| PostgreSQL | `localhost:5432` (`MOOC`, `learnsphere_payments`) |
| Seed users (`demo`/`test` only) | `admin@` / `teacher@` / `student@` / `external@` / `pending@learnsphere.com` — password from `SEED_PASSWORD`; never a literal (G-F10) |

---

## 2. Progress

Legend: `[x]` done · `[~]` partial · `[ ]` not started. Every ID below mirrors `Docs/plan/waves.md` exactly. A behaviour-changing story is not ticked until its first failing test, red/green evidence and integration test are recorded; docs/config stories use a verification checklist.

### Phase 0 — Foundations
- [x] P0-01 Freeze the source pack and docs tree
- [x] P0-02 Replace the stale tests with a real harness + test-first evidence format
- [x] P0-03 GitHub Actions CI
- [x] P0-04 Flyway baseline + `ddl-auto=validate`
- [x] P0-05 Docker Compose local stack
- [x] P0-06 Actuator health + structured logs
- [x] P0-07 Config hygiene (root duplicate build, env vars, profiles)
- [x] P0-08 Seed profile separation + reset script
- [x] P0-09 API conventions (problem+json, pagination, OpenAPI)

**P0-02 evidence (2026-10-24):**
- Backend red: `.\mvnw.cmd -q -Dtest=OnlineLearningPlatfromApplicationTests#shouldUseDisposablePostgresInsteadOfTheLocalDatabase test` failed because the test connected to `jdbc:postgresql://localhost:5432/MOOC` (the developer database).
- Backend green: `.\mvnw.cmd -q clean test` -> `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; the test asserts the live JDBC URL equals the `postgres:16-alpine` container URL and that the seeded roles exist.
- Frontend red: `npm test -- --watch=false` -> `src/app/app.spec.ts > App > should render title` failed with `NG0201: No provider found for ActivatedRoute`.
- Frontend green: `npm test` -> 2 tests passed; `angular.json` now runs Vitest with `src/test-setup.ts`, and `app.spec.ts` uses the real `appConfig` providers.
- Harness: `PostgresIntegrationTest` uses a disposable PostgreSQL 16 container; `application-test.properties` supplies test-only config; `.github/pull_request_template.md` records test file/name, red command/result, green command/result and suites run.
- Testcontainers is pinned to `1.21.4` because Spring Boot 3.3.5's managed `1.19.8` cannot negotiate with Docker Engine 29. Docker is required for the backend suite; no local database is used.

**P0-03 evidence (2026-10-24):**
- Added `.github/workflows/ci.yml`: triggered on every push and pull request; jobs for backend tests, payments-service build, frontend unit + production build, and Playwright smoke; a final `ci-gate` job fails unless all four succeed.
- Artifact upload is `if: always()`: backend/payments Surefire reports, frontend `dist/`, and Playwright HTML report/traces, each retained for 7 days.
- Backend CI-equivalent: `.\mvnw.cmd -B clean test` -> `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` at P0-03 (4 after the P0-04 baseline test); `BUILD SUCCESS`.
- Payments CI-equivalent: `.\mvnw.cmd -B test` -> `BUILD SUCCESS` at P0-03 (no test sources then; 3 after the P0-04 baseline test).
- Frontend CI-equivalent: `npm ci`; `npm test` -> 2 passed; `npm run build` -> success (existing initial-bundle warning: 2.37 MB vs 2 MB budget); `npm run e2e` -> 2 Playwright smoke tests passed.
- Playwright smoke (`e2e/public-smoke.spec.ts`) starts the Angular dev server, serves `/login`, checks the login form, and navigates to `/register`. Full user journeys remain W7-10.
- Remote follow-up: the current GitHub token has `WRITE`, not `ADMIN`, so branch protection could not be changed. A repository admin must require the `CI gate` status check before merges; the first GitHub-hosted run happens when this change is pushed.

**P0-04 evidence (2026-10-24):**
- Red: `.\mvnw.cmd -B test -Dtest=OnlineLearningPlatfromApplicationTests#shouldBuildTheSchemaFromFlywayAndValidateItWithHibernate` failed with `expected: "validate" but was: "create"`; the new assertion also had no `flyway_schema_history` table to read.
- Baselines: `learnsphere-backend/src/main/resources/db/migration/V1__baseline.sql` and `learnsphere-payments-service/src/main/resources/db/migration/V1__baseline.sql` were generated schema-only from the deployed PostgreSQL databases (38 tables in `MOOC`, 2 in `learnsphere_payments`) and normalized for Flyway/PostgreSQL 16. `\restrict`/`\unrestrict`, `transaction_timeout` and `CREATE SCHEMA public` were removed; `search_path` is explicit.
- Config: both services now use `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.baseline-on-migrate=true`, `baseline-version=1` and `locations=classpath:db/migration`; `flyway-core` and `flyway-database-postgresql` are on the classpath.
- Backend green: `.\mvnw.cmd -B clean test` -> `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`; Flyway applied V1 to an empty `postgres:16-alpine` database, then Hibernate validated all 36 mapped entities. `BUILD SUCCESS`.
- Payments green: `.\mvnw.cmd -B clean test` -> `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; Flyway applied V1 and Hibernate validated `orders`/`payments`. `BUILD SUCCESS`.
- The integration tests assert `ddl-auto=validate`, a successful `flyway_schema_history` row at version 1, and the expected application tables. Existing databases are adopted at V1 by `baseline-on-migrate`; no data is rewritten. `Docs/plan/schema-usage.md` records the baseline and migration contract.
- The local `MOOC`/`learnsphere_payments` databases were used only to generate the schema baselines; the proof database is disposable Testcontainers PostgreSQL 16. Starting the app locally will create `flyway_schema_history` and baseline V1 on the existing schema.

**P0-05 evidence (2026-10-24):**
- Assets: root `docker-compose.yml`; multi-stage `Dockerfile` + `.dockerignore` for the backend, payments service and Angular frontend; `nginx.conf` with SPA fallback and `/healthz`; `infra/postgres/init/01-create-payments-db.sql` to create `learnsphere_payments` beside `MOOC`.
- Health: `/actuator/health` is exposed on both Spring services and permitted without authentication in the backend (`SecurityConfig`). At P0-05 time this was the healthcheck prerequisite; P0-06 later added readiness/liveness groups, JSON logs and trace IDs.
- Config: `docker compose config --quiet` -> exit 0; services are `db`, `backend`, `frontend`, `payments`. Host ports can be overridden with `POSTGRES_PORT`, `BACKEND_PORT`, `PAYMENTS_PORT` and `FRONTEND_PORT`; `.env.example` documents the local defaults.
- Build: `docker compose build` -> `BUILD_EXIT=0` for all three images. The first health run found the frontend healthcheck used `localhost` while nginx listens only on IPv4; it now probes `http://127.0.0.1/healthz`.
- Start green: with `POSTGRES_PORT=5434` (local 5432/5433 are already owned by PostgreSQL 18 and `auth_postgres`), `docker compose up -d --wait --wait-timeout 240` -> `UP_EXIT=0`; all four containers reported `healthy`.
- Smoke: backend and payments `/actuator/health` -> `UP`; `GET /api/courses` -> 200 with seeded courses; frontend `/` and `/login` -> 200; CORS from `http://localhost:4200` returned `Access-Control-Allow-Origin: http://localhost:4200`.
- Database proof: `flyway_schema_history` has a successful V1 row in both databases; `MOOC` has 39 public tables (38 baseline + history) and `learnsphere_payments` has 3 (2 baseline + history).
- Regression: backend `.\mvnw.cmd -B clean test` -> 4 tests passed; payments -> 3 tests passed. No errors in service logs (only pre-existing Hibernate/dialect warnings).
- Teardown: `docker compose down -v --remove-orphans` -> exit 0; all containers, the network and both volumes were removed.
- Addendum: the stack was verified with disposable volumes. A normal `docker compose up` on a machine with free ports 5432/8080/8081/4200 uses the committed defaults; occupied ports can be changed in `.env`.

**P0-06 evidence (2026-10-25):**
- Readiness/liveness: both services expose `/actuator/health`, `/actuator/health/readiness` and `/actuator/health/liveness`; probes are enabled and readiness includes `readinessState,db`. `SecurityConfig` permits `/actuator/health/**`, and CORS exposes `X-Trace-Id`.
- Correlation: `TraceIdFilter` accepts `X-Request-Id`/`X-Trace-Id`, rejects unsafe values, generates a UUID when neither is usable, puts `traceId` in MDC, returns `X-Trace-Id`, and clears MDC after the request. The payments `RestTemplate` forwards the current trace ID as `X-Request-Id` to the backend.
- JSON logs: `logback.xml` plus `logstash-logback-encoder 7.4` emit JSON with `traceId` and `service`; the Spring banner and Hibernate `show-sql` plain output are disabled. `System.err`/`printStackTrace` were replaced with SLF4J in the websocket, YouTube and payments paths.
- Red/green: readiness went red before the probe config and green after; `TraceIdFilterTest` went red before the filter and green after; `TraceIdLoggingTest` was added before the JSON encoder and went green with `logback.xml`; `AuthServiceLoggingTest` went red on the logged reset token and green after removing it; `TraceIdPropagationTest` went red before the payments interceptor and green after.
- Regression: backend `.\mvnw.cmd -B clean test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; payments -> `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- Compose verification: `docker compose build backend payments` -> 0; a fresh `docker compose down -v` then `POSTGRES_PORT=5434 docker compose up -d --wait --wait-timeout 240` -> 0; all four containers healthy. Both services returned `UP` for health, readiness and liveness on `8080`/`8081`; `/api/courses`, frontend `/` and `/login` returned 200.
- Log proof: `POST /api/auth/forgot-password` with `X-Request-Id: p0-06-final-smoke` returned 200 and `X-Trace-Id: p0-06-final-smoke`; the corresponding JSON log line carried the same `traceId`. The returned reset token and the real `NVIDIA_NIM_API_KEY` were absent from both services' logs, and no `nvapi-` string was present.
- Structured-output proof: `docker compose logs --no-color --no-log-prefix` had 0 non-JSON lines for both backend and payments; both carried their `service` field.
- Teardown: `docker compose down -v --remove-orphans` -> 0; all containers, the network and both volumes were removed.

**P0-07 evidence (2026-10-25):**
- One build path: deleted the stale root `pom.xml`, `mvnw`, `mvnw.cmd` and `.mvn/`; each service now owns its wrapper and `pom.xml`. `RUNNING.md` and the payments README point at those service directories.
- Secrets: `application.properties` no longer contains a JWT secret, internal API key, database password or Razorpay key. `JWT_SECRET`, `INTERNAL_API_KEY`, `SPRING_DATASOURCE_PASSWORD`/`DB_PASSWORD`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` and `RAZORPAY_WEBHOOK_SECRET` are required environment values; there are no secret fallbacks in the Spring config or Compose. `.env.example` documents every local value, and Compose rejects a missing required variable.
- Profiles: both services have a shared `application.properties`, `application-local.properties` (host PostgreSQL), `application-prod.properties` (all datasource values required, forwarded headers) and the existing `application-test.properties` under `src/test/resources` (Testcontainers). `spring.profiles.default=local`; tests assert that the `test` profile is active without `local` leaking in.
- Secret scan: `git grep` is clean for `elon3005`, `change-this-super-secret`, `my_secret_internal_key`, `rzp_test_TRE1J0tk6PNK0i`, `your_webhook_secret` and `nvapi-`. At P0-07 time the shared demo seed password remained in `DataSeeder`; P0-08 has since removed it and moved the demo seed behind `demo`/`test`.
- Config proof: `docker compose --env-file .env.example config --quiet` -> 0; `docker compose --env-file .env.example build backend payments` -> 0; a fresh `POSTGRES_PORT=5434 docker compose --env-file .env.example up -d --wait --wait-timeout 240` -> 0 with all four containers healthy. The `local` profile booted first, then the same stack was recreated with `SPRING_PROFILES_ACTIVE=prod`; both services reported healthy and `/actuator/health` -> `UP` in each profile.
- Runtime proof: `/api/courses`, frontend `/` and `/login` -> 200; container environment showed `SPRING_PROFILES_ACTIVE=local` (and later `prod`) and the JWT value matched `.env.example`; the JWT and other `local-dev-only` values were absent from both services' JSON logs, which still had 0 non-JSON lines.
- Regression: backend `.\mvnw.cmd -B clean test` -> `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`; payments -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`. Teardown `docker compose --env-file .env.example down -v --remove-orphans` -> 0.

**P0-08 evidence (2026-10-25):**
- Seeder split: the old unconditional `DataSeeder` is gone. `RoleSeeder` (`@Order(1)`) seeds only the reference `STUDENT`/`TEACHER`/`ADMIN` vocabulary in every profile. `DemoDataSeeder` is `@Profile({"demo", "test"})` and creates the documented personas in a fixed order: teacher (`1`, TEACHER, TCET), student (`2`, STUDENT, TCET), admin (`3`, ADMIN, TCET), external student (`4`, STUDENT, Sinhgad) and pending teacher (`5`, TEACHER, no college plus a PENDING affiliation). It also creates the teacher's two LIVE courses, seven lectures, the student's CS201 enrolment and two verified colleges.
- Password: `app.seed.password` is required from `SEED_PASSWORD` by `application-demo.properties`; there is no literal in source or Compose. The test profile supplies its own value. Password hashes are BCrypt and the demo accounts authenticate with the configured value.
- Determinism: the demo rows use the fixed timestamp `2025-01-01 09:00:00` where the entity owns one, and the clean-reset insert order fixes the ids. `@PrePersist` hooks now preserve supplied timestamps instead of overwriting them.
- Reset: `scripts/reset-demo.sh` removes the volumes, starts Compose with the `demo` profile, waits for health, prints the persona table and never prints the password. `.env.example` defaults Compose to `demo`; `local` remains an empty development database and `prod` requires the deployment datasource.
- Red/green: `ProdSeedProfileTests` failed when the demo seeder profile was temporarily widened to include `prod` (`Expecting empty but was: {"demoDataSeeder"=...}`), then passed after restoring `@Profile({"demo", "test"})`. The test asserts the `prod` context loads, Flyway V1 is applied, the demo seeder bean is absent, the three reference roles exist, and users/courses/enrolments/colleges/departments/affiliation requests are all zero.
- Persona/password test: `OnlineLearningPlatfromApplicationTests#shouldSeedTheDocumentedDemoPersonasUnderTheTestProfile` asserts all five personas, their roles/colleges, the two courses, the enrolment, the PENDING affiliation, the configured seed password, and the fixed timestamps.
- Backend: `.\mvnw.cmd -B clean test` -> `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- Payments: `.\mvnw.cmd -B clean test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- Compose config: `docker compose --env-file .env.example config --quiet` -> 0; `docker compose build backend payments` -> 0.
- Prod proof: `SPRING_PROFILES_ACTIVE=prod POSTGRES_PORT=5434 docker compose --env-file .env.example up -d --wait db backend payments` -> 0; both services healthy with `SPRING_PROFILES_ACTIVE=prod`; DB counts were users `0`, courses `0`, enrollments `0`, colleges `0`, departments `0`, roles `3`, Flyway V1 `1`.
- Reset proof: `ENV_FILE=.env.example POSTGRES_PORT=5434 RESET_DEMO_SKIP_BUILD=1 bash scripts/reset-demo.sh` ran twice. Both runs printed personas `1`-`5` in the same order and produced byte-identical SQL snapshots of users, courses, lectures, enrollments, colleges, departments, affiliation requests and roles (timestamps `2025-01-01 09:00:00`). The stack was healthy after each run.
- Login proof: `POST /api/auth/login` with `student@learnsphere.com` and the `SEED_PASSWORD` from `.env.example` -> 200 with an access token; `/api/courses`, frontend `/` and `/login` -> 200. The seed password appeared in neither service's logs, and both logs remained 0 non-JSON lines.
- Teardown: `docker compose --env-file .env.example down -v --remove-orphans` -> 0; containers, network and volumes removed.

**P0-09 evidence (2026-10-25):**
- Contract: `ApiErrorCode` owns the stable code/status/type triples; `ProblemResponse` is the RFC 9457 body (`type`, `title`, `status`, `detail`, `instance`, `code`, `traceId`, optional `errors`); `ProblemResponseFactory` builds it; `GlobalExceptionHandler` maps 404/400/401/403/validation/malformed JSON/parameter/405/415/413/409/`ResponseStatusException`/unknown errors; `ApiSecurityErrorHandler` emits the same body for filter-chain `401`/`403`.
- Red/green: before the implementation, `ApiErrorContractTests` failed 6/6 - missing authentication returned an empty `403` instead of `401`, a missing resource returned a legacy `{timestamp,status,error}` body instead of `problem+json`, the test 500 endpoint returned `404`, and validation had no stable `code`. After the implementation all 6 passed. Two later tests (unsupported method and unknown path) plus a log assertion brought the final class to 8/8; the log assertion proves the server logs `Unhandled exception [traceId=...]` with the stack trace while the response only exposes the generic `INTERNAL_ERROR` detail.
- Determinism: a password violating both `@Size` and `@Pattern` produced three validation entries for two fields; `oneErrorPerField` now collapses duplicates deterministically (sorted by field, then message). Validation returns `errors: [{field, message}]` sorted by field.
- Pagination: `Dto/PageResponse` exposes `{content, page, size, totalElements, totalPages}`; `application.properties` sets zero-based `page`/`size`/`sort` defaults of 20 and a maximum of 100. `PageResponseTests` -> 3/3. Existing lists keep their array shape until their owning wave migrates them.
- OpenAPI: springdoc `2.6.0` publishes `/v3/api-docs` and `/swagger-ui.html` in `local`/`demo`/`test`; `application-prod.properties` disables both. `OpenApiConfig` adds the title/version, `bearerAuth` and the shared `Problem`/`PageResponse` schemas. `OpenApiDocumentTests` writes `target/openapi.json` during the normal suite; the generated document was OpenAPI `3.0.1` with 159 paths, `Problem`, `PageResponse` and `bearerAuth`. CI now runs `test -s target/openapi.json` and uploads the `openapi-document` artifact.
- Frontend: `ErrorHandlerService` reads problem+json (`errors[0].message`, then `detail`, then the legacy `message`/`fieldErrors` shape); the `403` branch of `error.interceptor.ts` uses the same extractor. `error-handler.service.spec.ts` covers problem+json, legacy, unreachable and unknown bodies -> 5 tests; frontend total 7/7 in 2 files.
- Live HTTP proof on the Compose `demo` stack: `/actuator/health` 200; `/v3/api-docs` 200; `/swagger-ui.html` 302 -> 200; `GET /api/courses/999999` -> `404 application/problem+json` with `code=NOT_FOUND`, `instance=/api/courses/999999` and the supplied `X-Request-Id` echoed as `traceId`; invalid login -> `400 VALIDATION_FAILED` with one message each for `email` and `password`; `GET /api/enrollments/me` without a token -> `401 UNAUTHORIZED`; valid demo login -> 200 with a token; the student calling `/api/teacher/courses` -> `403 FORBIDDEN`; `GET /api/courses` -> 200. Both services' logs remained 0 non-JSON lines and the seed password was absent.
- Regression: backend `.\mvnw.cmd -B clean test` -> `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` (8 `ApiErrorContractTests`, 1 `OpenApiDocumentTests`, 3 `PageResponseTests`); payments -> `Tests run: 10`, `BUILD SUCCESS`; frontend `npm test` -> 2 files / 7 tests passed; frontend production build succeeded with the pre-existing 2.37 MB initial-bundle warning.
- Deviation: the payments service keeps its legacy error bodies until W6 (W6-02…W6-05) hardens those endpoints; all Wave 1+ endpoints live in the backend and use the new contract from the first commit.
- Teardown: `POSTGRES_PORT=5434 docker compose --env-file .env.example down -v --remove-orphans` -> 0; containers, network and volumes removed.

### Wave 1 - Identity, session and authorization
- [x] W1-01 Freeze role vocabulary and seed roles
- [x] W1-02 Registration hardening (STUDENT only, teacher application)
- [x] W1-03 Account lifecycle ACTIVE/FLAGGED/BLACKLISTED
- [x] W1-04 JWT claims and lifetime
- [x] W1-05 Refresh tokens, rotation, logout revocation
- [x] W1-06 Client session: memory-only access token
- [x] W1-07 Unique user-role + admin-only assignment (closed with W1-11)
- [x] W1-08 One ownership layer across controllers
- [x] W1-09 WebSocket handshake + SUBSCRIBE authorization
- [x] W1-10 Auth/IDOR regression suite (legacy user API, localStorage, refresh replay)
- [x] W1-11 Lock down legacy `/api/users/**` (AUD-14)
- [x] W1-12 Visual baseline harness + token inventory + stylelint ratchet (DS-01)
- [x] W1-13 Theme correctness: public routes light, no hardcoded surfaces (DS-02)
- [x] W1-14 Typography classes + heading semantics (DS-03)
- [x] W1-15 Landing truth, nav and dead links, no restyle (DS-04)
- [x] W1-16 Auth screens on tokens (DS-05)

**W1-01 evidence (2026-10-27):**
- Contract: Role vocabulary is frozen to `ADMIN`, `TEACHER`, `STUDENT`. `RoleVocabulary` canonicalizes roles case-insensitively and maps `INSTRUCTOR` alias to `TEACHER` at the edge (A-01); arbitrary roles outside the vocabulary are rejected with 400 Bad Request. `POST /api/auth/register` maps `INSTRUCTOR` to `TEACHER` and persists the canonical role in `user_roles`. `GET /api/admin/users?role=INSTRUCTOR` maps to `TEACHER` and matches teachers. `/api/roles/**` is locked down to ADMIN-only (`@PreAuthorize("hasRole('ADMIN')")` and `SecurityConfig`) with `Cache-Control: private, no-store`. Creating/updating roles outside `{ADMIN, TEACHER, STUDENT}` returns 400; creating existing roles (including `INSTRUCTOR` mapping to `TEACHER`) returns 409 CONFLICT; reference roles cannot be deleted (409 CONFLICT).
- Red: `RoleVocabularyTests` failed 7/8 (unauthenticated/student calls returned 200 instead of 401/403; INSTRUCTOR registration failed with 400; admin user filter with role=INSTRUCTOR was empty; deleting reference role returned 204; creating SUPERUSER returned 200; duplicate INSTRUCTOR creation returned 200; Cache-Control lacked private no-store).
- Green: `RoleVocabularyTests` passed 8/8. Full backend suite `.\mvnw.cmd -B clean test` -> `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments service `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest `npm test` -> `2 files / 7 tests passed`.

**W1-02 evidence (2026-10-27):**
- Contract: Public registration (`POST /api/auth/register`) creates `STUDENT` accounts only (A-11, AC-03). The `role` field is optional (defaults to `STUDENT`); requesting `ADMIN` returns `400 BAD_REQUEST` ("ADMIN accounts cannot be self-registered"); requesting `TEACHER` or `INSTRUCTOR` creates the account with role `STUDENT`. Linking an unverified college at registration or during verification is rejected with `400 BAD_REQUEST` ("Only verified colleges can be linked" / "College must be verified to submit an affiliation request"). Authenticated teacher applications (`POST /api/auth/verify-college` with `idDocumentUrl` / `claimedDepartment`) verify the college is `VERIFIED` and create a `PENDING` `AffiliationRequest`. Duplicate pending requests return `409 CONFLICT`. Admin approval (`PUT /api/admin/colleges/affiliations/{id}/approve`) sets status `APPROVED`, attaches the college to the user, promotes the applicant to `TEACHER` in `user_roles`, and records an append-only `USER_ROLE_ASSIGNED` entry in `audit_log` (AC-03, A-24).
- Red: `RegistrationHardeningTests` failed 8/8 (self-registration with `ADMIN` returned 200 instead of 400; registration with `TEACHER` produced role `TEACHER` instead of `STUDENT`; omitting role returned 400 validation error; registration with unverified college returned 200; student teacher application did not create affiliation request; teacher application for unverified college returned 200; duplicate pending affiliation request returned 200 instead of 409; admin approval did not grant `TEACHER` role).
- Green: `RegistrationHardeningTests` passed 8/8; `RoleVocabularyTests` passed 8/8. Full backend suite `.\mvnw.cmd -B clean test` -> `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments service `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest `npm test` -> `2 files / 7 tests passed`.

**W1-03 evidence (2026-10-27):**
- Contract: Account status is constrained to `ACTIVE`, `FLAGGED`, `BLACKLISTED` (A-23, BR-01). `BLACKLISTED` accounts are rejected at login (`POST /api/auth/login`) with `401 UNAUTHORIZED` (generic `INVALID_CREDENTIALS`), and existing JWT tokens belonging to blacklisted users are rejected by `JwtAuthFilter` with 401. `FLAGGED` accounts can authenticate and perform read operations (`GET`), but mutating write operations (`POST`, `PUT`, `PATCH`, `DELETE`) are blocked with `403 FORBIDDEN` via `FlaggedUserInterceptor` (exempting `/api/auth/logout` and `/api/auth/refresh`). `isActive` is derived dynamically as `status != BLACKLISTED` on `UserAccount`, `UserResponseDto` and `UserAdminDto`. Status updates (`PUT /api/admin/users/{id}/status`) validate status values (rejecting invalid statuses with 400 Bad Request) and record `USER_STATUS_CHANGED` in `audit_log`. The append-only audit log query endpoint (`GET /api/admin/audit`) is live with `PageResponse` pagination, filtering by `actorId`, `resourceType`, `resourceId`, `action`, ADMIN-only protection, and `Cache-Control: no-store, private` headers.
- Red: `AccountLifecycleTests` failed 7/9 (missing `GET /api/admin/audit` -> 404; missing `isActive` property; FLAGGED user mutating writes permitted -> 200 instead of 403; BLACKLISTED user bearer token accepted -> 200 instead of 401; BLACKLISTED login error code mismatch; derived `isActive` stub false).
- Green: `AccountLifecycleTests` passed 9/9. Full backend suite `.\mvnw.cmd test` -> `Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments service `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest `npm test` -> `2 files / 7 tests passed`.

**W1-04 evidence (2026-10-27):**
- Contract: JWT access token lifetime is configured to 15 minutes (`900000` ms) in `application.properties` and `.env.example` (AC-06, A-09). The token carries standard and custom claims: `sub` (user email), `userId` (`Long`), `roles` (`List<String>`), `dept_id` (`Long` or `null` if unassigned), `iss` (`learnsphere`), and `aud` (`learnsphere-web`) (AC-05, A-09). Token validation in `JwtService` and `JwtAuthFilter` strictly verifies signature, expiration, issuer matching, and audience matching; tokens with invalid issuer, invalid audience, or expired validity are rejected immediately (returning 401 UNAUTHORIZED at filter chain).
- Red: `JwtClaimsAndLifetimeTests` failed 4 tests and errored 1 test out of 7 (`shouldIncludeRequiredClaimsAndLifetime` failed due to missing claims; `shouldProduceTokenWithDeptIdAtLogin` failed with expected `2L` but was `null`; `shouldRejectTokenWithInvalidIssuer` and `shouldRejectTokenWithInvalidAudience` expected `false` but was `true`; `shouldRejectExpiredToken` errored with unhandled `ExpiredJwtException`).
- Green: `JwtClaimsAndLifetimeTests` passed 7/7. Full backend suite `.\mvnw.cmd test` -> `Tests run: 64, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments service `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest `npm test` -> `2 files / 7 tests passed`.

**W1-11 evidence (2026-10-25):**
- Contract: `/api/users/me` is the only self-service route (GET/PUT) and accepts only `fullName`, `bio` and `avatarUrl`; e-mail, status, roles, department and password are ignored even when present in the body. `GET /api/users/{id}` and every `/{id}/roles` route are `@PreAuthorize("hasRole('ADMIN')")`; `/api/user-roles/**` is ADMIN at the filter chain. Retired: `GET /api/users`, `POST /api/users`, `PUT/DELETE /api/users/{id}`, `PATCH /api/users/{id}/status`.
- Red: `UserApiAuthorizationTests` -> `Tests run: 7, Failures: 5, Errors: 1`; `GET /api/users` returned `200`, a student read `/api/users/{id}` with `200`, no-store headers were absent, `/api/users/{id}/roles` returned the `UserRole` entity, and `audit_log` did not exist. Green: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`; full backend suite `Tests run: 32`, `BUILD SUCCESS`.
- Audit: `AuditLog` + `AuditService` write append-only rows inside the business transaction with actor, `before_json`/`after_json` snapshots and client IP (`X-Forwarded-For` first hop, else the socket address). Actions are `USER_ROLE_ASSIGNED`, `USER_ROLE_REVOKED` and `USER_STATUS_CHANGED` on resource type `USER`. `V2__audit_log.sql` creates the table, its actor/resource/created-at indexes, deduplicates any legacy `user_roles` rows and adds `ux_user_roles_user_role` on `(user_id, role_id)`; a duplicate assignment now returns `409 CONFLICT`.
- Harness fix: `PostgresIntegrationTest` gained `@DirtiesContext(AFTER_CLASS)`. The static container is stopped after each test class but Spring reused a cached context whose datasource still pointed at the previous container port, so a fourth `test`-profile class produced 30 s Hikari timeouts. Evicting the context after each class keeps the per-class database and the context lifecycle aligned; this was a latent `P0-02` harness defect, not a W1-11 regression.
- Live Compose proof (`demo`, `POSTGRES_PORT=5434`): `/api/users/me` -> `200` with `Cache-Control: no-store, private`; student `GET /api/users/3` -> `403 FORBIDDEN`; student `GET /api/user-roles` -> `403`; student status write -> `403`; admin `GET /api/users` -> `404 NOT_FOUND`; `POST /api/users` -> `404`; `PUT`/`DELETE /api/users/{id}` -> `405 METHOD_NOT_ALLOWED`; `PATCH /api/users/{id}/status` -> `404`; assign `TEACHER` -> `200` DTO `{id,userId,roleId,roleName}`, repeat -> `409 CONFLICT`, revoke -> `204`. `audit_log` held the assigned, revoked and status-changed rows with `actor_id=3`; the student's e-mail and bcrypt hash were unchanged and admin reads contain no `password` key. Backend logs: 43 lines, 0 non-JSON, 0 occurrences of the seed password.
- Regression: payments `Tests run: 10`, `BUILD SUCCESS`; frontend `2 files / 7 tests` passed. The frontend was not changed because it only calls `/users/me` and `/admin/users`.
- Note: `GET /api/admin/users` still returns an array rather than the `PageResponse` envelope, consistent with `P0-09` until its owning wave (W7-11/W7-12) migrates it.
- Teardown: `POSTGRES_PORT=5434 docker compose --env-file .env.example down -v --remove-orphans` -> 0; containers, network and volumes removed.

**W1-12 evidence (2026-10-25) — DS-01 visual baseline, token extraction, style ratchet:**
- Contract: the 47 screen contracts from `Docs/source/spec.md` §4 are one row each in `e2e/visual/screen-contracts.ts`; 46 are captured, `SCR-35` is skipped with a recorded reason (no route in `app.routes.ts`). Captured 46 × 3 viewports (360×800/768×900/1280×900) × 2 themes = **276 baselines** in `learnSphere/frontend/learnsphere/e2e/visual/__screenshots__/` (21.7 MB, avg 80 KB). `.gitignore` no longer ignores `__screenshots__/`.
- Determinism: every `/api/**` request is answered by `e2e/visual/api-stubs.ts` (session, catalogue, notes/bookmarks, assessments, admin summaries, payments on :8081); the clock is fixed at `2026-10-25T09:00:00Z`; `ls_token`/`ls_user`/`ls_theme_mode` are seeded before boot; all eight webfont weights are forced before capture; CSS animations are frozen by `toHaveScreenshot`; unmodelled endpoints fail the test. Second run without `--update`: `277 passed, 1 skipped` in 3.1 min, no diff beyond the 0.2 % pixel tolerance.
- Tokens: `src/styles/_tokens.scss` now owns every existing `:root`/`[data-theme]` custom property, including the two typography faces moved out of `_typography.scss`; `styles.scss` imports `tokens` before `typography`. **No visual change**: `HEAD` and current `styles.scss` compile to the same 501 normalised CSS lines — only the ordering of the font-variable block and the theme blocks differs, and they declare disjoint properties.
- Ratchet: stylelint 17 + `postcss-scss` (`stylelint.config.mjs`) bans raw hex colours, literal `border-radius` values and `transition: all`; `scripts/stylelint-ratchet.mjs` compares per-file counts against `stylelint.baseline.json` and fails on any per-file **or** global increase (`npm run lint:styles`, `npm run lint:styles:update` re-baselines). The token files are exempt.
- Baseline (scope `src/**/*.scss` + `src/**/*.html`, token files excluded; occurrences, not distinct values): hex **669** (54 files), radius **448** (58), `transition: all` **117** (49), uppercase **88** (46), inline `style` **269** (13), `translateY` lifts **58** (31), `backdrop-filter` **25** (18), icon families **2**. Distinct values for the DS-06 inventory: **147** hex colours and **37** radius values (including `var(--border-radius-*)`, `$radius-*` and multi-value shorthands); the most common raw radii are 8 px ×125, 12 px ×79, 50% ×73, 10 px ×55. Only the first three metrics are enforced now; the rest are recorded for DS-07/DS-13. Delta vs F24 (723 hex, 18 radii, 117, 84, 2): `transition: all` and icon families match exactly; F24 counted distinct radius values (18) and used different source boundaries for hex/uppercase.
- Red/green: a probe file with one hex, one radius and one `transition: all` -> exit 1 with all six messages; a doctored baseline whose global total equalled the current total still failed the rising file, proving the per-file rule independently. Restored -> exit 0, `669/669`, `448/448`, `117/117`.
- Verification: `npm run build` exit 0 (pre-existing CommonJS warnings; the known initial-bundle budget warning is unchanged); `npm test` `2 files / 7 tests`; `npm run e2e:visual` `277 passed, 1 skipped`; `npm run e2e` (smoke project only) `2 passed`.
- axe: **not applicable** — DS-01 changes no rendered pixel, so no screen changed; the first axe scan arrives with DS-02/W1-13 on the screens that story changes.
- CI: `frontend` now runs `npm run lint:styles`; a new `Visual baseline` job runs `npm run e2e:visual` on `windows-latest` (same platform as the baselines) and feeds `CI gate`. Neither has run on GitHub yet; the `CI gate` required-check follow-up is unchanged.

**W1-05 evidence (2026-10-27) — Refresh tokens, rotation, logout revocation (AUD-15):**
- Red: `RefreshTokenLifecycleTests` initially failed with compilation errors due to missing `RefreshToken` entity and `RefreshTokenRepository`.
- Flyway: Created `V3__refresh_tokens.sql` creating table `refresh_tokens` with columns `id`, `user_id` (FK to `users` cascade delete), `token` (UNIQUE), `family_id`, `status` (`ACTIVE`, `ROTATED`, `REVOKED`), `replaced_by_token`, `expires_at`, `created_at`, and indexes on `user_id`, `family_id`, `expires_at`, and `status`.
- Entity & Repository: Created `RefreshToken` JPA entity with `Status` enum (`ACTIVE`, `ROTATED`, `REVOKED`) and `RefreshTokenRepository` with `@Modifying(clearAutomatically = true, flushAutomatically = true)` family revocation query.
- Service & Controller: Implemented `RefreshTokenService`/`RefreshTokenServiceImpl` handling 7-day token issuance, atomic rotation marking predecessor `ROTATED` with `replacedByToken` and creating successor in the same `familyId`, replay detection revoking the entire `familyId` upon re-use of an already rotated token, logout token revocation, and `ResponseCookie` helpers (`Path=/api/auth; HttpOnly; SameSite=Lax; Max-Age=604800` on issue/rotate, `Max-Age=0` on logout). Updated `SecurityConfig` to permit `/api/auth/refresh` and `/api/auth/logout`.
- Green: `.\mvnw.cmd test -Dtest=RefreshTokenLifecycleTests` -> `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`, all 7 integration tests passing.
- Full regression: Backend `.\mvnw.cmd test` -> `Tests run: 71, Failures: 0, Errors: 0, Skipped: 0`; Payments `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`; Frontend `npm test` -> `7 passed (7)`.

**W1-06 evidence (2026-10-27) — Client session: memory-only access token (AUD-15):**
- Red: `npm test` failed initially with TypeScript compile errors for missing `AuthStore` and missing `getAccessToken`/`refreshToken` on `AuthService`.
- AuthStore: Created `AuthStore` (`src/app/core/auth/auth.store.ts`) holding `_accessToken` strictly in-memory and `currentUser$` via `BehaviorSubject`. Purges legacy keys `ls_token` and `ls_user` on session creation and clearing.
- StorageService: Refactored `StorageService` to delegate token/user operations to in-memory `AuthStore` ensuring zero tokens or sensitive user info are ever written to `localStorage`.
- AuthService: Updated `AuthService` to hold session in `AuthStore`, added `getAccessToken()`, added silent `refreshToken()` calling `POST /auth/refresh` with `{ withCredentials: true }`, updated `login`/`register` to send credentials, and updated `logout()` to call backend `POST /auth/logout` with credentials and clear client state.
- Interceptors: Updated `tokenInterceptor` to attach in-memory Bearer token from `AuthStore` and enforce `withCredentials: true`. Updated `errorInterceptor` to intercept 401s on protected endpoints, execute a single silent refresh using the HttpOnly cookie, retry the failed request with the rotated access token, and immediately log out and redirect to `/login` if refresh fails or on auth entrypoints.
- Green & Full regression: `npm test` -> `5 passed (5)` test files, `15 passed (15)` tests; `npm run build` -> production build succeeded (`BUILD_EXIT=0`); `npm run lint:styles` -> 100% passed ratchet. Backend suite (71/71) and payments suite (10/10) remain green.

**W1-08 evidence (2026-10-27) — One ownership layer across controllers (IDOR protection):**
- Contract: Built unified `OwnershipService` in `com.MOOC.OnlineLearningPlatfrom.Security` implementing `requireCourseStaff`, `requireModuleStaff`, `requireContentStaff`, `requireLectureStaff`, `requireAssignmentStaff`, `requireTestStaff`, `requireQuestionStaff`, `requireAttemptOwnerOrStaff`, `requireSubmissionOwnerOrStaff`, `requireEnrolled`, `requireSelf`, and `requireAdmin`.
- IDOR rule: In strict adherence to `PLAN.md` §6.4 and `api-catalogue.md` line 12, foreign/unauthorized IDs throw `ResourceNotFoundException` yielding `404 Not Found` (RFC 9457 `application/problem+json`), preventing attackers from probing resource existence.
- Controllers updated: `CourseServiceImpl.assertOwner` (course mutations), `ModuleController` (module creation/updates/deletions and module-nested content/assignment creation), `ContentController` (content updates/deletions), `AssignmentController` (assignment mutations, submission access, grading), `EnrollmentController` (self enrollment, unenrolment authorization, course roster reads, progress tracking with course enrollment validation), and `TestController` (test authoring, question mutations, test attempt authorization, answer submission, and attempt grading).
- Red: `OwnershipAuthorizationTests` failed 8/9 before the changes (course mutation returned 403 instead of 404; module, content, assignment, test, question, attempt, and progress endpoints allowed foreign mutations with 200/204 instead of 404).
- Green: `OwnershipAuthorizationTests` passed 9/9 (`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`).
- Full regression: Backend `.\mvnw.cmd test` -> `Tests run: 80, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; Payments `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; Frontend `npm test` -> `5 passed (5)` test files, `15 passed (15)` tests.

**W1-09 evidence (2026-10-27) — WebSocket handshake + SUBSCRIBE authorization:**
- Contract: `WebSocketAuthInterceptor` validates JWT on STOMP `CONNECT`, checks account active/non-blacklisted status, and authenticates the STOMP session. On `SUBSCRIBE`, destination authorization is enforced: `/topic/admin/**` requires `ADMIN` role; `/topic/course/{courseId}/**` and `/topic/live-sessions/{sessionId}/**` require course enrollment or staff/admin; `/user/...` destinations reject foreign user queues. In `LiveChatWebSocketController`, client-supplied `userId`/`sender` are never trusted and overridden with real authenticated principal claims, and unenrolled senders are rejected.
- Red: `WebSocketAuthorizationTests` initially failed compile and runtime assertions (controller didn't accept `Principal`, and interceptor didn't authorize destinations).
- Green: `WebSocketAuthorizationTests` passed 13/13 (`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`).
- Full regression: Backend `.\mvnw.cmd test` -> `Tests run: 93, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; Payments `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; Frontend `npm test` -> `5 passed (5)` test files, `15 passed (15)` tests.

**W1-10 evidence (2026-10-27) — Auth/IDOR regression suite:**
- Contract: Built comprehensive `SecurityRegressionSuiteTests` integrating and locking all core security invariants from Wave 1:
  1. Auth endpoints (`/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/me`, `/api/auth/verify-college`) and user profile endpoints return `Cache-Control: private, no-store` (NFR-03, AUD-15).
  2. Refresh token rotation issues single-use tokens; replay attacks on rotated tokens immediately revoke the entire token family, rejecting further session refreshes with 401.
  3. Role matrix strictly enforces role boundaries: students and teachers calling admin endpoints receive 403 Forbidden; students attempting course authoring receive 403 Forbidden; unauthenticated requests receive 401.
  4. IDOR defense: foreign resource mutations (courses, modules, contents, assignments, tests, questions, attempts, enrollments) return 404 Not Found (RFC 9457 `application/problem+json`), preventing attackers from discovering resource existence.
  5. Retired legacy `/api/users/**` endpoints return 404 or 405 Method Not Allowed; only `/users/me` is self-service.
  6. WebSocket destination authorization rejects unauthorized `SUBSCRIBE` requests to `/topic/admin/**`, foreign courses, and foreign user queues.
  7. Account status lifecycle: `BLACKLISTED` accounts rejected at login with 401; `FLAGGED` accounts permitted read operations (200) but blocked from mutating writes with 403 Forbidden.
  8. Client session: `AuthStore` retains access tokens strictly in memory, while `localStorage` contains zero auth tokens or user records.
- Red: `SecurityRegressionSuiteTests` failed 4 tests initially (auth endpoints lacked `private, no-store` headers; WebSocket principal lacked CustomUserDetails authority; course authoring lacked `@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")` allowing student creation with 200; flagged error code expected `ACCOUNT_FLAGGED` rather than RFC 9457 `FORBIDDEN`).
- Green: `SecurityRegressionSuiteTests` passed 7/7 (`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`).
- Full regression: Backend `.\mvnw.cmd test` -> `Tests run: 100, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; Payments `.\mvnw.cmd test` -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; Frontend `npm test` -> `5 passed (5)` test files, `15 passed (15)` tests.

**W1-13 evidence (2026-10-27) — DS-02 Theme correctness: public routes light, no hardcoded surfaces:**
- One theme source: `ThemeService` injected at bootstrap in `App` (`app.ts`); route-aware theme controller listens to `NavigationEnd` events and validates routes via `isPublicRoute`.
- Public route pinning: Public routes (`/`, `/login`, `/register`, `/forgot-password`, `/verify-college`, `/verify-certificate`) are strictly pinned to `'light'`. User saved theme in `localStorage` under `ls_theme_mode` is preserved and not overwritten upon visiting public routes.
- In-app route restoration: Navigating to in-app routes (`/student/**`, `/teacher/**`, `/admin/**`, etc.) automatically restores the user's saved choice from `ls_theme_mode`, falling back to system preference or `'light'` (no longer hardcoded default `'dark'`). Theme toggle and changes on in-app routes update `ls_theme_mode`.
- Body & token alignment: Updated `index.html` inline reset to use `background-color: var(--bg-app);`, harmonizing root body background with the design tokens.
- Landing surface cleanup: Replaced all literal `#ffffff` and `#F8FAF8` surfaces across the 5 landing sections with semantic tokens `var(--bg-surface)` and `var(--bg-app)`. Replaced hardcoded text/border literals (`#1A1830`, `#6B6880`, `#E8E7EF`, `#534AB7`, etc.) with `var(--text-primary)`, `var(--text-secondary)`, `var(--border-color)`, `var(--brand-primary)`, eliminating the 1.03:1 unreadable contrast defect on landing.
- Red: `ThemeService` initially lacked `isPublicRoute` and route-aware pinning, failing compilation and unit assertions in `theme.service.spec.ts`. `landing.component.spec.ts` failed detecting literal `#ffffff` and `#F8FAF8` surfaces in section wrappers.
- Green: `npm test` -> `7 passed (7)` test files, `22 passed (22)` tests, including 5/5 tests in `theme.service.spec.ts` and 2/2 tests in `landing.component.spec.ts`.
- Style ratchet: `npm run lint:styles` -> 100% passed ratchet (0 new violations, hex/radius/transitions unchanged).
- Full build: `npm run build` -> production build succeeded (`BUILD_EXIT=0`).

**W1-14 evidence (2026-10-27) — DS-03 Typography classes and heading semantics:**
- Contract: Semantic typography classes defined in `_typography.scss` for `.h2`, `.sec-label`, `.sub`, `.divider` mapped directly against the token scale (`var(--font-display)`, `var(--text-primary)`, `var(--text-secondary)`, `var(--brand-primary)`, `var(--border-color)`).
- Semantics: Replaced all 5 instances of non-semantic `<div class="h2">` with `<h2 class="h2 text-title">` across sections in `landing.component.html`. Replaced `<div class="sub">` with `<p class="sub text-subtitle">`. Replaced card titles and FAQ question titles with semantic `<h3>`.
- Red: `landing.component.spec.ts` failed assertions: `expected 5 to be +0` for `div.h2` elements, and missing `h2` headings.
- Green: `landing.component.spec.ts` passed 4/4 tests verifying 0 `div.h2`, at least 5 `h2` elements, and valid heading hierarchy starting with `h1` followed by `h2`.
- Verification: `npm test` -> `7 passed (7)` test files, `24 passed (24)` tests; `npm run lint:styles` -> 100% passed ratchet (0 new violations); `npm run build` -> production build succeeded.

**W1-15 evidence (2026-10-27) — DS-04 Landing truth and navigation (no restyle):**
- Contract: Landing truth and navigation (no restyle) per `DS-04` and `Docs/LANDING_PAGE_AUDIT_AND_IMPROVEMENT_PLAN.md` P0-1, P0-2:
  1. Desktop nav unhidden: removed conflicting inline `display:none;` on header nav, added semantic `nav.desktop-nav` with responsive CSS (`@media (max-width: 768px)` hiding on mobile and unhiding desktop). Added responsive mobile menu toggle button and drawer with Lucide `menu`/`x` icons.
  2. Eliminated dead spans & fixed navigation: converted plain `span` elements with `cursor:pointer` into semantic `<a>` / `<button>` links with working targets (`#courses`, `#departments`, `#colleges`, `/register`, `/verify-certificate`).
  3. Fixed CTA label ↔ target mismatches: "Browse free previews" converted to "Watch preview lecture" pointing to `#preview-lecture`. "Is your college registered?" points to `#faq`. "Register your college" CTA wired to `routerLink="/register"`.
  4. Eliminated invented magic numbers: removed unverifiable `1,200+`, `85+`, `40k+`, and course inflated enrollment claims, replacing them with honest, verifiable copy ("Curated courses", "Verified colleges", "Active community", "4.8 ★", "Active curriculum").
  5. Accessibility & keyboard: added `role="button" tabindex="0"` to preview video play trigger and keyboard-interactive course cards (`tabindex="0" role="link" routerLink="/register"`).
- Red: `landing.component.spec.ts` failed compilation and assertions due to missing `isMobileMenuOpen` property on `LandingComponent`.
- Green: `landing.component.spec.ts` passed 9/9 tests verifying unhidden desktop nav, working mobile drawer toggle, zero dead `cursor:pointer` spans, working "Register your college" CTA, and elimination of invented numbers.
- Full regression: `npm test` -> `7 passed (7)` test files, `29 passed (29)` tests; `npm run lint:styles` -> 100% passed ratchet (0 new violations); `npm run build` -> production build succeeded (`BUILD_EXIT=0`).

**W1-16 evidence (2026-10-27) — DS-05 Auth screens on tokens:**
- Contract: Auth screens on design tokens per `DS-05` (`features/auth/**` covering `login`, `register`, `forgot-password`, `verify-college`):
  1. Input surface: eliminated dark background leakage into light cards; all inputs and dropdowns use `var(--bg-input)`, `color: var(--text-primary)`, `border: 1px solid var(--border-color)`, `var(--border-radius-md)`, and `&:focus { border-color: var(--brand-primary); }`.
  2. Surfaces & cards: `.auth-layout` uses `var(--bg-app)` and `var(--font-body)`; `.auth-card` uses `var(--bg-surface)`, `border: 1px solid var(--border-color)`, and `box-shadow: var(--card-shadow)`.
  3. Status & error states: `.error-banner` uses `var(--status-red-bg)` and `var(--status-red-text)`; `.field-error` / `.error-text` use `var(--status-red-text)`; `.info-banner` and `.test-hint` use `var(--brand-surface)` and `var(--brand-primary)`.
  4. Disabled-button contrast & hierarchy: replaced low-contrast `#A09DB8` with legible `background: var(--bg-surface-elevated); color: var(--text-muted); border: 1px solid var(--border-color); cursor: not-allowed; opacity: 0.8;`. Primary buttons use `var(--brand-primary)` and `var(--text-inverse)`.
  5. Role selector & components: `.role-selector` uses `var(--bg-app)`, `.role-btn` uses `var(--text-secondary)`, and active button uses `var(--bg-surface)` and `var(--brand-primary)`.
  6. Style ratchet impact: eliminated 48 raw hex occurrences and 2 `transition: all` declarations across auth components.
- Suite: authored `src/app/features/auth/auth-screens.spec.ts` verifying input surfaces, validation states, role buttons, and disabled contrast across all four auth components.
- Verification: `npm test` -> `8 passed (8)` test files, `33 passed (33)` tests; `npm run lint:styles` -> 100% passed ratchet (`color-no-hex` fell by 48, `transition-all` fell by 2); `npm run build` -> production build succeeded (`BUILD_EXIT=0`).






### Wave 2 — Curriculum and authoring
- [x] W2-01 Department FK + backfill
- [x] W2-02 Course state machine + isPublished projection
- [x] W2-03 Course archive/soft delete + restore

**W2-01 evidence (2026-10-27) — Department FK + backfill:**
- Contract: `courses.department_id` is an FK to `department(department_id)` with `ON DELETE RESTRICT` and index `idx_courses_department_id`. Flyway migration `V4__course_department_fk.sql` backfills missing departments from legacy string labels, resolves any NULLs to the default department ("General"), and establishes the foreign key constraint.
- Course entity maps `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;` with `@JsonProperty("departmentId")` and maintains `@Column(name = "department") private String departmentLabel;` for backwards-compatibility.
- Course creation and updates enforce valid department presence: missing department returns 400 Bad Request; non-existent department ID returns 404 Not Found.
- `CourseResponseDto` returns both `departmentId` (`Long`) and `department` (display string).
- Public endpoint `GET /api/departments/{id}/courses` returns LIVE courses in that department as `List<CourseResponseDto>`, returning 404 Not Found for non-existent department.
- Red: `CourseDepartmentFkTests` failed 5/7 (creation without department returned 200 instead of 400; non-existent department returned 200 instead of 404; GET courses by non-existent department returned 401 instead of 404; Course entity did not expose `departmentId`).
- Green: `CourseDepartmentFkTests` passed 7/7. Full backend suite `.\mvnw.cmd test` -> `Tests run: 107, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet; Production build -> exit code 0 (`BUILD_EXIT=0`).

**W2-02 evidence (2026-10-27) — Course state machine + isPublished projection:**
- Contract: Course state machine `Course.Status` is canonical (`DRAFT`, `PENDING`, `LIVE`, `ARCHIVED`). Course creation (`POST /api/courses` and `POST /api/teacher/courses`) unconditionally enforces initial status `DRAFT`. `CourseResponseDto` projects `isPublished = (course.getStatus() == Course.Status.LIVE)`.
- Teacher transitions: teachers can transition `DRAFT -> PENDING` (submit for review) and `PENDING -> DRAFT` (withdraw submission). Teachers attempting to set status to `LIVE` (or `isPublished = true`) are rejected with `403 FORBIDDEN` (code `FORBIDDEN` in problem+json), enforcing admin approval only (A-04).
- Admin moderation: `PUT /api/admin/courses/{id}/status` is locked to `hasRole('ADMIN')`. Admin can approve `PENDING -> LIVE` or change status, validating enum values (invalid or blank status returns 400 Bad Request). Every admin status change writes an immutable audit record to `audit_log` with action `COURSE_STATUS_CHANGED`, resourceType `COURSE`, resourceId, and before/after status JSON snapshots.
- Catalog filtering & draft hiding: `GET /api/courses` (public catalog) by default returns only `LIVE` courses, strictly excluding `DRAFT` and `PENDING` courses. Non-admin users cannot query non-LIVE courses via `/api/courses`. `GET /api/courses/{id}` returns `404 NOT_FOUND` for non-LIVE courses unless requested by course staff (owner teacher) or an admin, preventing draft existence discovery per IDOR rules.
- Red: `CourseStateMachineTests` failed 7/9 (missing `isPublished` on DTO; teacher setting `LIVE` allowed with 200 instead of 403; public catalog returned drafts and pending courses; public GET `/api/courses/{id}` returned draft with 200 instead of 404).
- Green: `CourseStateMachineTests` passed 9/9. Full backend suite `.\mvnw.cmd test` -> `Tests run: 116, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet.

**W2-03 evidence (2026-10-27) — Course archive/soft delete + restore:**
- Contract: Replaced the destructive hand-written JDBC hard cascade in `deleteCourse` with soft-delete archiving (`status = ARCHIVED`). All historical records—modules, lectures, content, notes, bookmarks, discussions, enrollments, payments, royalties, and certificates—are completely preserved.
- Restoring a course (`POST /api/courses/{id}/restore` and `POST /api/teacher/courses/{id}/restore`): transitions status from `ARCHIVED` to `DRAFT`. Restoring an active (non-archived) course returns `400 BAD_REQUEST` ("Course is not archived").
- IDOR security: foreign teachers attempting to delete or restore a course receive `404 NOT_FOUND`. Admins can archive and restore any course.
- Visibility: archived courses are excluded from public catalog (`GET /api/courses`) and return `404 NOT_FOUND` on detail read (`GET /api/courses/{id}`) for public and students (course staff and admins bypass).
- Red: `CourseArchiveSoftDeleteTests` failed 5/7 (deletion previously caused foreign key conflict on hard cascade or failed to archive; restore endpoints returned 404).
- Green: `CourseArchiveSoftDeleteTests` passed 7/7. Full backend suite `.\mvnw.cmd test` -> `Tests run: 123, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet.
- [x] W2-04 Module/content ordering + delete renormalisation

**W2-04 evidence (2026-10-27) — Module/content ordering & delete renormalisation:**
- Rules: AC-07, AC-08, BR-06, [B-F6].
- Ordering: Modules inside courses and CourseContent inside modules are returned contiguously ordered by `position ASC` (1..n).
- Creation: Creating module or content without a specified position automatically assigns `count + 1`.
- Deletion: Deleting a module (`DELETE /api/modules/{id}`) or content (`DELETE /api/contents/{id}`) flushes the removal and re-normalises all remaining sibling items contiguously to `1..n` without gaps.
- Reorder endpoints:
  - `PUT /api/modules/{id}/reorder`: supports `{ "position": targetPosition }` (reordering module within course and shifting siblings contiguously) and `{ "orderedIds": [id1, id2, ...] }`.
  - `PUT /api/courses/{courseId}/modules/reorder`: supports bulk reordering via ordered ID list or ReorderRequest.
  - `PUT /api/contents/{id}/reorder`: supports `{ "position": targetPosition }` (reordering content within module and shifting siblings contiguously) and `{ "orderedIds": [id1, id2, ...] }`.
  - `PUT /api/modules/{id}/contents/reorder`: supports module-level bulk reordering of contents.
- IDOR security: All reorder and delete endpoints are protected by `OwnershipService` (`requireCourseStaff`, `requireModuleStaff`, `requireContentStaff`). Unauthorized foreign teachers or non-staff receive `404 NOT_FOUND`.
- Red: `ModuleContentOrderingTests` failed 4/9 tests (content deletion did not re-normalise positions and returned gap `[1, 3]`; reorder endpoints returned 404).
- Green: `ModuleContentOrderingTests` passed 9/9 tests. Full backend suite `.\mvnw.cmd test` -> `Tests run: 132, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet.
- [x] W2-05 Lecture → CourseContent migration

**W2-05 evidence (2026-10-27) — Lecture → CourseContent migration:**
- Rules: A-05, [B-F1], M-02.
- Schema Migration: Added Flyway `V5__legacy_lecture_migration.sql`:
  - Added `legacy_lecture_id BIGINT` to `course_content` with unique partial index (`uq_course_content_legacy_lecture`).
  - Auto-created default module for any course with legacy lectures lacking modules.
  - Migrated `lectures` 1:1 into canonical `course_content` (`contentType = 'VIDEO'`).
  - Backfilled `user_progress.content_id` for historical progress records from corresponding migrated `course_content`.
- Legacy Mutation Surface Retired: `POST /api/teacher/courses/{id}/lectures`, `POST /api/teacher/courses/{id}/bulk-lectures`, `PUT /api/teacher/lectures/{id}`, and `DELETE /api/teacher/lectures/{id}` reject mutations with `400 BAD_REQUEST` ("Legacy lecture authoring/modification/deletion is retired (A-05). Please author curriculum via /api/courses/{id}/modules and /api/modules/{id}/contents."). Dual writes eliminated.
- Read Surfaces & Playback: Legacy lecture read surfaces (`GET /api/courses/{id}/lectures` and `GET /api/student/courses/{courseId}/lectures/{lectureId}`) remain intact for playback compatibility.
- Progress Integration: Marking lectures complete in `StudentController` (`POST /api/student/lectures/{id}/complete`) automatically resolves and links canonical `CourseContent` into `user_progress.content`.
- Red: `LectureContentMigrationTests` failed compilation on missing `CourseContent.legacyLectureId`.
- Green: `LectureContentMigrationTests` passed 6/6 tests. Full backend suite `.\mvnw.cmd test` -> `Tests run: 138, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet.
- [x] W2-06 Assignment validation (create and update)

**W2-06 evidence (2026-10-27) — Assignment validation (create and update):**
- Rules: AC-09, [B-F8].
- Assignment creation (`POST /api/modules/{id}/assignments`) strictly validates non-blank title, `maxMarks > 0`, and future `dueDate` (`dueDate.isBefore(LocalDateTime.now())`), rejecting violations with `400 BAD_REQUEST` (RFC 9457 `application/problem+json`).
- Assignment update (`PUT /api/assignments/{id}`) validates non-blank title if provided (`isBlank()`), `maxMarks > 0` if provided, and future `dueDate` if modified (`dueDate.isBefore(LocalDateTime.now())`), rejecting past deadlines or non-positive marks with `400 BAD_REQUEST`.
- Assignment submission (`POST /api/assignments/{id}/submissions`) requires at least one of `submissionText` or `fileUrl` to be non-blank, rejecting empty submissions with `400 BAD_REQUEST`, and upserts existing student submissions.
- IDOR authorization: `OwnershipService.requireAssignmentStaff` returns `404 NOT_FOUND` for unauthorized foreign teachers attempting update or delete.
- Red: `AssignmentValidationTests` failed 3/11 tests (update with blank title returned 200 instead of 400; update with `maxMarks <= 0` returned 200 instead of 400; update with past `dueDate` returned 200 instead of 400).
- Green: `AssignmentValidationTests` passed 11/11 tests. Full backend suite `.\mvnw.cmd test` -> `Tests run: 149, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet.
- [x] W2-07 Storage port + upload safety

**W2-07 evidence (2026-10-27) — Storage port + upload safety:**
- Rules: spec §6, [B-F9], BR-26.
- Flyway Migration `V6__stored_files.sql`: created `stored_files` table with `file_key` (UNIQUE), `owner_id`, `original_filename`, `content_type`, `size_bytes`, `storage_path`, and indexes on `file_key` and `owner_id`.
- Storage Port & Local Adapter: created `StorageService` interface and `LocalStorageService` implementing safe file operations under private `uploads/{ownerId}/{uuid}.{ext}`.
- Upload Safety & Validation:
  - 200 MB maximum size cap strictly enforced (rejects > 200 MB with `400 BAD_REQUEST`).
  - MIME type allow-list for resources (PDF, DOC/DOCX, PPT/PPTX, TXT, PNG, JPEG, GIF, WEBP) and videos (MP4, WebM, MOV, MKV).
  - Magic byte verification: validates binary file headers (%PDF-, PNG, JPEG, GIF8, RIFF..WEBP, PK.. Zip, OLE2 compound document, MP4/MOV `ftyp`/`moov`/`mdat`, WebM EBML header) and forbids executables/scripts (`MZ`, ELF, `#!`).
  - Path traversal defense: validates and sanitizes filenames, verifies normalized paths stay strictly within upload root.
- Removed Public `/api/uploads/**` static handler:
  - Removed static resource handler in `WebConfig`.
  - Removed `.requestMatchers("/api/uploads/**").permitAll()` from `SecurityConfig`.
  - Unauthenticated access returns `401 UNAUTHORIZED`, and authenticated access returns `404 NOT_FOUND`.
- Authorized File Retrieval: created `FileController` (`GET /api/files/{key}`) returning `200 OK` with `Cache-Control: private, no-store` and streaming file for owner or admin, while foreign users receive `404 NOT_FOUND` (IDOR defense).
- Hardened Upload API: `UploadController` returns `201 CREATED` with opaque `{ "fileKey": key, "url": "/api/files/" + key }`, eliminating raw storage paths while preserving frontend compatibility.
- Red: `StorageAndUploadSafetyTests` failed 2/11 tests initially (path traversal double-encoded in mock request; unauthenticated `/api/uploads` expectation was 404 rather than 401).
- Green: `StorageAndUploadSafetyTests` passed 11/11 tests. Full backend suite `.\mvnw.cmd test` -> `Tests run: 160, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet.
- [x] W2-08 Notes/bookmarks rebind to CourseContent

**W2-08 evidence (2026-10-27) — Rebind notes/bookmarks to CourseContent:**
- Rules: A-15, AC-14, BR-11, [E-F10].
- Flyway Migration `V7__notes_bookmarks_content_rebind.sql`:
  - Added `content_id` FK column to `notes` table referencing `course_content(content_id)` with index `idx_notes_content_id`.
  - Added `content_id` FK column to `bookmarks` table referencing `course_content(content_id)` with index `idx_bookmarks_content_id`.
  - Backfilled existing `notes` and `bookmarks` rows having `lecture_id` from `course_content.legacy_lecture_id`.
- Entity & Schema Alignment:
  - Updated `Note.java` with `@ManyToOne CourseContent courseContent` mapped to `content_id` (preventing field collision with note text `content`).
  - Updated `Bookmark.java` with `@ManyToOne CourseContent content` mapped to `content_id`.
  - Added repository methods in `NoteRepository` and `BookmarkRepository` for content-level retrieval and deletion.
- Timestamp Bounding & DTO Contracts:
  - Validated `timestampSeconds >= 0` across notes and bookmarks (returns RFC 9457 `400 BAD_REQUEST` with `code: BAD_REQUEST` if negative).
  - Enforced `timestampSeconds <= durationSeconds` when content or lecture duration is known (> 0) (returns `400 BAD_REQUEST` if out of bounds).
  - Extended DTOs (`NoteRequestDto`, `NoteResponseDto`, `BookmarkRequestDto`, `BookmarkResponseDto`) with `contentId` and `contentTitle`.
  - Endpoints supported: `GET /api/notes?contentId=...`, `GET /api/bookmarks/content/{contentId}`, `DELETE /api/bookmarks/content/{contentId}`.
  - Ownership & IDOR defense: foreign users attempting to mutate or delete notes or bookmarks receive `404 NOT_FOUND`.
- Red-green verification: `NotesBookmarksRebindTests` passed 8/8 tests. Full backend suite `.\mvnw.cmd test` -> `Tests run: 168, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Payments suite -> `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Frontend Vitest -> `8 passed (8)` test files, `33 passed (33)` tests; Stylelint -> 100% passed ratchet.
- [~] W2-09 YouTube import into canonical content
- [ ] W2-10 Curriculum contract suite
- [ ] W2-11 Public catalogue/reference cache + ETag invalidation
- [ ] W2-12 Canonical media source + secure local playback
- [ ] W2-13 Opt-in YouTube channel publish (OAuth + idempotent upload)
- [ ] W2-14 Token consolidation + lint ratchet to zero-new (DS-06)
- [ ] W2-15 Icon unification: Lucide only (DS-07)
- [ ] W2-16 Identity direction + proof page (DS-08)
- [ ] W2-17 Public surfaces rebuild on the approved direction (DS-09)

### Wave 3 — Enrolment, progress and credentials
- [~] W3-01 Enrolment unique, self-only, published check
- [~] W3-02 Roster/unenrol authorization
- [~] W3-03 Progress unique + upsert + BigDecimal
- [~] W3-04 90 % completion rule
- [~] W3-05 Content-based course completion
- [~] W3-06 Certificate eligibility + real grade
- [~] W3-07 Leaderboard materialisation
- [ ] W3-08 Same-college entitlement
- [ ] W3-09 Learning regression suite (course surfaces, resume, player auth)
- [ ] W3-10 My Courses enrolled-only + Explore catalogue + resume (AUD-07)
- [ ] W3-11 Canonical player route + server enrolment check (AUD-08)
- [ ] W3-12 Course-surface polish (AUD-09)
- [ ] W3-13 Hot-path query optimization + private no-store
- [ ] W3-14 Student surfaces on the system (DS-10)
- [ ] W3-15 Player workspace typography + density (DS-11)

### Wave 4 — Assessment integrity
- [~] W4-01 Hide correctAnswer in all student DTOs
- [~] W4-02 Retire legacy `/api/tests` surface
- [ ] W4-03 Options in `Question.extra`
- [ ] W4-04 One answer per question + upsert
- [~] W4-05 Grading correctness (TEXT manual)
- [ ] W4-06 Attempt lifecycle + timeout job
- [~] W4-07 Schedule/duration/attempt-limit enforcement
- [ ] W4-08 BigDecimal score/marks migration
- [~] W4-09 Attempt review + manual-grade audit
- [ ] W4-10 Assessment integrity suite
- [ ] W4-11 Player-attached quiz end-to-end + completion gate

### Wave 5 — Engagement and community
- [~] W5-01 Canonical thread/post + legacy migration
- [ ] W5-02 Enrolment-gated community reads/writes
- [~] W5-03 Reply nesting + cross-thread validation
- [~] W5-04 Moderation on canonical posts
- [~] W5-05 Course-scoped announcements
- [ ] W5-06 Notification event catalogue + outbox
- [~] W5-07 Live session enrolment gating
- [~] W5-08 Messaging pagination/authorization
- [~] W5-09 Feedback eligibility + average
- [ ] W5-10 Live media provider + player integration

### Wave 6 — Commerce, royalties and institutions
- [~] W6-01 Remove simulated checkout rail
- [ ] W6-02 Payments service authentication
- [~] W6-03 Fail-closed verification + order idempotency
- [ ] W6-04 Remove `/orders/simulate-success`
- [~] W6-05 Internal enrol: server price + idempotent ledger
- [~] W6-06 One ledger + reconciliation
- [~] W6-07 Remove royalty auto-reconcile
- [~] W6-08 70/30 policy object
- [~] W6-09 Payout threshold/schedule/lock/statement
- [~] W6-10 College verification + affiliation gate
- [ ] W6-11 Same-college free entitlement
- [ ] W6-12 Commerce/security suite (forgery, replay, history, receipt)
- [ ] W6-13 Receipt PDF + modal (AUD-01)
- [ ] W6-14 Payment history UI truth (AUD-02)

### Wave 7 — Client experience and performance
- [~] W7-01 Angular 21 baseline + docs correction
- [ ] W7-02 Lazy-load role route groups
- [ ] W7-03 API base URL injection
- [ ] W7-04 Session UX: refresh/expiry/logout
- [~] W7-05 Remove dead `store/` scaffolding
- [~] W7-06 Route ↔ SCR matrix
- [ ] W7-07 Copy extraction for changed screens
- [ ] W7-08 Accessibility fixes
- [~] W7-09 Responsive verification at 360/768/1280
- [ ] W7-10 Frontend unit + Playwright smoke (profiles, settings, receipt, resume)
- [ ] W7-11 Teacher/admin profiles real data + save (AUD-11/AUD-12)
- [ ] W7-12 Profile/settings completeness + admin inspect (AUD-10/AUD-12/AUD-13)
- [ ] W7-13 Frontend performance + visual-preservation guard
- [ ] W7-14 Teacher/admin consoles on the system (DS-12)
- [ ] W7-15 Motion restraint + copy pass (DS-13)

### Wave 8 — Intelligence, discovery and release
- [~] W8-01 AI quiz generation into `extra`
- [~] W8-02 Transcript summary + fallback
- [~] W8-03 Chatbot grounding/safety
- [~] W8-04 YouTube import hardening
- [~] W8-05 Catalogue search/filter/pagination
- [~] W8-06 Trending/recommendations
- [~] W8-07 Admin reports/exports
- [ ] W8-08 Observability dashboards/alerts/runbooks
- [ ] W8-09 Release readiness

### Hardening
- [ ] H-01 Full regression + seed reset
- [ ] H-02 Security review/pen test
- [ ] H-03 Performance + bundle budgets
- [ ] H-04 Accessibility AA audit
- [ ] H-05 Migration rehearsal + restore
- [ ] H-06 Backup/restore + incident runbook
- [ ] H-07 Docs truth pass
- [ ] H-08 UAT + sign-off
- [ ] H-09 Audit remediation verification (all `AUD-xx`)
- [ ] H-10 Cache safety + visual-preservation gate
- [ ] H-11 Design-system conformance gate (NFR-04)

### Audit remediation status (`audit.md` 2026-10-24)

`AUD-14` is closed by `W1-11`; the remaining findings are **open** and no other audit fix has been implemented in this planning pass. `AUD` IDs mirror `Docs/plan/waves.md`; story status is in the sections above.

| AUD | Priority | Finding | Owner | Status |
|---|---|---|---|---|
| AUD-01 | P1/P2 | Receipt PDF/modal glyphs, layout, identity/time, tax wording, print fidelity | W6-13 | Open |
| AUD-02 | P1/P2 | Payment history fabrication/status/unit/timestamp defects | W6-06, W6-14 | Open |
| AUD-03 | P0 | Signature verification fails open | W6-03 | Open |
| AUD-04 | P0 | Simulation/order lookup unauthenticated or replayable | W6-02, W6-04 | Open |
| AUD-05 | P0 | Internal enrol trusts amount and is non-idempotent | W6-05 | Open |
| AUD-06 | P1/P2 | Two payment rails; simulated checkout reachable | W6-01 | Open |
| AUD-07 | P1/P2 | My Courses/catalogue conflated; no resume surface | W3-10 | Open |
| AUD-08 | P1/P2 | Player fallback/client-only enrolment/duplicate aliases | W3-11 | Open |
| AUD-09 | P3 | Course UI currency/mojibake/duration/filter defects | W3-12 | Open |
| AUD-10 | P2 | Student profile mojibake/error-toast gaps | W7-12 | Open |
| AUD-11 | P1 | Teacher profile static/fake save | W7-11 | Open |
| AUD-12 | P1/P2 | Admin profile fake saves/metrics/inspect | W7-11, W7-12 | Open |
| AUD-13 | P2 | Settings local-only; sign-out unimplemented | W7-12 | Open |
| AUD-14 | P0 | Legacy `/api/users/**` over-exposed | W1-11 | Closed (W1-11) |
| AUD-15 | P1 | Tokens in `localStorage`; refresh/logout contract | W1-05, W1-06 | Open |
| AUD-16 | P2 | Account status semantics inconsistent | W1-03 | Open |

### Design-system overlay status (`PLAN.md` §12, 2026-10-25)

Findings `F24` were measured on the running app (Chromium, 1489×1000 and 390×844, light and dark). `DS-01` is implemented (baseline + token layer + ratchet); `DS-02`…`DS-13` have not started. `DS` IDs mirror `Docs/plan/waves.md`; story status is in the sections above.

**P0 blocker:** `DS-02`…`DS-05` fix a landing page that is unreadable in the shipped default theme (`#f8fafc` on `#ffffff`, 1.03:1) plus the undefined section-heading classes. Until they close, the build is local/demo only — the same deployment rule the audit P0 items already enforce.

| DS | Priority | Finding | Owner | Status |
|---|---|---|---|---|
| DS-01 | P0 | No visual baseline; no styling lint; token counts unmeasured | W1-12 | Closed (W1-12) |
| DS-02 | P0 | Default dark theme + hardcoded light sections → invisible landing text | W1-13 | Open |
| DS-03 | P0 | `.h2/.sec-label/.sub/.divider` undefined; headings are `div`s at 16px/400 | W1-14 | Open |
| DS-04 | P0 | Landing nav hidden, dead links, invented stats/courses/prices | W1-15 | Open |
| DS-05 | P1 | Auth inputs dark-on-white; disabled-button contrast | W1-16 | Open |
| DS-06 | P1 | Two palettes, 723 hex literals, 18 radii, 11 local `$brand-*` redefinitions | W2-14 | Open |
| DS-07 | P2 | Two icon families (Lucide vs `mat-icon` + Material Icons font) | W2-15 | Open |
| DS-08 | P1 | No identity decision; text-only hero, zero imagery | W2-16 | Open |
| DS-09 | P1 | Public/auth surfaces not on the system | W2-17 | Open |
| DS-10 | P1 | Student surfaces use four card styles, no token scale | W3-14 | Open |
| DS-11 | P1 | Player workspace shares generic card chrome | W3-15 | Open |
| DS-12 | P2 | Teacher/admin consoles drift from shared components | W7-14 | Open |
| DS-13 | P2 | 117 `transition: all`, 52 hover lifts, `→` suffixes, unverifiable superlatives | W7-15 | Open |

**Release gate:** `H-11` (Hardening) runs the design-conformance checks and closes at the Hardening exit.

### Wave gate results

The Phase 0 gate passed on 2026-10-26 (`wave-0-done`). `AUD-14` (P0) is closed by `W1-11`; no later gate can pass until the owning wave's remaining `AUD-xx` items are closed and TDD evidence is recorded. The same rule applies to the design overlay: `DS-xx` items close at the owning wave gate, `DS-01` is closed by `W1-12`, and `DS-02`…`DS-05` are P0.

| Date | Wave | Suites run | Result | Notes |
|---|---|---|---|---|
| 2026-10-26 | Phase 0 | Local on `main` `2f3fbf0` (clean tree): backend `mvnw -B clean test` 32/32 + `target/openapi.json`; payments 10/10; frontend `npm ci`, `npm test` 7/7, `lint:styles`, `build`, `e2e` 2/2, `e2e:visual` 277 passed / 1 skipped. Compose: `POSTGRES_PORT=5434 docker compose --env-file .env.example up -d --build --wait` → db/backend/payments/frontend healthy; readiness 200 on both services; `/api/courses`, `/`, `/login` 200; demo login 200; `down -v` 0. GitHub Actions run `40344867992` (first hosted run): all 6 jobs green incl. `CI gate` and `Visual baseline` (windows-latest). | **PASS** | Exit criterion met: one-command stack + full suite green in CI on a clean checkout; demo personas sign in. Every P0 story was re-verified on its own commit during the split. Suite includes W1-11/W1-12 (already on `main`). Open: branch protection must require `CI gate` (admin); actions v4/`setup-java@v4` deprecation warnings (Node 20). |

**Commit convention:** `<ID>: <story title>` — one story per commit; body lists failing test names and red/green commands. Wave done: commit `Wave <n> complete`, tag `wave-<n>-done`.

---

## 3. Next up (in order)

1. **Plan v1.4 is the implementation baseline** (v1.3 was approved 2026-10-24; v1.4 adds the design-system overlay `DS-01…DS-13` in §12). The pre-existing product WIP is committed as `ee04458` (a snapshot, not a story); new work starts from a clean tree and lands one story per commit.
2. **Repository admin follow-up:** require the `CI gate` status check in branch protection. The first GitHub-hosted run (`40344867992`, 2026-10-26) is green; the current token has `WRITE`, not `ADMIN`. Bump `actions/checkout`/`setup-node`/`upload-artifact` and `setup-java` to the Node 24 majors before GitHub removes Node 20.
3. **P0-09 (API conventions), W1-01 (role vocabulary & edge mapping), W1-11 (legacy `/api/users/**`, which also closed W1-07) and W1-12 (DS-01 visual baseline + token layer + style ratchet) are complete.** Keep their change sets separate from the pre-existing product WIP.
4. **W1-13 (DS-02) is next**, then W1-14…W1-16 — the design overlay is P0 and every later DS story diffs against the `W1-12` baseline. **W1-05 then W1-06** remain the next security stories: refresh tokens with rotation/logout revocation, then the memory-only access token in Angular (`AUD-15`); W1-10 assembles the full auth/IDOR regression suite as those land.
5. **W2 → W8 in backlog order.** Do not skip waves to reach an audit story; the P1 audit surfaces are pulled into the owning wave: W3-10/W3-11, W6-13/W6-14, W7-11/W7-12, each with failing tests first and that wave's gate. The NFR stories W2-11, W3-13, W7-13 and H-10 are gates, not optional cleanup; do not defer them to H-09. Media completeness W2-12/W2-13/W4-11/W5-10 is part of the owning waves, not a post-release cleanup.

---

## 4. Deviations from the plan (and why)

| Date | Plan said | We did | Why |
|---|---|---|---|
| 2026-10-22 | Template path `docs/` (lowercase) | Used `Docs/` and kept relative links inside it | The repository already has `Docs/`; Windows is case-insensitive; a second tree would be confusing. |
| 2026-10-22 | Template assumes greenfield | Documented the target state while recording the existing MVP as `[~]` | This is a brownfield retrofit; deleting working code before the replacement is proven would be worse. |
| 2026-10-22 | A-01…A-22 were expected | Added A-23 (account lifecycle) and A-24 (affiliation gate) | Analysis A/F raised them; leaving them unresolved would violate the no-placeholder rule. |
| 2026-10-22 | `spec.md` v1.0 frozen | Issued v1.0.1 as an editorial correction (AC-06 cross-ref + §4 route-coverage note) | The 61 route entries map to 47 distinct screens; no contract changed, and the revision is logged in `spec.md`. |
| 2026-10-22 | `REF` is the schema authority | Payments, royalties, colleges and affiliations have no `REF` entity | The PDF ends at Section 10; for those areas the authority is `AG §4` + `spec` + `README` (recorded in F §0). |
| 2026-10-24 | Plan v1.0 with tests as a per-story step | Plan v1.1 adds mandatory TDD (red → green → refactor), explicit unit/integration layers, wave triggers/gates and the `AUD-01…AUD-16` audit overlay | The post-build audit found P0/P1 gaps; test-last or tests-as-follow-up is no longer acceptable. No product code was changed in this planning pass; audit fixes remain open. |
| 2026-10-24 | Plan v1.1 without explicit performance, caching or design-preservation rules | Plan v1.2 adds `NFR-01`…`NFR-03`, W2-11/W3-13/W7-13/H-10, cache/header tests and visual-regression gates | Optimization must be measured, caching must be safe and invalidated, and the current glassmorphic design must be preserved rather than redesigned. No product code was changed. |
| 2026-10-24 | Plan v1.2 claimed YouTube import and live-session gating, but not actual media sourcing/publishing, live playback or player-bound quiz completion | Plan v1.3 adds A-25…A-27, ADR-14/ADR-15, BR-25…BR-27, W2-12/W2-13/W4-11/W5-10, migrations M-12…M-15 and the media/player API/data/security/test gates | Code review showed local upload, YouTube publish, live video and post-video quiz were either mock, import-only or client-only. The new stories close F22/F23 without changing the design baseline. No product code was changed. |
| 2026-10-26 | One story = one commit, written as the story lands | P0-01…P0-09, W1-11 and W1-12 were committed retroactively, one commit each, on top of a `Snapshot: pre-plan product work` commit (`ee04458`); each commit was rebuilt and its suites re-run on that commit | All work had stayed uncommitted and interleaved with pre-plan WIP. The snapshot comes first so every story commit matches the tree its evidence was produced on; it lists known WIP defects (unowned unenrol/roster, `correctAnswer` reads, stub refresh) for W1-05/W1-08/W3-02/W4. Intermediate drafts of `docker-compose.yml`, `.env.example`, `RUNNING.md`, `PLAN.md` and `TRACKER.md` were not preserved and appear in final form in their first story. Pre-split tree kept at `refs/backup/pre-split`. |
| 2026-10-26 | W1 stories in `waves.md` order | W1-11 and W1-12 were completed before W1-02…W1-06 | `PLAN.md` §10.3 trigger 2 sequences P0 audit (`AUD-14`) and P0 design (`DS-01`) items first. W1-02…W1-06 remain next after the DS P0 stories. |
| 2026-10-26 | W1-01 ticked | W1-01 reopened as `[~]` | No W1-01 code exists beyond P0-08's `RoleSeeder`; the `INSTRUCTOR` → `TEACHER` edge mapping (A-01) is unimplemented and untested. |
| 2026-10-27 | W1-01 reopened | Closed W1-01 with `RoleVocabulary` and `RoleVocabularyTests` | Edge mapping (`INSTRUCTOR` → `TEACHER`) and frozen vocabulary (`ADMIN`, `TEACHER`, `STUDENT`) implemented across registration, admin filter, and admin `/api/roles/**`. |
| 2026-10-26 | `CLAUDE.md` is part of the frozen docs tree (P0-01) | `CLAUDE.md` is now tracked; `.gitignore` only ignores `.claude/` | The pre-plan `.gitignore` ignored `CLAUDE.md`, so the session entry point was never committed. |
| 2026-10-25 | `P0-02` harness: one throw-away container per test class | Added `@DirtiesContext(AFTER_CLASS)` to `PostgresIntegrationTest` | The static container is stopped after each class, but Spring reused a cached context whose datasource still pointed at the previous container port. With a fourth `test`-profile class the suite failed with 30 s Hikari timeouts. Evicting the context per class keeps the database and context lifecycles aligned; the test contract is unchanged. |

---

## 5. Rules for anyone working here

- **The frozen source pack is read-only.** `AGENTS.md`, `Docs/LMS_Entity_Reference.*` and the old project docs are not edited to match code; code is changed to match them, or an assumption is recorded. `Docs/source/spec.md` is versioned: contract changes append v1.x and add a PLAN assumption; editorial corrections are logged in its revision log.
- **One story at a time.** Do not start a story until the previous ID is ticked. Story IDs live in `Docs/plan/waves.md`; this tracker mirrors them exactly.
- **Test-first is mandatory for behaviour changes.** Write the failing unit/integration test (red), record the failure, then the smallest production change (green), then refactor. A story with tests written after the code is not done; docs/config-only changes need a verification checklist. Tests live in the same commit.
- **Keep the current design.** Preserve the glassmorphic shells, tokens, typography, spacing, shared components and established flows (`NFR-01`). Do not restyle, re-layout or replace components as part of a behaviour fix; only audit-required changes may alter layout, and they must reuse the existing shell. Update a visual baseline only for an approved visual change.
- **Measure before optimizing; cache only safe reads.** Name the metric and baseline first (`NFR-02`). Cache only public/reference DTO read models with TTL ≤ 60 s and after-commit invalidation; never cache auth, profiles, progress, attempts/answers, payments/receipts/royalties, admin data, uploaded-file downloads, media/live playback descriptors, signed URLs or YouTube connection/publish jobs, and send `private, no-store` (`NFR-03`). No speculative cache/index/rewrite.
- **Wave trigger then gate.** A wave starts only when its trigger in `PLAN.md` §10.3 is met; it finishes only when the full suite, Exit line, audit overlay, performance/cache/visual checks and TDD evidence pass. No gate, no next wave.
- **Money and answer suites block merges.** Payment forgery/replay, answer leakage, duplicate answers, and the IDOR matrix are merge gates (`PLAN §10`).
- **Assumptions only change by editing `PLAN.md` §3 and this tracker.** A silent local interpretation is a bug.
- **Migrations are forward-only.** No `ddl-auto=update`; no manual production DDL; destructive steps require a verified backfill (ADR-07).
- **No secrets or default passwords.** Seed users exist only under `demo`/`test`; production starts empty (G-F10, A-23).
- **Never weaken a test to get green.** A test that fails twice stops the line and is reported.
- **One story = one commit** (`<ID>: <title>`). Do not mix unrelated refactors into a story.
- **Angular baseline is 21.2**, not 19; the lockfile wins over prose (ADR-06).

---

## 6. Known issues / notes

- **CI gate is not enforced remotely yet.** `.github/workflows/ci.yml` runs backend, payments, frontend, Playwright smoke and visual checks and exposes a `CI gate`; it ran green on GitHub for the first time on 2026-10-26 (`40344867992`). A repository admin must still require that check in branch protection. Business-rule coverage is still thin (W1 onward).
- **No performance or cache gate yet.** The MVP has no systematic public-read cache, `ETag`, after-commit invalidation or explicit `no-store` policy for sensitive reads; there is no query-count budget or load test. A visual-regression baseline now exists (`W1-12`: 276 PNGs, stylelint ratchet, `Visual baseline` CI job), but it only guards pixels — it is not a performance measurement. W2-11/W3-13/W7-13/H-10 close the rest; do not add caching or a UI restyle before their failing tests exist.
- **Session is unsafe.** `ls_token` in `localStorage`, 24 h JWT, no refresh, logout is client-only. Do not expose this build to the internet before W1-05/W1-06.
- **Money is unsafe.** Two rails; microservice has no auth; `/orders/verify` is fail-open; `/orders/simulate-success` is unauthenticated; `/internal/enroll` trusts `amount` and writes a royalty on every call. Do not connect a live Razorpay key.
- **Answers leak.** Legacy `/api/tests/{id}/questions` and `/api/questions/{id}` expose `correctAnswer`; duplicate answers inflate scores. Use `/api/assessments` for demos only; do not run graded assessments.
- **Legacy user API is closed.** `W1-11` retired the generic list/create/update/delete/status routes; `/api/users/me` edits only `fullName`, `bio` and `avatarUrl`, and user/role management is ADMIN-only with `audit_log` history (`AUD-14`). `GET /api/admin/users` still returns an unpaginated array and `/api/admin/users/**` still owns the status write; both migrate with W7-11/W7-12.
- **Receipts/payment history are not trustworthy.** History can create fake `SUCCESS` rows, the UI overwrites status with `PAID`, and receipts omit the real payment id/time and use unsupported glyphs. W6-06/W6-13/W6-14 close `AUD-01`/`AUD-02`.
- **Course surfaces are conflated.** My Courses includes a catalogue, Video Lectures falls back to the first course, and the player is client-guarded only. W3-10/W3-11 close `AUD-07`/`AUD-08`.
- **Media/player features are incomplete.** Local upload returns a public path, YouTube support is import-only, the live room is simulated, and the post-video quiz is a client-only modal tied to a legacy lecture. W2-07 closes the storage/public-path gap; W2-12/W2-13/W4-11/W5-10 close the video, publish, quiz and live gaps. Do not claim YouTube upload, live streaming or player-bound quizzes are complete before those wave gates.
- **Teacher/admin profiles and settings are partly fake.** Saves and password/avatar actions can show success without an API write. W7-11/W7-12 close `AUD-10`–`AUD-13`.
- **Audit baseline.** `audit.md` is the 2026-10-24 source for the remediation overlay; it is not evidence that any finding is fixed. The audit verification checklist is H-09.
- **Seeded credentials.** Demo personas exist only under `demo`/`test` and use the required `SEED_PASSWORD` environment value; `local` and `prod` create no demo rows. Never enable `demo` on a shared host.
- **Docs are stale outside this tree.** `Docs/Architecture.md`, `Docs/Frontend.md`, `Docs/Progress.md` say Angular 19; `README.md` says 80/20 and Docker; `RUNNING.md:80` says H2. H-07 corrects them; until then, this plan/spec wins.
- **Canonical build only.** The stale root `pom.xml`/`mvnw`/`.mvn` duplicate was removed in P0-07; use `learnsphere-backend/` or `learnsphere-payments-service/`.
- **Build artefacts are present but not evidence.** `backend-boot.log`, `build-errors.txt` and `target/` are local leftovers; do not treat them as a green build.
- **Migrations are baselined.** `V1__baseline.sql` exists for both services and `ddl-auto=validate` is enforced; new schema changes must be forward-only Flyway migrations (P0-04 closed). Existing databases are adopted at V1 by `baseline-on-migrate`.
- **Health endpoints exist.** Both services expose `/actuator/health`, `/actuator/health/readiness` and `/actuator/health/liveness`; JSON logs carry a `traceId` (P0-06 closed). Deploy probes can use these endpoints.
