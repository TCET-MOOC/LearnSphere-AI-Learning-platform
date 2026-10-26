# G — Analysis: client experience, delivery & operations

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** `spec §4` (61 route entries / 47 screens), `spec §6` (WebSocket, uploads), `spec §7` (all NFRs), `spec §8` (out of scope), `spec §9` · `AG §1` (stack), `AG §3` (JWT standards), `AG §7` (frontend) · `README.md` §7/§12 · `RUNNING.md` · Angular app (`package.json`, `angular.json`, `app.routes.ts`, `app.config.ts`, `core/**`, `styles/**`, `environments/**`, specs), backend test tree, build/delivery files.
**Date of analysis:** 2026-10-22

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n (not applicable to this file) |
| `AG §n` | root `AGENTS.md` section n |
| `README` / `RUNNING` | root `README.md` / `RUNNING.md` |
| `code <path>:<line>` | implementation evidence |
| `derived` | analyst inference, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| `spec §4` | All 61 route entries / 47 distinct screen contracts read; routes compared against them. |
| `spec §7` | Every NFR row checked against the repo. |
| `AG §1, §3, §7` | Read in full. |
| Frontend | `package.json`, `angular.json`, `tsconfig.json`, `app.config.ts`, `app.routes.ts`, `core/auth/**`, `core/guards/**`, `core/interceptors/**`, `core/services/{api,storage,websocket}.service.ts`, both payment services, `styles/**`, the two spec files. |
| Backend tests | Complete `src/test` tree (one file). |
| Delivery | Repository root, `.gitignore`, `.env.example`, both `pom.xml`, both `application.properties`. |
| Does NOT exist | lazy route loading, NgRx wiring, refresh-token client, CI workflow, Dockerfile/compose, Flyway/Liquibase, actuator/health, e2e suite, lint script, accessibility/performance test config. |

---

## 1. The contract

1. **Screens** — 61 route entries / 47 distinct screen contracts cover public, student, teacher and admin surfaces (`spec §4`). Navigation must expose them by role; route-level authorization is part of the contract (`spec §7` "role guards on every mutating endpoint").
2. **Session** — stateless JWT, access 15 minutes, refresh 7 days, **access token in memory only, never `localStorage`** (`spec §7`; `AG §3`). Refresh is an endpoint the client calls silently.
3. **Operations** — one-command local run, seeded demo users, a **health endpoint**, and **no secrets in the repo for production** (`spec §7`).
4. **Data** — PostgreSQL with **forward-only migrations in the target state** (`spec §7`).
5. **Performance/accessibility** — catalogue and dashboards p95 < 1 s on seed data; keyboard reachable, visible focus, AA contrast, responsive at 360/768/1280 px (`spec §7`).
6. **Deferred** — email, cloud storage, native apps, ExamGuard client, multi-tenancy, refunds (`spec §8`). None of these may be presented as delivered.

---

## 2. Client stack: actual vs documented

