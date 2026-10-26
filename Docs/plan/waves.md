# LearnSphere AI — Wave backlog

The build broken into **stories**, wave by wave. Companion to [`../PLAN.md`](../PLAN.md) §11 (timeline, exit criteria, audit overlay) and [`../../TRACKER.md`](../../TRACKER.md) (what is done). Backlog version **1.4 · 2026-10-25**.

How to read a story:

- **ID**: `P0-nn` (Phase 0), `W<wave>-nn`, `H-nn` (hardening). `TRACKER.md` ticks these IDs exactly. Audit findings use `AUD-nn` and point at the owning story. Design findings use `DS-nn` and point at the owning story (`PLAN.md` §12).
- **Layer**: `DB` migration/seed · `API` · `UI` · `WK` worker/job · `TEST` · `OPS`.
- **Rules**: `BR-xx` = business rule in `PLAN.md` §6.6; `NFR-xx` = non-functional rule in §7.1 (design preservation, performance, cache safety, design conformance); `A-xx` = assumption in §3; `AC-xx` = acceptance criterion in `spec.md`; `[X-Fn]` = finding n in analysis X; `audit.md` = post-build audit finding (A–E sections); `DS-xx` = design-system story in §12.
- **Size**: S ≈ ≤ 2 dev-days · M ≈ 3–5 · L ≈ 6–10.
- **TDD is mandatory for every behaviour change.** Every behaviour-changing story starts with a failing test (red), then the smallest production change (green), then a refactor. Tests are committed with the story, and the commit/PR records the test names plus the red and green commands. A story whose tests were written only after the code is not done. Documentation/config-only stories use a verification script or checklist instead (`PLAN.md` §10.2).
- **Optimization is test-first too.** A cache/performance story starts with a failing hit/miss, invalidation, query-count or budget test and records the pre-change baseline. A UI change starts from a visual baseline. `NFR-01`…`NFR-04` apply to every story; no speculative cache/index/restyle.
- Every story meets the **Definition of Done** in `PLAN.md` §11: failing test first, unit test per rule, integration test for every write path, contract test where a boundary changes, permission/ownership assertion, accessibility and visual-regression check on changed screens, cache/no-store evidence where applicable, copy in content/config, migration tested, tracker ticked.
- Every wave has a **trigger** (entry criteria) and a **gate** (exit criteria) in `PLAN.md` §10.3. The next wave does not start until the full suite, wave Exit line and audit overlay are green and recorded in `TRACKER.md`.
- Commit message: `<ID>: <story title>` (e.g. `W1-05: Move access token to memory`); the body lists the failing test and the red/green commands.

Endpoints are specified in [`api-catalogue.md`](api-catalogue.md); data in [`schema-usage.md`](schema-usage.md).

---

## Audit remediation overlay (`audit.md`, 2026-10-24)

The audit is an overlay on the owning waves, not a late ninth wave. P0 items block the owning wave gate and any deployment beyond localhost; P1 items block release; P2/P3 items may move to H-09 only with a dated tracker deviation.

| AUD | Finding | Priority | Owner story/stories | Closes at |
|---|---|---|---|---|
| AUD-01 | Receipt PDF/modal: glyphs, layout, identity/time, tax wording, print fidelity, PII | P1/P2 | W6-13 | W6 gate |
| AUD-02 | Payment history: fabricated rows, status overwrite, lost failures, vocabulary, units, timestamps | P1/P2 | W6-06, W6-14 | W6 gate |
| AUD-03 | Verification continues after signature failure | P0 | W6-03 | W6 gate |
| AUD-04 | Open/replayable simulation; order lookup by path user id; no service auth | P0 | W6-02, W6-04 | W6 gate |
| AUD-05 | `/internal/enroll` trusts amount and writes on every call | P0 | W6-05 | W6 gate |
| AUD-06 | Two payment rails; main simulated checkout still reachable | P1/P2 | W6-01 | W6 gate |
| AUD-07 | My Courses includes catalogue; no enrolled-only continue/resume surface | P1/P2 | W3-10 | W3 gate |
| AUD-08 | No course-scoped resume; player fallback; client-only enrolment check; duplicate aliases | P1/P2 | W3-11 | W3 gate |
| AUD-09 | Course-card/currency/mojibake/duration/filter defects | P3 | W3-12 | W3 gate |
| AUD-10 | Student profile mojibake/placeholder/error-toast gaps | P2 | W7-12 | W7 gate |
| AUD-11 | Teacher profile static; save/avatar/password handlers fake | P1 | W7-11 | W7 gate |
| AUD-12 | Admin profile partly fake; save/password/avatar/inspect incomplete | P1/P2 | W7-11, W7-12 | W7 gate |
| AUD-13 | Student/teacher/admin settings local-only; sign-out unimplemented | P2 | W7-12 | W7 gate |
| AUD-14 | Legacy `/api/users/**` over-exposed; non-admin status/role changes | P0 | W1-11 | W1 gate |
| AUD-15 | Tokens in `localStorage`; logout/refresh contract not implemented | P1 | W1-05, W1-06 | W1 gate |
| AUD-16 | Account status semantics inconsistent | P2 | W1-03 | W1 gate |

## Design-system overlay (`PLAN.md` §12, 2026-10-25)

A visual audit of the running app produced `F24`: the token layer exists but nothing obeys it, and the shipped default theme makes four landing sections invisible (measured `#f8fafc` on `#ffffff`, 1.03:1). The overlay rides the wave whose window fits, in the order **legibility → system → identity → screens**. It adds no product behaviour.

P0 items block the owning wave gate and any deployment beyond localhost; P1 blocks release; P2 may move to H-11 only with a dated tracker deviation.

| DS | Finding | Priority | Owner story/stories | Closes at |
|---|---|---|---|---|
| DS-01 | No visual baseline; no lint on styling; token counts unmeasured (723 hex, 18 radii, 117 `transition: all`, 84 uppercase, 2 icon families) | P0 | W1-12 | W1 gate |
| DS-02 | Default theme dark + hardcoded light sections → landing text invisible (1.03:1); theme resolved in a service, not at bootstrap | P0 | W1-13 | W1 gate |
| DS-03 | `.h2/.sec-label/.sub/.divider` undefined; headings are `div`s at 16px/400 | P0 | W1-14 | W1 gate |
| DS-04 | Landing chrome: nav permanently `display:none`, ~12 dead `cursor:pointer` spans, invented stats/courses/prices, CTA label↔target mismatch | P0 | W1-15 | W1 gate |
| DS-05 | Auth inputs render `#0f172a` on a white card; disabled-button contrast | P1 | W1-16 | W1 gate |
| DS-06 | Two palettes, 723 literals, 11 local `$brand-*` redefinitions, 18 radii | P1 | W2-14 | W2 gate |
| DS-07 | Two icon families (341 Lucide vs 10 `mat-icon` + Material Icons font) | P2 | W2-15 | W2 gate |
| DS-08 | No visual identity decision; hero is a centred text stack with zero imagery in 69 templates | P1 | W2-16 | W2 gate |
| DS-09 | Public/auth surfaces not on the system | P1 | W2-17 | W2 gate |
| DS-10 | Student dashboard/cards/catalogue use four card styles and no token scale | P1 | W3-14 | W3 gate |
| DS-11 | Player workspace is the flagship surface but shares the generic card chrome | P1 | W3-15 | W3 gate |
| DS-12 | Teacher/admin consoles drift from the shared components | P2 | W7-14 | W7 gate |
| DS-13 | 117 `transition: all`, 52 hover lifts, `→` suffixes, unverifiable superlatives | P2 | W7-15 | W7 gate |

