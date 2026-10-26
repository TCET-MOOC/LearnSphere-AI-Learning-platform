# LearnSphere AI — Data / schema usage

Companion to [`../PLAN.md`](../PLAN.md) §8 (data plan), [`waves.md`](waves.md) and [`api-catalogue.md`](api-catalogue.md). This file maps every screen/feature onto the 36 existing JPA entities plus two media-control tables, records the forward-only migrations, and states how schema gaps are handled in code.

**Citation keys:** `REF p<n>` = entity reference page; `A-xx` = assumption in `PLAN.md` §3; `AC-xx` = acceptance criterion in `spec.md`; `[X-Fn]` = finding n in analysis X; `audit.md` = 2026-10-24 post-build audit.

---

## 1. Screen / feature → table map

**Coverage:** 61 client route entries → 47 distinct screens. The 14 route entries without a separate
screen heading (IDs 09, 27–29, 39–40, 42–49) are **derived** to be aliases, redirects or legacy
entries; W7-06 maps each to a canonical screen below or retires it. They add no table surface of their own.

| Screen / feature | SCR | Reads | Writes | Notes |
|---|---|---|---|---|
| Landing & public catalogue | SCR-01, SCR-11 | `courses`, `departments`, `feedback`, `users` (teacher) | — | Only `status = LIVE` (A-04); rating from `feedback` (AC-23; `[E-F12]`) |
| Login | SCR-02 | `users`, `user_roles`, `roles` | `refresh_tokens`, `audit_log` | `BLACKLISTED` rejected; `last_login` updated (AC-01) |
| Register | SCR-03 | `colleges`, `departments` | `users`, `user_roles`, `affiliation_requests` | Public registration = `STUDENT`; teacher application (A-11, A-24) |
| Forgot / reset password | SCR-04 | `users`, `password_reset_tokens` | `password_reset_tokens` | Token currently logged; SMTP later (`spec §8`) |
| College verification | SCR-05 | `colleges`, `affiliation_requests` | `affiliation_requests` | Only `VERIFIED` colleges are selectable (A-24) |
| Certificate verification (public) | SCR-06 | `certificates`, `users`, `courses` | — | Code lookup only; no PII beyond name/course (AC-24) |
| Checkout | SCR-07 | `courses`, `enrollments`, `payments`, `college_entitlements` | `orders` + `payments` (microservice), `payments` (main ledger) | One Razorpay rail (A-06, A-21) |
| Payment success | SCR-08 | `payments`, `enrollments`, `courses` | — | Must call status/verify; never trust the query string (`spec §3.5`; `[F-F3]`) |
| Student dashboard | SCR-10 | `enrollments`, `user_progress`, `test_attempts`, `notifications`, `leaderboard` | — | Completion computed on the fly (AC-13) |
| Course detail | SCR-12 | `courses`, `module`, `course_content`, `feedback`, `users` | `enrollments` (free/entitled) | Published only; same-college entitlement (A-20); My Courses is enrolled-only and Explore is separate (`audit.md` C1–C3; BR-23) |
| Lecture player | SCR-13 | `course_content` + media descriptor, `user_progress`, `notes`, `bookmarks`, `tests` (content-bound), `enrollments` | `user_progress` | `CourseContent` canonical (A-05); media descriptor is short-lived and `private, no-store` (A-25; W2-12); 90 % rule plus required quiz gate (A-08, A-27); server-side enrolment check; resume first incomplete (`audit.md` C2–C7; BR-23, BR-25, BR-26) |
| Notes / bookmarks | SCR-14, SCR-15 | `notes`, `bookmarks`, `course_content` | `notes`, `bookmarks` | Content-bound; timestamp bounded (A-15) |
| Assessments / quiz attempt | SCR-16, SCR-17 | `tests` (+ `content_id` for post-video), `questions`, `test_attempts`, `student_answers`, `enrollments`, `course_content` | `test_attempts`, `student_answers` | Server timer; TEXT manual (AC-17, AC-18); post-video quiz is content-bound and gates completion only on a server-verified pass (A-27; W4-11) |
| Leaderboard | SCR-18 | `leaderboard`, `test_attempts`, `users`, `enrollments` | `leaderboard` (worker) | Course table materialised after attempts; XP separate (`[C-F10, C-F11]`) |
| Discussion | SCR-19 | `discussion_thread`, `post`, `users`, `enrollments` | `discussion_thread`, `post`, `flagged_content` | Canonical thread/post; legacy `discussion_posts` retired (A-13, A-14) |
| Live sessions | SCR-20 | `live_sessions` + provider source, `enrollments`, `users` | `live_sessions` | Join requires enrolment; playback descriptor only while `LIVE`; no placeholder join URL (A-18, A-26; W5-10) |
| Certificates / purchases | SCR-21, SCR-22 | `certificates`, `payments`, `enrollments`, `courses` | `certificates` | Certificate eligibility 90 % per content (AC-24; A-08); purchases read the real ledger, keep failed/pending, and receipts use real ids/time (`audit.md` A/B; BR-21/22) |
| Messages / notifications / announcements | SCR-23, SCR-24, SCR-25 | `conversations`, `messages`, `notifications`, `announcements`, `enrollments` | same + `notification_outbox` | Course-scoped announcements; outbox events (A-17) |
| Profile & settings (all roles) | SCR-26, SCR-41, SCR-61 | `users`, `user_roles`, `roles`, `colleges`, `departments` | `users`, `audit_log` | API-backed saves only; no local-only success toast; unsupported controls removed (AUD-10…AUD-13; BR-24); password self-only; no role self-assignment (AC-03) |
| Teacher dashboard | SCR-30 | `courses`, `enrollments`, `user_progress`, `test_attempts`, `payments`, `royalties` | — | All aggregates scoped to own courses (`[B-F7]`) |
| My courses / course editor | SCR-31, SCR-32 | `courses`, `module`, `course_content`, `assignment`, `departments` | same | Ownership + affiliation gate (AC-04, A-24) |
| Upload / media source | SCR-33 | `course_content` (source descriptor), `module` | `course_content`, private `uploads` | Storage port; 200 MB; MP4/WebM; random key; no public path; optional YouTube publish job (A-25; W2-12, W2-13) |
| Students & standings | SCR-34 | `enrollments`, `user_progress`, `test_attempts`, `leaderboard`, `users` | — | Teacher owns course; no global roster (`spec §5`; `[C-F3]`) |
| Teacher assessments | SCR-35 | `tests` (+ `content_id`/delivery fields), `questions`, `question_options` → `extra`, `test_attempts`, `student_answers` | `tests`, `questions` | `extra` canonical; one post-video quiz per content; no direct entity exposure (A-12, A-27; W4-11) |
| Live / trending / royalties | SCR-36, SCR-37, SCR-38 | `live_sessions`, `courses`, `enrollments`, `royalties`, `payouts`, `payments` | `live_sessions`, `payouts` | Royalty reads ledger only (A-21, A-22) |
| Admin dashboard | SCR-50 | `users`, `courses`, `enrollments`, `payments`, `flagged_content`, `colleges` | — | Counts from ledger/academic tables |
| User governance | SCR-51 | `users`, `roles`, `user_roles`, `audit_log` | `users.status`, `user_roles`, `audit_log` | Admin only; `FLAGGED` read-only; generic `/api/users` writes retired (`AUD-14`; W1-11) |
| Course approvals | SCR-52 | `courses`, `users`, `departments` | `courses.status`, `audit_log` | `PENDING → LIVE` admin only (A-04) |
| Revenue / reports | SCR-53, SCR-54 | `payments`, `courses`, `enrollments`, `users` | — | Revenue = captured ledger; no fabricated rows (A-21) |
| Payouts | SCR-55 | `royalties`, `payouts`, `users`, `payments` | `payouts`, `audit_log` | ₹500 minimum, monthly, locked (A-22) |
| Moderation / sentiment | SCR-56, SCR-57 | `flagged_content`, `post`, `messages`, `announcements` | `flagged_content` | Resolve hides/masks content; sentiment is heuristic (`[E-F8]`) |
| Colleges & affiliations | SCR-58 | `colleges`, `affiliation_requests`, `users`, `courses` | `colleges`, `affiliation_requests`, `audit_log` | Verify/reject archive, not hard delete (A-24) |
| Announcements / messages (admin) | SCR-59, SCR-60 | `announcements`, `conversations`, `messages`, `notifications` | same | Global admin announcements only (A-17) |
| AI quiz generation | SCR-35 (assist) | `course_content`, `questions` | `questions.extra` | NVIDIA NIM; fallback (`spec §6`; A-12) |
| YouTube import / publish | SCR-33 | `module`, `course_content`, `lectures` (legacy), `youtube_channel_connections`, `media_publish_jobs` | `course_content`, `youtube_channel_connections`, `media_publish_jobs` | Import is link/import only; publish is opt-in, OAuth, unlisted and idempotent; local fallback retained (A-25; W2-09, W2-13, W8-04) |
| Search / trending | SCR-11 | `courses`, `enrollments`, `feedback`, `departments` | — | Indexed; p95 < 1 s (`spec §7`) |

