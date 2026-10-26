# LearnSphere AI — Specification

**Version:** v1.0.1 · 2026-10-22
**Status:** frozen. v1.0.1 is an editorial correction (cross-reference + route-coverage note); no
contract changed. Later contract changes are appended as v1.1 …; substantive sections are never
rewritten. Editorial corrections are logged here and do not require a new assumption.
**Revision log:** v1.0.1 — corrected AC-06's cross-reference to F4; added the §4 coverage note
(61 client route entries → 47 distinct screens).

**Provenance:** consolidated from `AGENTS.md` §1–§10 (`AG §n`), `LMS_Entity_Reference.pdf` pages 3–22
(`REF p n`), and the non-PDF preservation rules in `AG §4`. It is the normalised contract that every
later document cites. Where the two sources disagree, the reference PDF governs the persistence
contract and `AGENTS.md` governs the non-PDF capabilities (recorded as findings, not fixed here).

---

## 1. Overview & goals

LearnSphere AI is a college-first learning management platform. Faculty publish video courses,
modules, content and assessments; students enrol, watch, take tests, submit assignments and earn
certificates; administrators govern users, courses, colleges, revenue and moderation.

**Goal of v1:** a single institution can run its courses end to end — author → publish → enrol →
learn → assess → certify — with payments for external students and a faculty royalty ledger.

**Success in 3 months (derived):** one real department runs one real course end to end; students see
progress and certificates; faculty see royalties; admins can audit every action.

---

## 2. Users & roles

| Role | Scope | Can |
|---|---|---|
| `STUDENT` | global role | browse published courses, enrol, consume content, attempt tests, submit assignments, comment, leave feedback, earn certificates |
| `INSTRUCTOR` | global role (**implemented as `TEACHER`**, Q-01) | create/own courses, modules, content, assignments, tests, grade, see enrolled students, royalties |
| `ADMIN` | global role | all of the above + user governance, course approval, colleges, revenue, payouts, moderation |
| course-scoped role | `enrollment.role` = `STUDENT` or `INSTRUCTOR` | distinguishes the learner from the teacher inside one course |

**AC-01** A deactivated account cannot authenticate.
**AC-02** A user's password is never present in any API response.
**AC-03** Only an admin can assign or revoke a global role.
**AC-04** Only the course creator (or an admin) can mutate that course's curriculum.

---

## 3. Core journeys

### 3.1 Register and sign in (SCR-02, SCR-03)
Trigger: new visitor. Steps: register (name, email, password, role, department) → BCrypt hash →
`user_account` row → `user_role` row → JWT → redirect to role dashboard.
Outcome: authenticated session. Failure: duplicate email → 409; deactivated account → 401.
**AC-05** Login returns an access token; the token carries `userId`, `roles`, `dept_id`.
**AC-06** Access token lives 15 minutes; a refresh token (7 days) rotates it. *(Current code differs — F4/A-09.)*

### 3.2 Author a course (SCR-31, SCR-32, SCR-33)
Trigger: instructor. Steps: create course (draft) → add modules in order → add `CourseContent`
(VIDEO/PDF/QUIZ) and `Assignment` per module → submit for approval → admin publishes.
Outcome: a `LIVE` course visible in the catalogue. Failure: non-owner mutation → 403; invalid content
type → 400; past due date → 400.
**AC-07** Modules and content are returned ordered by `position`.
**AC-08** Deleting a module re-normalises the remaining positions to 1..n.
**AC-09** An assignment has `maxMarks > 0` and, on creation, a future `dueDate`.
**AC-10** A course cannot be enrolled in unless published.

### 3.3 Learn a course (SCR-12, SCR-13, SCR-14, SCR-15)
Trigger: student opens an enrolled course. Steps: see modules/contents → play a video → progress is
upserted → notes and bookmarks at timestamps → post-video quiz.
Outcome: progress persisted; course completion derived. Failure: not enrolled → 403.
**AC-11** One progress row per `(user, content)`; repeat calls update, never duplicate.
**AC-12** `completedAt` is set when `progressPercent >= 90` (REF p12) and progress is clamped to 100.
**AC-13** Course completion is computed on the fly, never stored on `enrollment`.
**AC-14** A note/bookmark always belongs to a user and a content/lecture item.