**Release gate:** `H-11` (Hardening) runs the design-conformance checks; it closes at the Hardening exit, not at a wave gate.

## Wave triggers (entry criteria)

All triggers for W1 onward also require the prior wave gate to be `PASS` in `TRACKER.md`. The first story's failing tests must exist before its production code.

| Wave | Trigger |
|---|---|
| Phase 0 | Plan v1.4 approved; P0-02 test harness/P0-03 CI first; audit fixes not started; baseline WIP identified but not staged |
| W1 | Phase 0 gate; failing tests for AUD-14/AUD-15/AUD-16 and the auth/IDOR matrix authored; `DS-01` visual baseline harness is the first UI story and the 47-screen baseline is captured before any design change |
| W2 | W1 gate; failing publication/ownership/curriculum, media-source/upload/playback, YouTube-publish contract and cache-invalidation/ETag tests authored; baseline p95 captured; `DS-08` direction decision scheduled before `DS-09` |
| W3 | W2 gate; failing AUD-07/AUD-08/AUD-09, enrolment/progress/resume, query-count and private no-store tests authored; `DS-10`/`DS-11` baselines captured before their changes |
| W4 | W3 gate; failing answer-leak/duplicate-answer/timeout and player-quiz completion-gate tests authored |
| W5 | W4 gate; failing community/WS-authorization, live-media gating/descriptor and feedback-cache invalidation tests authored |
| W6 | W5 gate; failing AUD-01…AUD-06 and commerce/no-store tests authored; no live Razorpay key |
| W7 | W6 gate; failing AUD-10…AUD-13, profile/settings, frontend and visual-baseline tests authored; `DS-12`/`DS-13` baselines captured |
| W8 | W7 gate; failing AI/search/report/observability and trending-cache tests authored |
| Hardening | W8 gate; all AUD items closed or tracker deviation approved; all DS items closed or moved to H-11 with a dated deviation; `audit.md` §G verification plan, H-10 cache/perf/visual checks and H-11 design-conformance checks scheduled |

---

## Phase 0 — Foundations

The walking skeleton: tests, CI, migrations, one-command run, health and config. Everything else depends on this.

| ID | Layer | Story | Done when | Size |
|---|---|---|---|---|
| P0-01 | OPS | Freeze the source pack and docs tree (`Docs/source`, `Docs/analysis`, `Docs/plan`, `PLAN.md`, `TRACKER.md`, `CLAUDE.md`) | Every claim cites spec/REF/AG/code; no unresolved placeholders; `TRACKER.md` mirrors this file | S |
| P0-02 | TEST | Replace the stale tests with a real harness: Testcontainers PostgreSQL profile for backend, fix `app.spec.ts`, add Vitest setup, and add the test-first PR checklist/evidence format | `mvn test` and `npm test` pass on a clean checkout without a local database; a deliberately failing sample test blocks green until production code is added | M |
| P0-03 | OPS | GitHub Actions CI: backend tests, frontend unit + build, Playwright smoke, artifact upload | Every push/PR runs the suite; a red run blocks merge | M |
| P0-04 | DB | Flyway baseline for `MOOC` and `learnsphere_payments`; switch both services to `ddl-auto=validate` | A clean database builds from migrations only; no Hibernate DDL | M |
| P0-05 | OPS | Docker Compose: two PostgreSQL databases, backend, payments service, frontend; one `docker compose up` | Stack starts with one command; healthchecks pass | M |
| P0-06 | OPS | Actuator health/readiness + structured JSON logs + correlation id | `/actuator/health` is green; logs carry `traceId`; no secrets logged | S |
| P0-07 | OPS | Config hygiene: remove the root duplicate `pom.xml`/`mvnw`/`.mvn`, move all secrets to env vars, add profiles (`local`, `test`, `prod`), complete `.env.example` | `git grep` finds no production secret literal; the documented env file starts the stack | M |
| P0-08 | DB | Seed profile separation: demo data only under `demo`/`test`; seed password from env; deterministic reset script | `prod` boots empty; `reset-demo` reproduces the same ids and personas | M |
| P0-09 | API | API conventions: RFC 9457 problem+json, stable `code`, validation errors, pagination envelope, OpenAPI document | Every new endpoint in W1+ uses the envelope; OpenAPI is generated in CI | M |

**Exit:** `docker compose up` + full suite green in CI on a clean checkout; a fresh developer can sign in with the documented seed users.

---

## Wave 1 — Identity, session and authorization