7. The running client is **Angular 21.2** (`package.json:18`; CLI 21.2.16 at `:35`), TypeScript 5.9, standalone bootstrap (`main.ts`), Vitest 4 + jsdom. The documentation still says **Angular 19** (`Docs/Architecture.md:5,9,39`; `Docs/Frontend.md:5`; `Docs/Progress.md:5,24`). **derived:** the docs describe an older architecture; the code is the only reliable source for the client.
8. Size: 180 TypeScript files, 68 templates, 67 stylesheets under `src/app` (measured 2026-10-22). Shared UI includes 15 components (`sidebar`, `topbar`, `teacher-sidebar`, `admin-sidebar`, `data-table`, `empty-state`, `stat-cards`, `status-pills`, `progress-bar`, `video-player`, `certificate-modal`, `confirm-dialog`, `avatar`, `ai-chatbot`, `footer`).
9. The design system is real: CSS custom properties for theme-reactive colours, spacing/radius/typography tokens, light/dark mode via `ThemeService` (`src/styles/_variables.scss`; `core/services/theme.service.ts`). This is the strongest part of the client.
10. **Routing is fully eager.** `app.routes.ts` imports every feature component at the top (`app.routes.ts:2-15` and the rest of the import block), and there is **no `loadComponent` or `loadChildren` anywhere**. All 61 route entries (`path:` count) ship in the initial bundle. The initial budget is 2 MB warning / 3 MB error (`angular.json:50-51`); per-component style budget is 500 kB / 1 MB (`:55-56`). **derived:** first paint on a slow connection is bounded by the whole application, and every feature is exposed to every user at download time. This conflicts with `spec §7` p95 < 1 s.
11. **State management is aspirational.** `src/app/store/` contains hand-written `actions/reducers/effects/selectors` folders, but `package.json` has **no `@ngrx` dependency**, `app.config.ts` has **no store provider** (`app.config.ts:20-25`), `store/app.state.ts` is empty, and no file imports from `@app/store`. **derived:** the folder is dead scaffolding; real state lives in services and component fields.
12. **API base URLs are hard-coded.** `environment.ts:3` is `http://localhost:8080/api`; the microservice client is `http://localhost:8081/api/payments` (`features/student/services/payments.service.ts:38`). There is no production environment file, no proxy config, and no injection token. **derived:** a production build points at localhost unless the file is edited before build.

---

## 3. Session and route protection

13. The JWT is stored in **`localStorage`** under `ls_token`, and the user snapshot under `ls_user` (`core/services/storage.service.ts:4,15,19,23,28`). This directly violates `spec §7` and `AG §3`. `AuthService.isAuthenticated()` only checks that a token string exists (`auth.service.ts:37`).
14. `AuthResponse` has a single `token` field (`core/auth/auth.models.ts:22-25`); there is no refresh token, no expiry metadata and no call to `/auth/refresh` anywhere in the client. The backend token lives **24 h** (`application.properties:14`). **derived:** the client cannot silently renew; when the token expires the user is logged out mid-session by the 401 interceptor.
15. `logout()` only clears storage (`auth.service.ts:89-91`); it does not call `POST /api/auth/logout`, so no server-side refresh-token revocation occurs. The backend endpoint exists but the client never uses it.
16. The 401 interceptor clears the session and navigates to `/login` (`core/interceptors/error.interceptor.ts:24-27`). Because the route guard checks token presence only, an **expired token still opens protected routes** until the first API call returns 401. **derived:** the user sees a flash of authenticated shell before being ejected.
17. The interceptor attaches `Authorization: Bearer <token>` to every `HttpClient` request (`core/auth/token.interceptor.ts:18`), and the WebSocket handshake passes the same token (`core/services/websocket.service.ts:42`). Role-based route lists are therefore UI hints; actual authorization must be server-side. The B analysis shows curriculum writes are authentication-only, so the hint is currently stronger than the enforcement.
18. `verifiedCollegeGuard` exists (`core/guards/verified-college.guard.ts:11`) but is **not referenced in `app.routes.ts` or any component**. The teacher-affiliation gate is dead code (also F finding F13).

---

## 4. UX surface and error handling

19. The shell renders one of three sidebars plus a shared topbar, and marks standalone pages (login/register/landing) to hide navigation (`app.ts`; `app.html`). Route count is 61, but it includes redirects and a wildcard, so it is not a one-to-one implementation of the 61 `SCR-` contracts. **derived:** screen coverage must be tracked per `SCR-` id, not by route count.
20. Error handling is centralized for HTTP (`error.interceptor.ts`) with a toast service, but there is no retry/backoff, no offline state, no optimistic-update rollback and no request cancellation policy. Components frequently catch errors locally as well. **derived:** the same failure can produce both a toast and a component message; there is no single error taxonomy.
21. Copy is hard-coded in templates (68 HTML files); there is no i18n or content/config layer. The wave Definition of Done requires copy in content/config, so this is a standing gap for every UI story, not a one-off.
22. Accessibility and responsive behaviour are claimed in docs (`README` design section; `RESPONSIVENESS_AUDIT.md`) but there is **no automated check**: no axe/Lighthouse config, no Playwright viewport suite, no CI. The 360/768/1280 px target in `spec §7` is therefore unverified.