### 3.4 Assess (SCR-16, SCR-17, SCR-35)
Trigger: student starts a test. Steps: `POST /tests/{id}/attempts` → `IN_PROGRESS` → answers →
submit → server grades MCQ/TF, leaves TEXT for the instructor → attempt `COMPLETED`.
Outcome: score and per-question marks. Failure: no attempt / wrong owner → 404; past
`startedAt + durationMinutes` → `ABANDONED`; before `scheduledAt` → 403.
**AC-15** `correctAnswer` is never exposed before submission.
**AC-16** Score is computed server-side; client-supplied scores are ignored.
**AC-17** TEXT answers stay ungraded (null) until an instructor grades them.
**AC-18** Attempt timeout is enforced server-side, never by the browser.

### 3.5 Pay and enrol (SCR-07, SCR-08)
Trigger: external student buys a course. Steps: create order → Razorpay checkout → verify signature →
`/internal/enroll` → enrollment + payment + 70 % royalty credit.
Outcome: student enrolled, teacher balance credited, payment recorded. Failure: bad signature → 400;
already enrolled → 409; free course → 400 (use direct enrol).
**AC-19** Payment verification fails closed on an invalid signature.
**AC-20** Webhook processing is idempotent per `razorpay_payment_id`.
**AC-21** The teacher receives 70 % of the paid amount; the platform retains 30 %.

### 3.6 Discuss, review, certify (SCR-18, SCR-19, SCR-21)
Trigger: student finishes a course / has a question. Steps: post a thread/reply, leave one review per
course, request a certificate when every lecture is complete → verification code → public verify page.
**AC-22** Only the author or an admin may edit/delete a thread, post or review.
**AC-23** One feedback row per `(user, course)`; rating is 1–5.
**AC-24** A certificate is issued only when the course is complete; its code verifies publicly.

---

## 4. Screens

> Screen IDs are stable. Grouped sub-screens share one ID when they share one purpose.
>
> **Coverage:** the client has 61 route entries; this spec defines 47 distinct screens. IDs not
> defined here (09, 27–29, 39–40, 42–49) are **derived** to be route aliases, redirects or legacy
> entries; W7-06 maps each to a canonical screen or retires it (`schema-usage.md` §1).

### SCR-01 — Landing & public catalogue
Purpose: explain the product and list live courses. Shows: hero, value props, course cards, ratings.
Actions: browse, open course (redirects to login for detail actions), register. States: empty
catalogue, loading skeletons, API error.

### SCR-02 — Login
Purpose: authenticate. Shows: email, password, role-aware redirect. Actions: sign in, go to register,
forgot password. States: invalid credentials, deactivated, loading.

### SCR-03 — Register
Purpose: create an account. Shows: name, email, password, role picker, department/college.
Actions: submit, go to login. States: duplicate email, weak password, validation errors.

### SCR-04 — Forgot / reset password
Purpose: recover access. Shows: email field; reset form with token. Actions: request link, set new
password. States: unknown email (same response), expired token.

### SCR-05 — College verification
Purpose: attach a student to a college or submit a teacher affiliation request. Shows: college
picker, department, document URL for teachers. Actions: submit. States: pending, approved, rejected.

### SCR-06 — Certificate verification (public)
Purpose: prove a certificate is genuine. Shows: holder, course, grade, date, status. Actions: enter
code. States: valid, invalid, revoked.

### SCR-07 — Checkout
Purpose: pay for a course. Shows: course summary, price, Razorpay widget. Actions: pay, cancel.
States: payment pending, failed, already enrolled.

### SCR-08 — Payment success
Purpose: confirm enrolment. Shows: success message and next-step link. States: pending verification.

### SCR-10 — Student dashboard
Purpose: daily learning home. Shows: enrolled courses, progress, XP, upcoming assessments, live
sessions, announcements. States: no enrolments.

### SCR-11 — Browse courses
Purpose: find a course. Shows: filters (department, search), cards, price, rating. Actions: open,
enrol. States: empty result, loading.