Screens/features: register, login, refresh, logout, forgot/reset password, college verification, profile, role/status admin, WebSocket handshake.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W1-01 | API | Freeze role vocabulary: `ADMIN/TEACHER/STUDENT`, seed idempotently, map `INSTRUCTOR` to `TEACHER` at the edge | `roles`, `user_roles`, `DataSeeder` | A-01; `spec §2` | S |
| W1-02 | API | Registration hardening: public registration creates `STUDENT`; teacher application flow; admin never self-registers | `POST /api/auth/register`, `POST /api/auth/verify-college`, `affiliation_requests` | A-11, A-24; AC-03 | M |
| W1-03 | DB/API | Account lifecycle `ACTIVE/FLAGGED/BLACKLISTED`; derive `is_active`; enforce at login and on writes; append-only admin audit list; make status writes admin-only and retire the ambiguous `isActive → BLACKLISTED` mapping | `users.status`, `PUT /api/admin/users/{id}/status`, `GET /api/admin/audit`, `audit_log` | A-23; AC-01; `audit.md` D8 (`AUD-16`); `PLAN §6.10` | M |
| W1-04 | API | JWT claims and lifetime: `userId`, `roles`, `dept_id`; 15 min; secret from env; reject invalid issuer/audience | `SecurityConfig`, `JwtService`, `application.properties` | AC-05, AC-06; A-09 | M |
| W1-05 | DB/API | Refresh tokens: 7-day `HttpOnly` cookie, rotation, family revoke, logout revoke | `refresh_tokens`, `POST /api/auth/refresh`, `POST /api/auth/logout` | A-09; AC-06; `audit.md` D7 (`AUD-15`) | M |
| W1-06 | UI | Client session: access token in memory, refresh interceptor with one retry, logout calls API, remove `ls_token`/`ls_user` | `AuthStore`, `token.interceptor.ts`, `error.interceptor.ts` | A-09; `audit.md` D7 (`AUD-15`); `[G-F3, G-F4]` | M |
| W1-07 | DB/API | Unique `(user_id, role_id)`; admin-only assign/revoke; remove self-service role writes | `user_roles`, admin-only role endpoints | AC-03; `audit.md` D6 (`AUD-14`); `[A-F2, A-F3]` | S |
| W1-08 | API | One ownership layer: `requireSelf`, `requireEnrolled`, `requireCourseStaff`, `requireAdmin`; apply to every mutating curriculum/test/enrolment controller | All course/module/content/test/enrolment controllers | AC-04; `[A-F7, B-F7, D-F2, C-F2]` | L |
| W1-09 | API | WebSocket authorization: validate handshake, authorize `SUBSCRIBE` per destination, reject foreign user queues | `/ws`, `WebSocketAuthInterceptor`, `LiveChatWebSocketController` | A-16; `[E-F15]` | M |
| W1-10 | TEST | Auth/IDOR regression suite: login/refresh/replay, role matrix, foreign-id 404, WebSocket subscribe denial, legacy user API denial, no token in `localStorage`, auth/profile responses `no-store` | `src/test/.../security`, frontend auth specs | AC-01…AC-06; NFR-03; `AUD-14`, `AUD-15`; `PLAN §10` | M |
| W1-11 | API/TEST | Lock down legacy `/api/users/**`: keep `/users/me` self-only; retire unsafe generic list/create/update/delete/status writes; admin management stays on admin-guarded endpoints (`/api/admin/users/**` and `/api/user-roles`); role/status changes admin-only and audited; no entity/password payloads | `UserAccountController`, `UserAdminController`, `users`, `user_roles`, `audit_log` | `audit.md` D6, E1 (`AUD-14`); AC-03; A-23 | M |
| W1-12 | UI/TEST | Visual baseline harness + token inventory: Playwright screenshots at 360/768/1280 px for the 47 screen contracts in both themes; move the existing `:root` custom properties into `src/styles/_tokens.scss` (values unchanged); add `stylelint` hex/radius/`transition: all` bans with a per-file ratchet; record the `F24` counts as the ratchet baseline. **No visual change.** | `e2e/visual`, `src/styles/_tokens.scss`, `stylelint.config.*`, `.github/workflows` | `PLAN §12`; NFR-04; `DS-01` | M |
| W1-13 | UI/TEST | Theme correctness: resolve the theme once at bootstrap; public routes (`/`, `/login`, `/register`, `/forgot-password`, `/verify-college`) pin to light; in-app routes use the saved choice; remove every hardcoded section/surface/text colour from public templates; body background agrees with the token. **First failing tests:** computed-style assertion that no public-route section uses a literal surface with a token text colour; theme E2E across reload; contrast ≥ 4.5:1 on the four landing sections | `theme.service.ts`, `landing.component.*`, `styles.scss`, `e2e/visual` | `DS-02`; A-28; NFR-04; `PLAN §12.1` D-1 | M |
| W1-14 | UI | Typography classes and heading semantics: define `.h2/.sec-label/.sub/.divider` against the token scale or delete them in favour of `text-title`/`text-eyebrow`/`text-subtitle`; replace `div.h2` with real `h2/h3`; enforce heading order per page | `landing.component.*`, `_typography.scss`, `styles.scss` | `DS-03`; NFR-04(b); `PLAN §12.1` D-2 | S |
| W1-15 | UI | Landing truth and navigation (**no restyle**): bind or remove the invented stats/courses/prices; unhide the nav and add a mobile menu; convert dead `cursor:pointer` spans to links or delete them; fix CTA label↔target mismatches; every clickable element reaches a real route or is removed | `landing.component.*`, `app.routes.ts` | `DS-04`; `Docs/LANDING_PAGE_AUDIT_AND_IMPROVEMENT_PLAN.md` P0-1, P0-2; `PLAN §12.1` D-5 | M |
| W1-16 | UI | Auth screens on tokens: login/register/forgot/verify-college input surface, label/hint/error states, disabled-button contrast, one button hierarchy; no hardcoded input colour | `features/auth/**` | `DS-05`; NFR-04; `PLAN §12.1` D-3 | S |

**Exit:** login/refresh/logout E2E green; no token in `localStorage`; IDOR matrix green; auth/profile responses are `private, no-store`; a `FLAGGED` user is read-only; a student cannot author; non-admin users cannot list, mutate, change status or assign roles through `/api/users/**`; the landing and auth screens render legibly at 360/768/1280 px in the default theme with no literal surface/text pair and a valid heading outline; no dead link in the public shell; the visual baseline harness runs in CI; `AUD-14`…`AUD-16` and `DS-01`…`DS-05` closed.

---

## Wave 2 — Curriculum and authoring