---

## 2. What we add

The initial `V1__baseline.sql` in each service is the deployed-schema capture (38 tables in `MOOC`, 2 in `learnsphere_payments`). It contains schema only, creates a clean database, and is adopted as Flyway's baseline when an existing database has no history. Only the following forward-only migrations are allowed after it. Each group is one forward-only migration; destructive steps happen only after a verified backfill. `M-xx` is a plan identifier, not a Flyway version: versions are assigned monotonically at merge time so W2 migrations run before W4/W5 migrations.

| Migration | Adds / changes | Why |
|---|---|---|
| M-01 | `courses.department_id` FK + index; backfill from `courses.department` | A-03; `[B-F2]` |
| M-02 | `course_content.legacy_lecture_id`; copy `lectures` → `course_content`; keep `lectures` read-only | A-05; `[B-F1]` |
| M-03 | Unique `(user_id, course_id, role)` on `enrollments`; dedupe first | AC-10; `[C-F1]` |
| M-04 | Unique `(user_id, content_id)` on `user_progress`; `progress_percent NUMERIC(5,2)`; dedupe first | AC-11/12; A-08, A-10 |
| M-05 | Unique `(attempt_id, question_id)` on `student_answers`; dedupe (keep latest) first | AC-16; `[D-F4]` |
| M-06 | Convert existing `questions.extra` TEXT → JSONB; migrate `question_options`; drop collection table | A-12; `[D-F8]` |
| M-07 | Ledger integrity on existing `payments`: UNIQUE `gateway_payment_id` (dedupe first), NOT NULL `user_id`/`course_id`/`amount`, CHECK `amount > 0`, index `(status, paid_at)`, normalise status (`PENDING→CREATED`, `SUCCESS→PAID`) | A-21; `[F-F8…F-F10]` |
| M-08 | `refresh_tokens`, `audit_log`, `notification_outbox`; indexes on `user_id`, `expires_at`, `status` | A-09, A-17, §6.10 |
| M-09 | `college_entitlements` + unique `(student_id, course_id)`; `affiliation_requests` status index | A-20, A-24 |
| M-10 | Migrate `discussion_posts` → `post`; verify/enforce thread/parent FKs and indexes; drop legacy table | A-13; `[E-F1]` |
| M-11 | `test_attempts`: backfill `started_at` from `attempted_at`, add `completed_at`, constrain `status`; add `tests.created_at` | AC-18; `[D-F5, D-F11]` |
| M-12 | `course_content`: `source_type` (`YOUTUBE`/`UPLOAD`/`EXTERNAL`), `provider_video_id`, `storage_key`, `external_url`, `media_status`, `mime_type`, `size_bytes`; backfill from legacy `file_path`/lecture URLs; `(module_id, position)` index | A-25; F22; W2-12 |
| M-13 | `youtube_channel_connections` (teacher, channel, encrypted refresh token, scopes, revoked_at) and `media_publish_jobs` (content, idempotency key, status, attempts, provider video id, error code); unique/index constraints | A-25; W2-13 |
| M-14 | `tests.content_id` FK + backfill from `lectures` via `legacy_lecture_id`; `placement`, `required_for_completion` (default false), `pass_threshold` (default 60), `max_attempts` (default 3; 0 = unlimited); unique partial index for one required post-video quiz per content | A-27; F23; W4-11 |
| M-15 | `live_sessions`: `source_type` (`YOUTUBE_LIVE`/`HLS`), `provider_video_id`, `source_url` (HTTPS playback URL only), `playback_mode`, `started_at`, `ended_at`; retire the generated `join_url` placeholder; reject RTMP/stream keys | A-26; F22; W5-10 |