### SCR-12 — Course detail
Purpose: decide and enter a course. Shows: curriculum (modules/content), reviews, course leaderboard,
progress, certificate state. Actions: enrol, pay, start lecture, write review. States: not enrolled,
enrolled, completed.

### SCR-13 — Lecture player
Purpose: consume a lecture. Shows: YouTube/file player, transcript, notes drawer, bookmarks, discussion,
post-video quiz. Actions: play/pause, seek, speed, fullscreen, save note/bookmark, generate quiz.
States: buffering, ended, progress-save error.

### SCR-14 / SCR-15 — Notes / bookmarks
Purpose: revisit timestamped annotations. Shows: list with course/lecture filter. Actions: edit,
delete, jump to timestamp. States: empty.

### SCR-16 / SCR-17 — Assessments / quiz attempt
Purpose: take and review a test. Shows: available tests, timer, question palette, answers; result and
per-question feedback after submission. Actions: start, answer, submit. States: scheduled, in
progress, completed, abandoned, review.

### SCR-18 — Leaderboard
Purpose: rank learners. Shows: global / college / course scopes, XP, podium. States: empty, no college.

### SCR-19 — Discussion
Purpose: course/lecture Q&A. Shows: threads, nested replies, composer. Actions: create, edit, delete.
States: empty, locked, error.

### SCR-20 — Live sessions
Purpose: join a live class. Shows: scheduled sessions, join link/embed, chat. States: scheduled, live,
ended.

### SCR-21 / SCR-22 — Certificates / purchases
Purpose: credentials and payment history. Shows: issued certificates, download, verify link; orders,
status. States: none.

### SCR-23 / SCR-24 / SCR-25 — Messages / notifications / announcements
Purpose: communication. Shows: conversations, unread counts, announcement feed. Actions: send, mark
read, pin (staff). States: empty, unread badge.

### SCR-26 / SCR-41 / SCR-61 — Profile & settings (all roles)
Purpose: manage identity. Shows: avatar, bio, department/college, password, theme. Actions: save.
States: validation errors.

### SCR-30 — Teacher dashboard
Purpose: teaching home. Shows: courses, students, revenue/royalties, pending grading, trending.
States: no courses.

### SCR-31 / SCR-32 — My courses / course editor
Purpose: author a course. Shows: course list; editor with modules, lectures, contents, assignments,
reorder. Actions: create, edit, publish/submit, delete. States: draft, pending, live, archived.

### SCR-33 — Upload
Purpose: add material. Shows: video upload, resource upload, YouTube import, quiz builder. Actions:
upload, import playlist, generate quiz with AI. States: uploading, processing, failed.

### SCR-34 — Students & standings
Purpose: monitor a cohort. Shows: enrolled students, progress, scores, at-risk flags. Actions: nudge,
export. States: empty.

### SCR-35 — Teacher assessments
Purpose: author and grade tests. Shows: tests per course/lecture, questions, submissions. Actions:
create test, add/bulk-add questions, grade answers. States: no questions.

### SCR-36 / SCR-37 / SCR-38 — Live / trending / royalties
Purpose: run the teaching business. Shows: live sessions, trending courses, royalty balance and
payout history. States: empty, pending payout.

### SCR-50 — Admin dashboard
Purpose: command centre. Shows: users, courses, revenue, moderation, medals, activity. States: empty.

### SCR-51 — User governance
Purpose: manage accounts. Shows: users, role distribution, status. Actions: activate/deactivate,
assign role. States: self-protection guard.

### SCR-52 — Course approvals
Purpose: publish or reject courses. Shows: pending courses, curriculum preview. Actions: approve,
archive. States: none pending.

### SCR-53 / SCR-54 — Revenue / reports
Purpose: financial and operational visibility. Shows: monthly revenue, role distribution, course
performance, CSV export. States: no data.

### SCR-55 — Payouts
Purpose: pay faculty. Shows: pending royalties, balances, history. Actions: pay one, process all.
States: nothing due.

### SCR-56 / SCR-57 — Moderation / sentiment
Purpose: keep the community safe. Shows: flagged content, severity, sentiment trend. Actions:
resolve, dismiss. States: empty queue.