Screens/features: teacher courses, course editor, module/content ordering, secure uploads and playback, YouTube import/publish, assignments, notes/bookmarks.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W2-01 | DB/API | Add `courses.department_id` FK; backfill from the string; require FK on new courses | `courses`, `departments`, `GET /api/departments/{id}/courses` | A-03; `[B-F2]` | M |
| W2-02 | API | Course state machine: `DRAFT/PENDING/LIVE/ARCHIVED`; `isPublished` projection; admin approval only | `POST/PUT /api/courses`, `PUT /api/admin/courses/{id}/status` | A-04; AC-10 | M |
| W2-03 | API | Course archive/soft delete + restore; replace the hand-written cascade | `DELETE /api/courses/{id}`, `POST /api/courses/{id}/restore` | `[B-F5]`; `spec §5` soft-delete | M |
| W2-04 | API | Module/content ordering: `position` 1..n, reorder endpoint, re-normalise after delete | `module`, `course_content`, `PUT /api/modules/{id}/reorder` | AC-07, AC-08; `[B-F6]` | M |
| W2-05 | DB/API | `Lecture` → `CourseContent` 1:1 migration; player/progress/quiz read content; legacy lecture API read-only | `lectures`, `course_content`, `GET /api/courses/{id}/lectures` | A-05; `[B-F1]` | L |
| W2-06 | API | Assignment validation: `maxMarks > 0`, future `dueDate` on create and update; submission requires at least one of text or file | `assignment`, `assignment_submission` | AC-09; `[B-F8]` | S |
| W2-07 | API | Storage port + local adapter: opaque key (no public URL), random names, MIME + magic-byte/type/size allow-list, traversal rejection, `GET /api/files/{key}` owner/entitlement-authorized download; remove the public `/api/uploads/**` static handler after stored links are migrated | `UploadController`, `StorageService`, `GET /api/files/{key}`, `/uploads` | `spec §6`; `[B-F9]`; BR-26 | M |
| W2-08 | DB/API | Rebind notes/bookmarks to `CourseContent`; migrate lecture rows; bound timestamps to duration | `notes`, `bookmarks` | A-15; AC-14; `[E-F10]` | M |
| W2-09 | API | YouTube import into canonical content: playlist → module/content, transcript cache, quota handling | `POST /api/teacher/youtube/import-playlist` | A-05; `spec §6` | M |
| W2-10 | TEST | Curriculum contract suite: ordering, reorder, delete normalisation, publication, assignment dates, storage safety (public-URL removal, foreign-key 403/404, oversized/mislabelled upload) | `src/test/.../curriculum` | AC-07…AC-09; `PLAN §10`; W2-07 | M |
| W2-11 | API/TEST | Public read cache + HTTP caching for published catalogue/departments: Caffeine DTO cache (TTL ≤ 60 s), `ETag` from a per-key cache generation, `Cache-Control: public`; invalidate after publish/update/archive/module/content/department writes; drafts, staff views and private data bypass; hit/miss metrics; tests for hit, 304, invalidation and draft exclusion | `GET /api/courses`, `GET /api/courses/{id}`, `GET /api/departments`, Caffeine config | NFR-02, NFR-03; A-04; W2-02, W2-03 | M |
| W2-12 | DB/API/UI/TEST | Canonical media source + secure playback: one source per video (`YOUTUBE`/`UPLOAD`/`EXTERNAL`); migrate lecture URLs/file paths; local upload through `StorageService` with random key, MIME + magic-byte MP4/WebM allow-list, 200 MB cap; video keys are never served by the legacy public handler (W2-07 removes it); unattached upload keys expire after 24 h; `GET /api/contents/{contentId}/playback` returns a short-lived entitlement-bound signed URL/embed/external URL; no raw path; player renders descriptor; archive cleanup. **First failing tests:** non-enrolled playback, expired/tampered signature, oversized/unsupported/mislabelled upload, public-path regression, source union validation, no-path-leak, range streaming | `course_content.source_type/provider_video_id/storage_key/external_url/media_status/mime_type/size_bytes`; `GET /api/contents/{contentId}/playback`; `POST /api/teacher/upload/video`; `VideoPlayerComponent` | A-25; BR-26; NFR-03; W2-05, W2-07; `spec §6` | L |
| W2-13 | API/UI/OPS/TEST | Opt-in YouTube publish: teacher OAuth channel connection (upload scope), encrypted refresh token, signed single-use state; idempotent resumable `videos.insert` job from a stored `UPLOAD`, privacy `unlisted`, optional playlist, quota-aware queue (default API quota allows only a handful of uploads/day), retry/error states; publish returns `202 {jobId}`, 409 while an active duplicate exists; persist provider video id + job status but keep the local source active until an explicit switch; feature flag + audit. **First failing tests:** OAuth state replay, token encryption/no-leak, idempotent publish replay, concurrent duplicate 409, quota failure, foreign job 404, publish success leaves source `UPLOAD`, local fallback after failure | `youtube_channel_connections`, `media_publish_jobs`; `POST /api/teacher/media/youtube/connect`, `GET /api/teacher/media/youtube/callback`, `GET/DELETE /api/teacher/media/youtube/connection`, `POST /api/teacher/contents/{contentId}/youtube-publish`, `GET /api/teacher/media/youtube/jobs/{jobId}` | A-25; BR-26; NFR-03; W2-12; `spec §6` | L |
| W2-14 | UI/TEST | Token consolidation: one palette (4–6 base + semantic status pairs), one type scale, one radius scale, one elevation scale in `_tokens.scss`; delete the 11 local `$brand-*` redefinitions; migrate literals screen by screen; the `stylelint` ratchet may only fall. **First failing test:** lint count does not rise in a touched file; no hex outside `_tokens.scss` | `src/styles/**`, `src/app/**/*.scss`, `stylelint.config.*` | `DS-06`; NFR-04(a)(c); A-28 | L |
| W2-15 | UI | Icon unification: one family (Lucide) with one sizing scale; remove the 10 `mat-icon` usages and the Material Icons font request; decorative icons `aria-hidden` | `student/dashboard`, `topbar`, `index.html` | `DS-07`; NFR-04(c) | S |
| W2-16 | UI/TEST | Identity direction + proof: record the client decision (**"verified"** stamp/seal vs **"campus notice-board"**); deliver the token file, the type pairing, one hero treatment and one converted screen as proof; decision recorded in `TRACKER.md` §4. **This is the only decision gate in the overlay.** | `Docs/PLAN.md` §12, `_tokens.scss`, landing + one in-app screen | `DS-08`; A-28; ADR-16 | L |
| W2-17 | UI/TEST | Public surfaces rebuild on the approved direction: landing, login, register, forgot-password, verify-certificate; real artefacts/imagery instead of flat icon blocks; one orchestrated load moment; re-capture the public/auth baselines once, then freeze under NFR-01 | `features/landing/**`, `features/auth/**`, `features/public/**`, `e2e/visual` | `DS-09`; NFR-01 exception; NFR-04; A-28 | L |

**Exit:** a teacher authors a course with ordered modules/content, uploads a private video and previews it, imports a playlist, optionally exercises the fake YouTube publisher (a real unlisted publish is a credential-gated manual smoke), and archives/restores the course; seeded enrolled/non-enrolled fixtures prove playback authorization (the full enrolment flow gate is W3); a student cannot see or mutate a draft; a non-enrolled user cannot obtain a playback descriptor; positions are contiguous after deletes; public catalogue reads return `ETag`/304 and a publish/archive/content edit invalidates the cached entry; one palette and one icon family with zero new lint violations; the direction decision is recorded and the public/auth baselines are re-captured and frozen; `DS-06`…`DS-09` closed.

---

## Wave 3 — Enrolment, progress and credentials