**Shipped so far:** `V2__audit_log.sql` implements the `audit_log` slice of M-08 (append-only table, actor/resource/created-at indexes, `actor_id → users` with `ON DELETE SET NULL`) and adds the unique `(user_id, role_id)` index on `user_roles`, deduplicating legacy rows first. `refresh_tokens` and `notification_outbox` from M-08 are still pending.

**Not added:** a separate `orders` table in the main DB (the microservice owns orders), a `question_options` replacement table (JSON is canonical), a separate `video_assets` table (one descriptor lives on `course_content`), an `is_published` column (derived projection), a second royalty ledger, and any RTMP/WebRTC ingest or recording table (A-26).

---

## 3. Gaps and how we handle them in code

| Gap | Handling |
|---|---|
| Reference says `is_published`; code has `status` | `status` is the column; DTO exposes `isPublished = status == LIVE` (A-04) |
| Reference says `course.department` FK; code has a string | Add `department_id`; keep the string as `departmentLabel` for transition; new writes require FK (A-03) |
| `Lecture` and `CourseContent` are parallel | Migrate `lectures` 1:1 into `course_content`; player/progress/quiz read content; `lectures` becomes read-only (A-05) |
| §6.8 said videos are YouTube URLs, but the teacher UI uploads local files | One `source_type` descriptor on `course_content`; private upload with signed playback, YouTube link/import or opt-in publish; no §6.8 contradiction (A-25; W2-12/W2-13) |
| Local uploads are served from a public `/api/uploads/**` path | Storage keys are private; playback uses an entitlement-bound short-lived signed descriptor; no directory listing (A-25; BR-26; W2-12) |
| Live room shows a simulated broadcast | `live_sessions` stores a validated YouTube Live/HLS source; descriptor only while `LIVE`; player shows real provider stream or an explicit unavailable state (A-26; W5-10) |
| Post-video quiz is a client-only modal tied to a lecture | `tests.content_id` + `placement=POST_VIDEO`; server computes pass and completion; one required quiz per content; retakes/limits enforced (A-27; W4-11) |
| YouTube publish/OAuth absent | Optional channel connection and idempotent unlisted upload job; token encrypted, no token in logs/cache; local fallback (A-25; W2-13) |
| Reference says no `QuestionOption` table | `Question.extra` JSON array; migrate `question_options`; validate `correctAnswer` against it (A-12) |
| `UserAccount.is_active` absent; `isEnabled()` always true | Derive `isActive = status != BLACKLISTED`; enforce `BLACKLISTED` at login (A-23) |
| JWT lacks `dept_id` | Add `dept_id` claim from the user's department; null allowed for admin (AC-05, A-09) |
| `questions.correct_answer` leaks via entity | DTOs omit it for students; legacy entity reads retired (AC-15) |
| `student_answers` is append-only | Unique `(attempt, question)` + upsert; score sums the current row only (AC-16) |
| `user_progress` has both `content_id` and `lecture_id` | `content_id` is required after M-02; `lecture_id` nullable and read-only for migration (A-05) |
| `leaderboard` table is never written | Materialise after attempt completion; XP rank is a separate labelled projection (`[C-F10, C-F11]`) |
| `payments` history is synthesised | Remove backfill; ledger rows only from verified gateway events; reconciliation report lists gaps (A-21; AUD-02) |
| Main `payments` and microservice `orders` use different status vocabularies and amount units | Normalise to `CREATED/PAID/FAILED` in the API and one documented amount unit (paise for gateway-facing DTOs); persist the existing columns until M-07, never hard-code `PAID` in UI (AUD-02; BR-21) |
| Receipt has no tax/GST data but is labelled `TAX INVOICE` | Treat it as a payment receipt; print real order/payment id, `paidAt` date + time and status; no GSTIN/tax claim until a real tax model exists (AUD-01; BR-22) |
| Teacher/admin profile fields and stats are hard-coded | Use `users`, real aggregates and `PUT /users/me`; remove or wire fake handlers; no local-only save (AUD-10…AUD-13; BR-24) |
| Royalties are auto-created from all enrolments | Remove auto-reconcile; royalty rows derive from captured ledger rows; a repair job reports, never mints (A-21) |
| `college_entitlements` absent | Add table; checkout checks it before creating an order; free enrol writes `enrollment` with `role=STUDENT`, no payment (A-20) |
| `discussion_posts` legacy model | Migrate to `discussion_thread`/`post`; old endpoints return `410` after W5-01 (A-13) |
| `Notes`/`Bookmarks` point to `Lecture` | Migrate to `course_content_id`; keep `lecture_id` only as a transitional nullable (A-15) |
| No audit trail | `audit_log` append-only; written by role/status/course-status/grade/payout/college changes (§6.10) |
| No notification events | `notification_outbox` + fixed event catalogue; poller idempotent by `(event_type, resource_id, user_id)` (A-17) |