### SCR-58 — Colleges & affiliations
Purpose: manage institutional partners. Shows: colleges, verification state, teacher requests.
Actions: add, verify, approve/reject. States: pending request.

### SCR-59 / SCR-60 — Announcements / messages (admin)
Purpose: broadcast and converse. Shows: composer, audience, threads. Actions: publish, pin, send.
States: empty.

---

## 5. Data model

> Source of truth: `REF p3–p22` + `AG §1–§10`. Column-level detail is in the reference; this section
> fixes the entities, keys and lifecycle rules.

| # | Entity | Table | PK | Key rules |
|---|---|---|---|---|
| 1 | Department | `department` | `department_id` **Integer** | name unique/not null; createdAt immutable |
| 2 | Role | `role` | `role_id` **Integer** | name unique (`ADMIN`, `INSTRUCTOR`, `STUDENT`); seeded |
| 3 | UserAccount | `user_account` | `user_id` Long | email unique; BCrypt password; `is_active` default true; lastLogin |
| 4 | UserRole | `user_role` | `user_role_id` Long | unique `(user_id, role_id)`; assignedAt immutable |
| 5 | Course | `course` | `course_id` Long | title not null; `is_published` default false; creator; department FK |
| 6 | Module | `module` | `module_id` Long | course FK not null; ordered `position`; renormalise on delete |
| 7 | CourseContent | `course_content` | `content_id` Long | module FK; type VIDEO/PDF/QUIZ; position; durationSeconds |
| 8 | Assignment | `assignment` | `assignment_id` Long | module FK; maxMarks > 0; dueDate future; UTC |
| 9 | Enrollment | `enrollment` | `enrollment_id` Long | unique `(user_id, course_id, role)`; role STUDENT/INSTRUCTOR |
| 10 | UserProgress | `user_progress` | `progress_id` Long | unique `(user_id, content_id)`; upsert; completedAt at ≥90 % |
| 11 | Test | `test` | `test_id` Long | course FK; scheduledAt gates access; duration enforced server-side |
| 12 | TestAttempt | `test_attempt` | `attempt_id` Long | IN_PROGRESS → COMPLETED / ABANDONED; score server-side |
| 13 | Question | `question` | `question_id` Long | MCQ/TEXT/TF; marks > 0; options + hints in `extra` JSON; **no options table** |
| 14 | StudentAnswer | `student_answer` | `student_answer_id` Long | attempt+question; marksAwarded null until graded |
| 15 | Leaderboard | `leaderboard` | `leaderboard_id` Long | unique `(user_id, course_id)`; avgScore, ranks, updatedAt |
| 16 | AssignmentSubmission | `assignment_submission` | `submission_id` Long | unique `(student_id, assignment_id)`; text or file required |
| 17 | Feedback | `feedback` | `feedback_id` Long | unique `(user_id, course_id)`; rating 1–5 |
| 18 | DiscussionThread | `discussion_thread` | `thread_id` Long | title+content required; cascade posts |
| 19 | Post | `post` | `post_id` Long | thread+user required; `parent_post_id` self-reference |
| + | Preserved non-PDF entities | `college`, `affiliation_request`, `lecture`, `note`, `bookmark`, `live_session`, `certificate`, `announcement`, `conversation`, `message`, `notification`, `payment`, `royalty`, `payout`, `flagged_content` | — | Must not be deleted (AG §4) |

**Statuses:** course `DRAFT | PENDING | LIVE | ARCHIVED` (derived from `is_published`); attempt
`IN_PROGRESS | COMPLETED | ABANDONED`; payment `PENDING | SUCCESS | FAILED`; order
`CREATED | PAID | FAILED`.

---

## 6. Integrations

| Integration | Direction | Contract |
|---|---|---|
| YouTube Data API v3 | outbound | import playlist → lectures/content; transcript → AI quiz; embed playback |
| NVIDIA NIM (`meta/llama-3.1-70b-instruct`) | outbound | extract questions from transcript, summarise transcript, chatbot; graceful fallback when unconfigured |
| Razorpay | outbound + webhook | create order, verify HMAC signature, `payment.captured` / `payment.failed` |
| Payments microservice → main backend | internal HTTP | `POST /internal/enroll` with `X-Internal-Key`; returns enrollment + royalty credit |
| WebSocket STOMP `/ws` | inbound | live chat, notifications; JWT validated on handshake |
| Local file storage `/uploads` | both | course videos/resources, avatars; 200 MB limit |