Screens/features: course detail/enrol, My Courses (enrolled-only), Explore catalogue, player/resume, certificates, leaderboard, same-college access.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W3-01 | DB/API | Unique `(user_id, course_id, role)`; enrol self only; require `status == LIVE` | `enrollments`, `POST /api/enrollments` | AC-10; A-04; `[C-F1, C-F2]` | M |
| W3-02 | API | Roster and unenrol authorization: course staff/admin only; unenrol self or admin; 404 for foreign ids | `GET /api/courses/{id}/enrollments`, `DELETE /api/enrollments/{id}` | `[C-F3]`; `spec §5` | S |
| W3-03 | DB/API | Unique `(user_id, content_id)`; progress upsert; `BigDecimal(5,2)`; clamp 0–100 | `user_progress`, `POST /api/progress` | AC-11, AC-12; A-08, A-10 | M |
| W3-04 | API | Completion rule: `completedAt` at ≥90 %; un-complete on regression; no stored course completion; a required content-bound quiz gate is added by W4-11 (none exists before W4) | `user_progress`, `POST /api/student/lectures/{id}/complete` | AC-12, AC-13; A-08, A-27 | S |
| W3-05 | API | Content-based course completion aggregation; dashboard/roster use the same query | `GET /api/student/courses/{id}/progress`, `GET /api/progress/course/{id}/summary` | AC-13; `[C-F7, C-F8]` | M |
| W3-06 | API | Certificate eligibility + real grade/score; idempotent issuance; public verification | `certificates`, `POST /api/certificates/issue`, `GET /api/certificates/verify/{code}` | AC-24; `[C-F9]` | M |
| W3-07 | WK/API | Materialise `leaderboard` after attempt completion; keep XP rank separate and labelled | `leaderboard`, `TestAttempt` event | `[C-F10, C-F11]`; `AG §8` | M |
| W3-08 | API | Same-college entitlement: verified student + teacher college → free enrol; no payment row; override audited | `POST /api/enrollments`, `college_entitlements` | A-20; `[F-F15]` | M |
| W3-09 | TEST | Learning regression suite: enrol/permission, upsert, 90 %, certificate, entitlement, My Courses/catalogue separation, resume target, player authorization | `src/test/.../learning`, frontend specs | AC-10…AC-14, AC-24; `AUD-07`…`AUD-09`; `PLAN §10` | M |
| W3-10 | UI/API | My Courses enrolled-only; move “Explore more courses” and sidebar Video Lectures to standalone `/student/explore` with all published courses + search; add Continue/resume resolving the first incomplete content; enrolled cards link to Continue, not duplicate enrolment | `GET /api/enrollments/me`, `GET /api/student/courses/{courseId}/resume` (new), `GET /api/student/courses/{courseId}/progress`, `GET /api/courses`; `courses.component.*`, Explore route | `audit.md` C1–C3, C6 (`AUD-07`); BR-23; AC-10, AC-12, AC-13 | L |
| W3-11 | API/UI/TEST | Course-scoped entry `/student/courses/:courseId/player` resolves the first incomplete content and redirects to the existing lecture playback route; keep playback at `/student/courses/:courseId/lectures/:lectureId`; remove first-course fallback and duplicate aliases; server checks enrolment before returning lecture metadata and delegates playback to the W2-12 descriptor (no raw video URLs) | `GET /api/student/courses/{courseId}`, `GET /api/student/courses/{courseId}/lectures/{lectureId}`, `GET /api/courses/{id}/lectures`, `GET /api/contents/{contentId}/playback`; `app.routes.ts`, `LectureComponent`, enrolment access helper | `audit.md` C2, C4, C5, C7 (`AUD-08`); BR-23, BR-26; A-05; AC-10; W2-12 | M |
| W3-12 | UI | Course-surface polish: real duration sum, correct filters, enrolled/available card states, empty states, remove mojibake/currency text | `CourseCardComponent`, `courses.component.*`, Explore section, `lecture.component` | `audit.md` C6, C8 (`AUD-09`) | S |
| W3-13 | API/TEST | Hot-path read optimization + cache boundary: set-based resume/player/progress/dashboard queries, DTO projections, pagination and indexes (`user_progress`, `enrollments`, `leaderboard`, `test_attempts`); no N+1; public course structure may reuse W2-11 cache but per-user progress is always fresh; `Cache-Control: private, no-store` on resume/progress/leaderboard; query-count and cold/warm p95 tests | `GET /api/student/courses/{courseId}/resume`, `GET /api/student/courses/{courseId}/progress`, `GET /api/student/courses/{id}`, `GET /api/courses/{courseId}/leaderboard`; `user_progress`, `enrollments`, `leaderboard`, `test_attempts` | NFR-02, NFR-03; AC-13, AC-14; W3-05, W3-07 | M |
| W3-14 | UI | Student surfaces on the system: dashboard, course cards, My Courses and Explore catalogue — one card hierarchy (no identical card for course/stat/panel), tabular numerals for progress/scores, real empty/loading/error states. Rides `W3-10`, `W3-12`. **Baseline captured before the change.** | `features/student/dashboard/**`, `features/student/courses/**`, shared `course-card` | `DS-10`; NFR-01; NFR-04; BR-23 | M |
| W3-15 | UI | Player workspace typography and density: keep the 2-column video + transcript + notes layout, make it the one bold element and quiet the surrounding chrome; no generic card treatment on the player shell. Rides `W3-11`, `W4-11`, `W5-10`. **Baseline captured before the change.** | `features/student/courses/lecture/**`, `shared/video-player`, `interactive-transcript` | `DS-11`; NFR-01; NFR-04 | M |

**Exit:** a student enrols, watches to 90 %, sees completion, gets a verifiable certificate; a same-college student gets free access; duplicate progress/enrolment rows are impossible; My Courses is enrolled-only; Explore is separate; Continue/resume opens the first incomplete lecture; a non-enrolled student cannot fetch player metadata or a playback descriptor; resume/player/dashboard p95 < 1 s with no N+1; progress and leaderboard responses are `private, no-store`; student surfaces and the player workspace pass the token lint and axe with before/after baselines attached; `AUD-07`…`AUD-09` and `DS-10`, `DS-11` closed.

---

## Wave 4 — Assessment integrity

Screens/features: assessments list, attempt/quiz, teacher test builder, post-video quiz in the player, manual grading, results.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W4-01 | API | Student question DTO hides `correctAnswer`; retire leaky `/api/tests/{id}/questions` and `/api/questions/{id}` reads | `questions`, `GET /api/assessments/{id}` | AC-15; `[D-F1]` | M |
| W4-02 | API | Retire legacy `/api/tests` write surface (or admin-only, deprecated); `/api/assessments` is the only student/teacher path | `TestController`, `AssessmentController` | `[D-F2, D-F12]` | M |
| W4-03 | DB | `Question.extra` JSON options; migrate `question_options`; validate `correctAnswer` against options; drop collection table | `questions.extra`, `question_options` | A-12; `[D-F8]` | M |
| W4-04 | DB/API | Unique `(attempt_id, question_id)`; answer upsert; reject answers after finalize/timeout | `student_answers` | AC-16; `[D-F4]` | M |
| W4-05 | API | Grading: MCQ/TF auto; TEXT stays `null` until manual; no auto-full-marks; score from stored answers only | `POST /api/attempts/{id}/submit`, `PATCH /api/answers/{id}/grade` | AC-16, AC-17; `[D-F3]` | M |
| W4-06 | DB/API | Attempt lifecycle: `startedAt`, `completedAt`, `ABANDONED`, timeout job, server clock | `test_attempts`, `AttemptTimeoutJob` | AC-18; `[D-F5]` | M |
| W4-07 | API | Enforce `scheduledAt`, `durationMinutes`, and attempt limit on start; check enrolment | `POST /api/tests/{id}/attempts`, `POST /api/assessments/{id}/start` | AC-18; `[D-F6, D-F7, D-F10]` | M |
| W4-08 | DB | Numeric migration: score/marks `BigDecimal(8,2)`; update DTOs and aggregates | `test_attempts.score`, `student_answers.marks_awarded`, `questions.marks` | A-10; `[D-F9]` | M |
| W4-09 | API | Attempt review: answers + marks, correct answers only after finalize; manual grade audit | `GET /api/assessments/attempts/{id}`, `GET /api/attempts/{id}/answers` | AC-15, AC-16; `[D-F12]` | S |
| W4-10 | TEST | Assessment integrity suite: leak, duplicate answer, TEXT grade, timeout, schedule, foreign attempt, post-video completion gate | `src/test/.../assessment` | AC-15…AC-18; `PLAN §10` | M |
| W4-11 | DB/API/UI/TEST | Player-attached quiz end-to-end: bind one test to `CourseContent`; `placement=POST_VIDEO`; server-owned `pass_threshold` (default 60 %) and `max_attempts` (default 3; 0 = unlimited); player pauses at end and opens an accessible in-player quiz page; start/resume/finalize; required quiz gates `completedAt`; pass is sticky across failed retakes/quiz edits; retake/timeout/error paths; no answer or completion leakage. **First failing tests:** watch-to-90 without quiz stays incomplete, pass before 90 % stays incomplete, watch-90 + pass completes once, failed retake stays complete after an earlier pass, retake limit enforced, adding/changing a required quiz does not retroactively revoke, foreign content 404, client completion rejected, quiz/attempt `no-store` | `tests.content_id/placement/required_for_completion/pass_threshold/max_attempts`; `POST /api/teacher/tests`, `PUT /api/teacher/tests/{testId}`; `GET /api/assessments/content/{contentId}`; `POST /api/assessments/{id}/start`, attempt answer/finalize; `PostVideoQuizComponent`, `LectureComponent`, `VideoPlayerComponent` | A-27; BR-25; AC-12, AC-15, AC-18; W4-05, W4-07, W3-11 | L |