---

## 4. Setup and test data

### 4.1 Databases

| Database | Owner | Tables |
|---|---|---|
| `MOOC` | Main backend | All academic, engagement, institution and financial-ledger tables |
| `learnsphere_payments` | Payments service | `orders`, microservice `payments` (gateway mirror) |

- PostgreSQL 16+ (the code uses standard SQL; no vendor-specific JSON beyond `jsonb`).
- Flyway runs before the app; `spring.jpa.hibernate.ddl-auto=validate`; both services use `baseline-on-migrate=true` so an existing schema is adopted at V1 without replaying it.
- Local reset: `./scripts/reset-demo.sh` drops both schemas, migrates, seeds.
- Test: Testcontainers PostgreSQL 16; each test class gets a clean schema; no H2 (`[G-F11]`).

### 4.2 Seed personas

| Persona | Email | Role | College | Purpose |
|---|---|---|---|---|
| Admin | `admin@learnsphere.com` | ADMIN | TCET (VERIFIED) | Approvals, payouts |
| Teacher | `teacher@learnsphere.com` | TEACHER | TCET (VERIFIED) | 2 LIVE courses, modules/content, test |
| Student | `student@learnsphere.com` | STUDENT | TCET (VERIFIED) | Enrolment, progress, certificate |
| External student | `external@learnsphere.com` | STUDENT | Other (VERIFIED) | Paid checkout |
| Pending teacher | `pending@learnsphere.com` | TEACHER | Pending affiliation | Authoring blocked (A-24) |

