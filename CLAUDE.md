# LearnSphere AI

College-first learning platform: Angular 21.2 SPA, Spring Boot 3.3.5 backend (Java 17), Razorpay payments microservice, PostgreSQL, NVIDIA NIM. It serves students, teachers and admins across courses, assessments, community, certificates and course commerce. The frozen contract is `Docs/source/spec.md`; the development plan is `Docs/PLAN.md`.

- **Start every session by reading [`TRACKER.md`](TRACKER.md).** It holds status, next steps and deviations. Update it before you finish.
- The plan: [`Docs/PLAN.md`](Docs/PLAN.md) (assumptions A-01…A-27 in §3). Backlog: [`Docs/plan/waves.md`](Docs/plan/waves.md). Endpoints: [`Docs/plan/api-catalogue.md`](Docs/plan/api-catalogue.md). Data: [`Docs/plan/schema-usage.md`](Docs/plan/schema-usage.md). Audit overlay: [`audit.md`](audit.md) → `AUD-01…AUD-16` in `PLAN.md` §11.
- Requirement detail: [`Docs/analysis/`](Docs/analysis/). Source material: [`Docs/source/`](Docs/source/). `project-docs-template/` is tooling — context only, never shipped or copied into product code.

## Hard rules

- **The frozen source pack wins.** `AGENTS.md`, the entity reference and the old project docs are not edited to match code. Code changes to match them, or an assumption is recorded in `PLAN.md` §3. `Docs/source/spec.md` is versioned: substantive changes append v1.x and add an assumption; editorial corrections are logged in its revision log.
- **No token in `localStorage` and no default secrets.** Access token lives in memory (15 min); refresh is an `HttpOnly` cookie (7 days). Seed users/passwords exist only under `demo`/`test`; production starts empty.
- **One model per domain.** `CourseContent` is canonical (not `Lecture`), the Razorpay microservice is the only checkout, and the main `payments` table is the single ledger. Do not add a second rail, curriculum model or royalty ledger.
- **Server owns the answer and the price.** Never expose `correctAnswer`, never accept a client price/score, and never enrol without the publication/entitlement check. Every mutating endpoint calls the ownership helper (`self`, `enrolled`, `staff`, `admin`).
- **Money and answers are idempotent.** One ledger row per gateway payment id; one answer per `(attempt, question)`; progress per `(user, content)`. Replays must be harmless.
- **Migrations are forward-only.** Flyway only; `ddl-auto=validate`; no manual production DDL. A destructive change needs a verified backfill.
- **Test-first is mandatory (TDD).** Write the failing unit/integration test first, confirm the intended red, implement the smallest change to green, then refactor. Tests are committed with the story; a story with tests written after the code is not done. Legacy refactors start with a characterisation test; defects start with a regression test.
- **Preserve the current design (`NFR-01`).** Keep the glassmorphic shells, tokens, typography, spacing, shared components and established flows. Do not restyle, re-layout or replace components as part of a behaviour fix. Only audit-required changes and the new player/live/quiz states may alter layout, and they must reuse the existing shell; a visual baseline changes only for an approved visual diff.
- **Measure before optimizing; cache only safe reads (`NFR-02`, `NFR-03`).** Name the baseline metric and budget first. Cache only public/reference DTO read models with TTL ≤ 60 s and after-commit invalidation; never cache auth, profiles, progress, attempts/answers, payments/receipts/royalties, admin data, uploaded-file downloads, media/live playback descriptors, signed URLs or YouTube connection/publish jobs, and send `private, no-store`. No speculative cache, index or rewrite.
- **Never weaken or delete a test to get green.** The payment, assessment-integrity, IDOR and cache-safety suites block merges. A test that fails twice stops the line.

## Commands

```bash
# install (first time)
cd learnSphere/frontend/learnsphere && npm ci

# run the stack (three terminals)
cd learnsphere-backend && ./mvnw spring-boot:run          # :8080
cd learnsphere-payments-service && ./mvnw spring-boot:run # :8081
cd learnSphere/frontend/learnsphere && npm start           # :4200

# database (Compose defaults to the demo profile and its documented personas)
docker compose up -d db
./scripts/reset-demo.sh                                    # drop, migrate + deterministic demo seed

# tests
cd learnsphere-backend && ./mvnw test
cd learnSphere/frontend/learnsphere && npm test
npx playwright test                                        # after W7-10

# build / typecheck
cd learnsphere-backend && ./mvnw package
cd learnSphere/frontend/learnsphere && npm run build
```

Ports: backend 8080, payments 8081, frontend 4200, PostgreSQL 5432 (`MOOC`, `learnsphere_payments`). The canonical builds are `learnsphere-backend/` and `learnsphere-payments-service/`; the root Maven duplicate was removed in P0-07.

## Workflow (story loop and wave gate)

- One story at a time, in `Docs/plan/waves.md` order. Do not start a story until the previous ID is ticked.
- Per story: write the failing test first (red) → smallest implementation (green) → refactor → unit + edge-case + integration + affected E2E tests → cache/query/visual checks where applicable → fast suite for everything built so far → tick `TRACKER.md` with test evidence → commit → push. Docs/config-only stories use a verification checklist instead.
- **Commit convention:** `<ID>: <story title>` (`P0-nn`, `W<wave>-nn`, `H-nn`). One story = one commit; the body lists the failing test names and the red/green commands.
- **Wave trigger/gate:** a wave starts only when its trigger in `PLAN.md` §10.3 is met. When the last story is ticked, run the full suite across Phase 0 and all earlier waves, check the wave's Exit criteria and audit overlay, record the result in `TRACKER.md`, then commit `Wave <n> complete` and tag `wave-<n>-done`. No next wave until it is green.
- Record any divergence from `PLAN.md` in `TRACKER.md` §4 with date and reason. Never silently "fix" the plan.

## Conventions

- **New endpoint** → add it to `Docs/plan/api-catalogue.md` first (method, path, perm, target, cache policy), then implement it with the ownership helper, problem+json errors, and a test per rule. Public reads get `ETag`/`Cache-Control`; sensitive reads get `private, no-store`.
- **New screen** → reuse the shared shell/components and design tokens (`src/styles/_variables.scss`); add the route under the correct lazy role group; capture the visual baseline at 360/768/1280 px before changing it; put copy in `core/content/*.ts`, never hard-code it.
- **New table/field/migration** → add a forward-only Flyway migration, update `Docs/plan/schema-usage.md`, and add the fixture/migration test.
- **New enum/state** → define it in one place, document the transitions in `PLAN.md` §6.5, and test illegal transitions.
- **Docs** → every claim cites `spec §x`, `REF p<n>`, `AG §n`, `[X-Fn]` or `code path:line`; no unresolved placeholders.