---

## 5. Testing: one test per tier, both stale

23. **Backend:** exactly one test file, `OnlineLearningPlatfromApplicationTests.contextLoads()` (`src/test/java/.../OnlineLearningPlatfromApplicationTests.java:9-12`). `@SpringBootTest` boots the full context against the configured PostgreSQL URL (`application.properties:3-6`); there is no H2/test profile. **derived:** `mvn test` fails on a machine without the `MOOC` database, so the suite is neither hermetic nor a regression net.
24. **Frontend:** exactly one spec, `app.spec.ts`. It asserts that the rendered DOM contains an `<h1>` with text `Hello, learnsphere` (`app.spec.ts:16-21`), but `app.html` has **no `<h1>` at all** and never had that copy. **derived:** the only frontend test fails (or is skipped), and `npm test` is not a usable gate.
25. No unit tests cover auth, payment, progress, assessment or curriculum rules — the areas with the most severe findings in A–F. No integration test crosses the frontend → backend → microservice boundary. No contract test pins the `/internal/enroll` payload or the Razorpay webhook shape. No e2e framework is declared (`package.json` has no Playwright/Cypress).
26. `package.json` has `start`, `build`, `watch`, `test` and no `lint` script (`package.json:4-10`); there is no ESLint config. Prettier is present but unenforced. **derived:** formatting and lint drift are inevitable.

---

## 6. Build, delivery and environments

27. **No CI.** There is no `.github/` directory, no pipeline file and no branch protection evidence. Nothing runs the tests on push.
28. **No containers.** There is no `Dockerfile` and no `docker-compose*.yml`, although `README.md:387` lists Docker containers as the deployment target. A three-process local run (frontend 4200, backend 8080, microservice 8081) plus two PostgreSQL databases is documented manually in `RUNNING.md`. The repository root also contains a stale copy of the backend Maven build (`pom.xml`, `mvnw`, `.mvn/`) with **no `src/`**; the canonical backend is `learnsphere-backend/`. **derived:** running Maven at the root is a dead end, and the duplicate build files invite edits in the wrong place.
29. **No migrations.** Neither `pom.xml` declares Flyway or Liquibase; both services use `spring.jpa.hibernate.ddl-auto=update` (`learnsphere-backend/.../application.properties:6`; `learnsphere-payments-service/.../application.properties:16`). `spec §7` requires forward-only migrations in the target state. **derived:** schema changes are applied by Hibernate at boot, with no version history, no rollback story and no reviewable DDL.
30. **No health endpoint.** Neither `pom.xml` includes `spring-boot-starter-actuator`, and no controller exposes `/health`; `spec §7` requires one. There are no readiness/liveness probes, metrics or traces.
31. **Secrets and default credentials are in the repo.** The backend commits a JWT signing secret and DB password (`application.properties:4-5,13`), the internal API key (`:28`), and the microservice commits Razorpay key defaults plus `your_webhook_secret` (`learnsphere-payments-service/.../application.properties:19-21,24`). `DataSeeder` unconditionally creates `teacher@learnsphere.com`, `student@learnsphere.com` and `admin@learnsphere.com` with the literal password `Password@123` (`Config/DataSeeder.java:69-71,130,141`) in **every** environment. This violates "no secrets in the repo for production" (`spec §7`) and is a live takeover risk if the app is exposed.
32. **The runbook is stale.** `RUNNING.md:80` says the microservice uses H2 by default; `application.properties:12-16` defaults to PostgreSQL `learnsphere_payments`. There is no one-command start for all three processes, and no seed reset command.
33. `.env.example` documents only the NVIDIA key (`NVIDIA_NIM_API_KEY`); it omits DB, JWT, Razorpay, webhook and internal-key variables that the services actually read. **derived:** a new developer cannot configure a complete environment from the example.