- Password: `SEED_PASSWORD` env var; the literal `Password@123` is removed (G-F10).
- Seeder runs only under `demo`/`test`; production profile leaves the schema empty.
- Deterministic ids and timestamps; no random data. The reset script prints the persona table.

### 4.3 Test fixtures

- `PaymentFixtures`: created/captured/failed orders; failed-then-successful attempts for the same course; signed webhook payloads; duplicate capture; amount-tamper case; real gateway payment id + `paidAt`; receipt data.
- `AssessmentFixtures`: MCQ/TF/TEXT questions; an in-progress attempt; a timed-out attempt; a duplicate-answer replay; a required post-video quiz with pass/fail/retake fixtures, a passed-then-failed-retake case, a completed-before-required-quiz case and a content-completion attempt.
- `CurriculumFixtures`: a LIVE course, a PENDING course, 3 modules with positions, content with durations, a YouTube playlist; an uploaded private video, a YouTube-linked video, a missing/expired signed descriptor; an enrolled and a non-enrolled student for player authorization.
- `YouTubeFixtures`: a fake OAuth connection, a queued/published/failed publish job, quota failure, idempotent replay, and a foreign job.
- `LiveMediaFixtures`: scheduled/live/ended YouTube Live and HLS sessions, a no-source session, and an ended-session descriptor attempt.
- `ProfileFixtures`: student/teacher/admin `users/me` payloads with real department/bio and save persistence.
- `CollegeFixtures`: two verified colleges, one pending, one teacher with approved affiliation, one pending.
- Every fixture is loaded through repositories/services, not raw SQL, except migration tests.