**Exit:** a student cannot see answers before submit, cannot double-answer, and is auto-abandoned at the deadline; TEXT is ungraded until a teacher grades it; a required post-video quiz completes content only after 90 % watch plus a server-verified pass, retakes respect the limit, and the player does not finish the lecture itself; attempt/answer/quiz responses are `private, no-store`.

---

## Wave 5 — Engagement and community

Screens/features: discussion threads/posts, reviews, notes/bookmarks, announcements, notifications, messages, live sessions with real provider playback, moderation.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W5-01 | DB/API | Canonical `discussion_thread`/`post`; migrate `discussion_posts`; UI switches to threads | `discussion_thread`, `post`, `/api/courses/{id}/threads` | A-13; `[E-F1]` | L |
| W5-02 | API | Enrolment-gated community reads/writes; course staff/admin override; public catalogue hides discussions | thread/post/discussion controllers | A-14; `[E-F3]` | M |
| W5-03 | API | Reply nesting: two levels, parent must be same thread, cascade delete, ordering `createdAt ASC` | `post.parent_post_id` | A-13; `[E-F5, E-F6]` | M |
| W5-04 | API | Moderation on canonical posts: profanity/auto-flag, resolve removes/masks content, clear flag | `flagged_content`, `ModerationController` | `[E-F7, E-F8]` | M |
| W5-05 | API | Announcements: teacher course-scoped, admin global; pin/expiry; no cross-course broadcast | `announcements` | A-17; `[E-F13]` | M |
| W5-06 | DB/WK | Notification event catalogue + outbox: enrolment, grade, feedback, reply, live start, announcement | `notifications`, `notification_outbox` | A-17; `[E-F14]` | M |
| W5-07 | API | Live session join enrolment-gated; start/end authorization; no attendance in v1 | `live_sessions`, `GET /api/student/live-sessions/{id}` | A-18; `[E-F16]` | S |
| W5-08 | API | Messaging pagination, participant authorization, report/block, retention note | `conversations`, `messages` | `[E-F17]`; `spec §4 SCR-23` | M |
| W5-09 | API/TEST | Feedback eligibility (enrolled), rating 1–5, one per course, average; public rating aggregate cached ≤ 60 s and invalidated on feedback write; community regression suite | `feedback`, `GET /api/courses/{id}/feedback/avg` | AC-22, AC-23; NFR-03; `[E-F12]` | M |
| W5-10 | DB/API/UI/TEST | Real live media: validate/store a provider playback source (`YOUTUBE_LIVE` video id or HTTPS HLS manifest) on create/update; reject RTMP/encoder stream keys; start requires a source; `GET /api/student/live-sessions/{id}` returns a playback descriptor only while `LIVE` and enrolled; player live mode handles YouTube `/live/{id}` and HLS (native where supported, lazy `hls.js` otherwise), surfacing CORS/format failures; no simulated broadcast; end hides playback; WS start/end status; no recording. **First failing tests:** start without source blocked, non-enrolled descriptor 404, ended/scheduled descriptor absent, RTMP/stream key rejected, playback URL only in descriptor, player live/error states, HLS error does not simulate a stream, descriptor is `private, no-store` | `live_sessions.source_type/provider_video_id/source_url/playback_mode/started_at/ended_at`; `PUT /api/teacher/live-sessions/{id}/source`; `GET /api/student/live-sessions/{id}`; `SUBSCRIBE /topic/live-sessions/{sessionId}/status`; `live-room.component.*`, `VideoPlayerComponent` | A-26; BR-27; A-18; W5-07; NFR-03 | L |

**Exit:** one discussion model in UI and API; only enrolled users read/write; a flagged post is actually hidden; a teacher announcement notifies only that course; a live session cannot start without a real source; an enrolled student sees provider playback only while `LIVE`; a non-enrolled user cannot obtain a descriptor; the public rating cache invalidates on a feedback write.

---

## Wave 6 — Commerce, royalties and institutions