---

## 7. Documentation and status drift

34. `Docs/Progress.md:3` declares **"Production Candidate (Feature-Complete MVP)"**. The evidence says otherwise: one stale test per tier, no CI, no migrations, no health endpoint, fail-open payment verification (F), unenforced authorization (B), and no same-college access (F). **derived:** the status document is not a reliable planning input; the frozen spec plus this analysis replace it.
35. `README.md` and the code disagree on three material points: royalty split 80/20 vs 70/30 (`README.md:53-54` vs `spec` AC-21 and code), Docker deployment vs no container files (`README.md:387`), and same-college free access vs no such rule (`README.md:62` vs F finding F15). `Docs/Architecture.md`/`Docs/Frontend.md` disagree on Angular 19 vs 21.2.
36. The `Docs/` corpus has overlapping narratives (`Architecture.md`, `Backend.md`, `Frontend.md`, `system_design.md`, `Progress.md`, four audit files) with no single authority. The frozen `Docs/source/spec.md` and this analysis tree are the replacement; old files remain as historical context only.

---

## 8. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|
| G1 | Docs say Angular 19; the app is Angular 21.2 | baseline must be the lockfile, not the prose |
| G2 | 61 route entries (47 distinct screens), all eagerly imported; no lazy loading; 2 MB/3 MB initial budget | p95 < 1 s NFR is at risk; bundle grows with every screen |
| G3 | JWT in `localStorage`, 24 h, no refresh token | violates the hardest security rule in `spec §7`/`AG §3` |
| G4 | `logout()` is client-only; no `/auth/logout` call | no revocation; stolen tokens stay valid for 24 h |
| G5 | Route guard checks token presence, not expiry | expired sessions render the authenticated shell until the first 401 |
| G6 | `store/` is dead scaffolding; no NgRx provider | misleading architecture; state is ad-hoc |
| G7 | API base URLs hard-coded to localhost (8080/8081) | production build points at the developer machine |
| G8 | One backend test (needs PostgreSQL) and one stale frontend test | no regression net for the highest-risk rules |
| G9 | No CI, no containers, no migrations, no health endpoint | four explicit `spec §7` NFRs unmet |
| G10 | Fixed demo passwords, JWT secret, DB password and internal key committed; seeder runs everywhere | production takeover risk |
| G11 | `RUNNING.md` H2 claim is false; 3-process setup has no one-command path | onboarding and "one-command local run" both fail |
| G12 | `Progress.md` calls the system feature-complete/production candidate | status reporting is unreliable |
| G13 | README claims 80/20, Docker and same-college free access; none match code | product promises diverge from behaviour |
| G14 | No actuator, structured logs, metrics, tracing or audit trail | incidents cannot be diagnosed or reconstructed |
| G15 | Accessibility/responsive targets have no automated verification | AA/360-768-1280 claims are unproven |
| G16 | `.env.example` omits every variable except the AI key | environment setup is undocumented |
| G17 | Root `pom.xml`/`mvnw` duplicate `learnsphere-backend/` but have no `src/` | contributors can run the wrong build |

## 9. Gaps — what the sources do NOT provide

- **NO DECISION** on the Angular baseline (19 vs 21) — resolved by taking the lockfile as truth and updating docs.
- **NO SPEC** for a production deployment topology beyond "cloud / Docker"; no domain, TLS, proxy or secret-manager contract.
- **NO SPEC** for log retention, metrics, alerting or audit events.
- **NO SPEC** for test coverage thresholds or which suites block a merge.
- **NO SPEC** for seed-data lifecycle (demo accounts are seeded, but there is no reset/disable switch).
- **NO SPEC** for accessibility conformance level beyond "AA" targets.

## 10. Open questions

None new. Every G item is either an explicit `spec §7` requirement (token in memory, migrations, health, one-command run, no production secrets) or an engineering choice recorded as an ADR in `PLAN.md` §4.7 (Angular 21 baseline, lazy loading, CI, containers, test pyramid). No G question needs a new working assumption.