### 4.4 Retention, read models and caching

- Financial ledger rows are never hard-deleted; corrections are reversal rows. Audit rows are append-only (1-year minimum for the pilot).
- Attempts/submissions stay for the course lifetime; content deletes are `ARCHIVED`, preserving progress. Local video masters are retained after YouTube publish; binary cleanup runs only after the content retention window.
- Media playback descriptors are generated per request, expire in ≤ 5 minutes and are never stored. OAuth refresh tokens are encrypted at rest and never logged or cached. RTMP/encoder stream keys are never stored; an HLS playback URL is secret-bearing and appears only in the entitled descriptor.
- Dashboards read aggregate queries over `enrollments`, `user_progress`, `test_attempts`, `payments`; no N+1; indexed by course/user/status. Resume/player/progress queries use DTO projections and have query-count assertions (W3-13; NFR-02).
- Cache DTOs, never JPA entities. Public cache entries use after-commit invalidation and an `ETag`; cache keys include filters/role/college where the response varies (NFR-03; W2-11).

| Read model | Cache | Invalidation |
|---|---|---|
| Published catalogue + `LIVE` course detail | Caffeine, TTL ≤ 60 s, `ETag` | Course publish/update/archive, module/content edit, department write, teacher-profile update (after commit) |
| Department list/detail | Caffeine, TTL ≤ 60 s, `ETag` | Department create/update/delete (after commit) |
| Public rating average (W5-09) | Caffeine, TTL ≤ 60 s | Feedback create/update/delete |
| Trending snapshot (W8-06) | Caffeine, TTL ≤ 60 s | Enrolment or rating write |
| Course leaderboard | No shared cache; read materialised rows | Attempt-completion worker; response is `private, no-store` |
| Uploaded file download (W2-07) | None | Owner or linked-content entitlement; `private, no-store`; no directory listing |
| Media playback descriptor / signed URL (W2-12) | None | Generated per request from the stored source; `private, no-store`; expiry ≤ 5 min |
| Live playback descriptor (W5-10) | None | Generated only while `LIVE` and entitled; `private, no-store`; no stream key; HLS playback URL only inside the descriptor |
| YouTube connection/publish jobs (W2-13) | None | Own data only; `private, no-store`; tokens never returned |
| Auth, profiles, progress, attempts/answers, payments/receipts/royalties, notifications/messages, admin lists | None | Always `private, no-store`; authorization is re-evaluated per request |

- Do not negative-cache auth, publication or entitlement failures; the next request must re-check the database. Draft and staff-only course views bypass the public cache.
- Certificate rows are immutable once issued; a reissue creates a new row with a new code and `supersedes_id`.
- PII/secrets: no tokens/passwords, OAuth refresh tokens, stream keys, file keys or signed playback URLs in logs; payment payloads store gateway ids only (`spec §7`). No cached DTO may contain another user's PII, payment data, `correctAnswer`, file key or media source key.