Screens/features: course purchase, payment success, purchases/payment history, receipts, teacher royalties, admin revenue/payouts, colleges/affiliations.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W6-01 | UI/API | Remove the simulated checkout page/verify path; every purchase uses the Razorpay modal | `features/payment/**`, `POST /api/payments/checkout`, `POST /api/payments/verify` | A-06; `audit.md` B10, E3 (`AUD-06`); `[F-F1]` | M |
| W6-02 | API | Payments service authentication and ownership: JWT or service identity on every `/api/payments/**` call; remove `@CrossOrigin("*")`; derive the user from auth; retire `/orders/{userId}` in favour of `/orders/me`; no client-supplied `userId` | `OrderController`, `SecurityConfig` (payments), `/orders/me` | `audit.md` B8, B11, E2 (`AUD-04`); `[F-F2]` | M |
| W6-03 | API | Fail-closed verification, order idempotency, no price fallback: invalid signature → 400; one capture per order; createOrder never substitutes a default amount | `POST /api/payments/orders`, `POST /api/payments/orders/verify` | AC-19; `audit.md` B7 (`AUD-03`); `[F-F2, F-F4, F-F5]` | M |
| W6-04 | API | Remove `/orders/simulate-success`; replace with a `test`-profile fake that cannot run in prod | `OrderController`, `application-test.properties` | `audit.md` B8 (`AUD-04`); `[F-F3]` | S |
| W6-05 | API | `/internal/enroll`: recompute price, ignore body `amount`, require key + service id, idempotent ledger write, require real gateway payment id | `InternalController`, `payments` | A-19; `audit.md` B9 (`AUD-05`); `[F-F6, F-F7]` | M |
| W6-06 | DB/API | One ledger keyed by gateway payment id; remove fabricated/history-created rows; retain pending/failed attempts; normalise `PENDING/SUCCESS/FAILED` to `CREATED/PAID/FAILED` at the API; preserve `paidAt` and `gatewayPaymentId`; reconcile microservice orders | `payments`, `PaymentServiceImpl.getHistory`, `PaymentResponseDto` | A-21; `audit.md` B1–B6, B12 (`AUD-02`); `[F-F8, F-F10]` | L |
| W6-07 | API | Remove royalty auto-reconcile; royalties only from captured ledger rows; revenue derives from the same ledger | `royalties`, `RoyaltyServiceImpl`, `RevenueServiceImpl` | A-21; `[F-F9, F-F10]` | M |
| W6-08 | API | 70/30 policy object used by payment, internal enrol and revenue; report split consistently | `RoyaltyPolicy`, revenue DTOs | A-07; AC-21; `[F-F11]` | S |
| W6-09 | API/WK | Payout: ₹500 minimum, monthly statement, DB lock, idempotency key, teacher-visible history | `payouts`, `PayoutAdminController` | A-22; `[F-F18]` | M |
| W6-10 | API | College verification + teacher affiliation gate; reject/archive instead of hard delete; notify decision | `colleges`, `affiliation_requests`, `AdminController` | A-24; `[F-F12…F-F14]` | M |
| W6-11 | API | Same-college free entitlement + admin override + audit; UI shows "Included by your college" | `college_entitlements`, `POST /api/enrollments` | A-20; `[F-F15]` | M |
| W6-12 | TEST | Commerce/security suite: forgery, replay, amount tamper, ledger reconciliation, status mapping, failed-before-success preservation, receipt fields, entitlement, payout lock, payment/receipt responses `no-store` | `src/test/.../commerce`, frontend purchase/receipt specs | AC-19…AC-21; NFR-03; `AUD-01`…`AUD-06`; `PLAN §10` | L |
| W6-13 | UI/TEST | Receipt PDF + modal: monochrome/ASCII layout, dynamic wrapping and row heights, real receipt/order/payment/transaction IDs, paid date **and** time, status, amount; remove the fabricated `INV-{id+1000}`; only `PAID` rows; no GST/tax claim; modal/print parity; owner-scoped data | `PdfExportService`, `PurchasesComponent`, `PaymentResponseDto`; `payments` | `audit.md` A1–A6, E5 (`AUD-01`); BR-22 | M |
| W6-14 | UI/TEST | Payment history UI: normalise `PENDING/SUCCESS/FAILED` and display labels Paid/Pending/Failed for `PAID/CREATED/FAILED`; keep failed/pending before a later success, dedupe by gateway order not course, preserve transaction ID/capture time, one amount/timestamp unit, correct counts/totals, no success without a real successful payment, microservice orders supplementary with ledger precedence, receipt action only for successful payments | `PurchasesComponent`, `PaymentsService`, `GET /api/payments/history`, `GET /api/payments/orders/me` | `audit.md` B2–B6, B12 (`AUD-02`); BR-21 | M |

**Exit:** one checkout rail; invalid signatures never enrol; replaying any money call cannot mint a second enrolment, payment or royalty; revenue/royalty reconcile to the ledger; same-college students are free; payment history shows real paid/pending/failed attempts without fabricated rows; receipts contain real transaction identity/time and no missing glyphs; payment/receipt responses are `private, no-store`; `AUD-01`…`AUD-06` closed.

---

## Wave 7 — Client experience and performance

Screens/features: all 61 route entries / 47 distinct screens, role shells, route loading, session UX, student/teacher/admin profiles and settings, accessibility, responsive layouts.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W7-01 | OPS | Angular 21 baseline: correct docs, remove Angular 19 claims, pin the toolchain, document the upgrade policy | `Docs/Architecture.md`, `Docs/Frontend.md`, `package.json` | `[G-F1]`; ADR-06 | S |
| W7-02 | UI | Lazy-load role route groups + preload strategy; split the 61 eager routes | `app.routes.ts` | `[G-F2]`; `spec §7` | M |
| W7-03 | UI | API base URL injection tokens + environment files; remove hard-coded `localhost:8080/8081` | `environment*.ts`, `ApiService`, `PaymentsService` | `[G-F7]` | S |
| W7-04 | UI | Session UX: silent refresh, expiry warning, logout API, redirect preservation | `AuthStore`, interceptors, guards | A-09; `[G-F4, G-F5]` | M |
| W7-05 | UI | Remove dead `store/` scaffolding; standardise on signals + feature services | `src/app/store`, feature services | `[G-F6]`; ADR-06 | S |
| W7-06 | UI | Route ↔ SCR matrix: map all 61 route entries to the 47 screen contracts, fix gaps/redirects, remove duplicate aliases | `app.routes.ts`, `schema-usage.md` | `spec §4` | M |
| W7-07 | UI | Copy extraction for changed screens; no hard-coded copy in new code | `core/content/*.ts` | `PLAN §11` DoD | M |
| W7-08 | UI | Accessibility fixes: labels, focus, modal trap, contrast, keyboard player/quiz/checkout | shared components + changed screens | `spec §7`; `[G-F15]` | M |
| W7-09 | UI | Responsive verification at 360/768/1280 px; fix overflow, tables, player, checkout | changed screens | `spec §7` | M |
| W7-10 | TEST | Frontend unit suite + Playwright smoke; replace the stale `Hello, learnsphere` test; cover session, purchase/receipt, catalogue/resume, profile save and settings persistence | `src/**/*.spec.ts`, `e2e/**` | `audit.md` D1–D5; `[G-F8]`; `PLAN §10` | M |
| W7-11 | UI | Teacher/admin profiles: load fresh `GET /users/me`, real department/bio/stats, writable name, save via `PUT /users/me`; remove fake avatar/password/health handlers or wire them to real actions; edits persist after refresh | `features/teacher/profile`, `features/admin/profile`, `AuthService`, user service | `audit.md` D2, D3 (`AUD-11`, `AUD-12`); BR-24; AC-02 | L |
| W7-12 | UI | Profile/settings completeness: student profile copy/mojibake/error-toast cleanup without churning its WIP styles; persist supported settings or remove unsupported controls; sign-out calls the API; admin Inspect loads roles/payments/audit; role assignment UI | `features/student/profile`, student/teacher/admin settings, admin user management, `/api/users/me`, `/api/admin/users/**` | `audit.md` D1, D4, D5 (`AUD-10`, `AUD-12`, `AUD-13`); BR-24; AC-03; A-23 | M |
| W7-13 | UI/TEST | Frontend performance + design-preservation guard: route preload, `OnPush`/`trackBy` and image/font loading only where measured; hashed static assets served `immutable`; Playwright visual baselines at 360/768/1280 px for changed screens including player, in-player quiz and live states; token/component audit; no restyle/re-layout outside the DS stories that own the screen (`PLAN §12`); only audit-required layout, DS-owned conversion and the new player/live/quiz state changes are allowed | Angular build, shared components, `src/styles/_tokens.scss`, `e2e/visual` | NFR-01, NFR-02, NFR-04; `[G-F2]`; W7-02, W7-09 | M |
| W7-14 | UI | Teacher and admin consoles on the system: tables, stat cards, status pills and filters — one density scale, tabular numerals for money/score, no duplicated card chrome | `features/teacher/**`, `features/admin/**`, `shared/data-table`, `shared/stat-cards`, `shared/status-pills` | `DS-12`; NFR-01; NFR-04 | M |
| W7-15 | UI | Motion restraint and copy pass: replace `transition: all` with named properties, remove hover-lift from non-interactive cards, at most one orchestrated page-load moment per page; CTA vocabulary (verb + object, same name through the flow), no `→` suffix, no unverifiable superlative, copy into `core/content/*.ts` | `src/app/**/*.scss`, `src/app/core/content/*.ts` | `DS-13`; NFR-04(e)(f); W7-07 | M |