---

## 7. Non-functional requirements

| Area | Requirement |
|---|---|
| Auth | stateless JWT; access 15 min, refresh 7 days; access token in memory only (never localStorage) |
| Passwords | BCrypt; min length validated server-side |
| Authorization | role guards on every mutating endpoint; ownership checks for course/curriculum/content |
| Data | PostgreSQL; forward-only migrations in the target state; `BigDecimal(5,2)` for percent, `(8,2)` for marks, `(6,2)` for avg |
| Files | local disk until cloud storage lands; type/size validation; no executable uploads |
| Performance | catalogue and dashboards p95 < 1 s on seed data; AI calls time out and fall back |
| Accessibility | keyboard reachable, visible focus, colour contrast AA, responsive at 360 / 768 / 1280 px |
| Privacy | no password/correct-answer leakage; payment data never stored in plain text beyond gateway ids |
| Operations | one-command local run; seeded demo users; health endpoint; no secrets in the repo for production |

---

## 8. Out of scope (later / never)

| Item | Decision | Why |
|---|---|---|
| SMTP / transactional email | **later** | password reset currently logs the token; needs a provider |
| Cloud object storage (S3/GCS) | **later** | local `/uploads` is sufficient for the pilot |
| Fine-tuned sentiment classifier | **later** | keyword lexicon ships first |
| At-risk intervention cron worker | **later** | needs reliable progress data first |
| Coding sandbox | **later** | separate execution infrastructure |
| LTI 1.3 / ERP sync | **later** | enterprise integration, not pilot |
| Native mobile apps | **later** | responsive web first |
| ExamGuard client | **later** | only the `security_policy` string exists today |
| Multi-tenant isolation | **never in v1** | single-institution deployment; colleges are a grouping, not a tenant boundary |
| Refunds / chargebacks | **later** | finance process not specified |

---

## 9. Open questions

Every question is resolved by a numbered assumption in `PLAN.md` §3. None stays open.

| ID | Question | Resolved by |
|---|---|---|
| Q-01 | Global role is `INSTRUCTOR` in the reference but `TEACHER` in code — which wins? | A-01 |
| Q-02 | Table names are singular in the reference but plural in the running schema — rename or map? | A-02 |
| Q-03 | `course.department` is a FK in the reference but a string in code — which is authoritative? | A-03 |
| Q-04 | Is publication `is_published` (boolean) or `status` (`DRAFT…LIVE`)? | A-04 |
| Q-05 | `Lecture` and `CourseContent` are parallel curriculum models — merge or bridge? | A-05 |
| Q-06 | Two payment rails exist (simulated main backend vs Razorpay microservice) — which is production? | A-06 |
| Q-07 | Royalty split is 70/30 in code/architecture but 80/20 in `README.md` — which? | A-07 |
| Q-08 | Completion is ≥90 % in the reference but 100 % in progress/certificate code — which? | A-08 |
| Q-09 | Access tokens live 24 h and are stored in localStorage, contrary to the hard rule — fix now or defer? | A-09 |
| Q-10 | `Score`/`marksAwarded` are `Integer` in code but `BigDecimal(8,2)` in the reference — migrate? | A-10 |
| Q-11 | Registration lets a user pick `TEACHER`/`ADMIN` — is self-service role selection intended? | A-11 |
| Q-12 | AI MCQ options go to `question_options` (code) but must live in `extra` (reference) — which storage wins? | A-12 |

---

## 10. Version history

| Version | Date | Change |
|---|---|---|
| v1.0 | 2026-10-22 | Initial frozen consolidation of `AGENTS.md`, the entity reference PDF and the preserved non-PDF features. |
| v1.0.1 | 2026-10-22 | Editorial: corrected AC-06 cross-reference; added §4 coverage note (61 route entries / 47 screens). No contract change. |