**Exit:** initial JS ≤ 500 kB gzip; every role group lazy-loads; no `localhost` in a production build; axe clean on changed screens at three widths; E2E smoke green; teacher/admin profile edits persist after refresh; no settings success toast without an API write; visual baselines pass with no unapproved design drift; hashed assets carry immutable cache headers; teacher/admin consoles pass the token lint and axe; no `transition: all` remains in changed files; `AUD-10`…`AUD-13` and `DS-12`, `DS-13` closed.

---

## Wave 8 — Intelligence, discovery and release

Screens/features: AI quiz generation, transcript summary, chatbot, YouTube, search, trending, reports, observability.

| ID | Layer | Story | Endpoints / tables | Rules | Size |
|---|---|---|---|---|---|
| W8-01 | API | AI quiz generation writes `Question.extra` options, validates answers, caps output, logs prompts without PII | `POST /api/ai/extract-questions`, `questions` | A-12; `spec §6` | M |
| W8-02 | API | Transcript summary with timeout, retry cap and graceful "AI unavailable" fallback | `POST /api/ai/summarize-transcript` | `spec §6`; NFR p95 | S |
| W8-03 | API | Chatbot grounding on course content, safety filter, no answer leakage, token/time cap | `POST /api/chatbot/ask` | AC-15; `spec §6` | M |
| W8-04 | API | YouTube import hardening: canonical content, transcript cache, quota/error handling, duplicate detection | `YouTubeController`, `course_content` | A-05; `spec §6` | M |
| W8-05 | API | Catalogue search/filter/pagination with indexes; stable relevance and empty states | `GET /api/courses` | `spec §7` p95; `[G-F2]` | M |
| W8-06 | API | Trending/recommendations from enrolment/progress/ratings; label the signal; no fake scores; snapshot cached ≤ 60 s and invalidated on enrolment/rating writes | `GET /api/courses/trending` | `spec §4 SCR-11`; NFR-03 | S |
| W8-07 | API | Admin reports/exports: revenue, completion, at-risk, flagged; CSV/PDF from the ledger and progress | `GET /api/admin/reports/summary` | `spec §4 SCR-53/54` | M |
| W8-08 | OPS | Observability: dashboards for latency/errors/payment failures, alerts, runbooks | Actuator, logs, alert config | `spec §7`; `[G-F14]` | M |
| W8-09 | OPS | Release readiness: performance test, security scan, backup/restore drill, release notes | CI + runbooks | `PLAN §10` | M |

**Exit:** AI fails gracefully without blocking a lesson; search p95 < 1 s on seed data; trending/rating caches invalidate on enrolment/rating writes; an operator can diagnose a failed payment from logs and a runbook.

---

## Hardening

| ID | Layer | Story | Done when | Size |
|---|---|---|---|---|
| H-01 | TEST | Full regression on a clean checkout + deterministic seed reset | All suites green from scratch; no manual step | M |
| H-02 | TEST | Security review/pen test: auth, IDOR, payment forgery/replay, answer leakage, upload abuse, media playback authorization, YouTube OAuth/publish, live descriptor/stream-key leakage | No critical/high findings open; fixes retested | M |
| H-03 | TEST | Performance: catalogue/resume/player/media descriptor/in-player quiz/dashboard p95 < 1 s, live descriptor p95 < 1 s (provider excluded), query-count budgets, bundle budgets, Lighthouse CI | Budgets met on seed data at target viewports; no N+1 on hot paths | M |
| H-04 | TEST | Accessibility audit at AA, keyboard-only flows, screen-reader spot checks | No serious/critical axe findings; manual checklist signed | M |
| H-05 | DB | Migration rehearsal on a production-like copy; rollback/restore runbook | Forward migration + restore proven; timing recorded | M |
| H-06 | OPS | Backup/restore + incident runbook: payment failure, DB loss, token compromise | Restore drill passes; on-call steps are one page | S |
| H-07 | OPS | Docs truth pass: README, RUNNING, Progress, AGENTS, screenshots | No claim contradicts code/TRACKER; Angular 21 and 70/30 corrected | M |
| H-08 | OPS | UAT with admin/teacher/student personas + sign-off; known-issues list | Sign-off recorded; blockers triaged | M |
| H-09 | TEST/OPS | Audit remediation verification: execute `audit.md` §G end to end (PDF glyphs/fields, payment states, My Courses/Explore/resume, player authorization, teacher/admin persistence), plus teacher upload→student playback, player quiz completion and live playback UAT; confirm every `AUD-xx` is closed or has an approved deviation, and confirm no unrelated WIP is staged | Checklist signed with commands/evidence; all P0/P1 closed; full suite green; no unrelated WIP in the release commit | M |
| H-10 | TEST/OPS | Cache and visual-preservation gate: verify public cache hit/miss and `ETag`/304, invalidation after publish/archive/edit/feedback/profile update; assert `private, no-store` on auth/profile/progress/assessment/uploaded-file/media/live/YouTube-job/payment/receipt/admin reads; run Playwright visual diffs at 360/768/1280 px and a token/component review; confirm no unapproved redesign | Cache/header tests, Playwright baselines and review checklist; `NFR-01`, `NFR-03`; owners W2-07/W2-11/W2-12/W2-13/W3-13/W4-11/W5-10/W7-13 | M |
| H-11 | TEST/OPS | Design-system conformance gate: `stylelint` ratchet at zero new violations and no hex outside `_tokens.scss`; one icon family; WCAG AA contrast on every screen in both themes; heading outline valid on every route; at most one eyebrow per page; no `transition: all`; every CTA is verb + object with no `→` suffix; every rendered number comes from the API; public/auth baselines reviewed and signed after `DS-09` | Lint report, axe runs at 360/768/1280 px, contrast report, Playwright baselines, review checklist; `NFR-04`; owners `DS-01`…`DS-13` | M |

**Exit:** signed UAT, no open critical/high security issue, p95 met, cache/visual gate passed, restore drill passed, design-conformance gate passed (`H-11`: zero new lint violations, one icon family, AA contrast in both themes), docs match reality, and the audit verification checklist is signed with every `AUD-xx` closed or explicitly deferred.
