# C — Analysis: enrolment, progress, credentials & ranking

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** `spec §3.3`, `spec §3.6`, `spec §5` rows 9/10/15, `spec §9` Q-05/Q-08 · `AG §5`, `AG §8` · `REF p11–12, p17` · backend `Entity/{Enrollment,UserProgress,Leaderboard,Certificate}`, `Service/impl/{Enrollment,Leaderboard,Certificate}ServiceImpl`, `Controller/{Enrollment,Leaderboard,Certificate,Student}Controller`, repositories · Angular `student/services/{student,leaderboard,certificate}.ts`, `features/student/{courses,leaderboard,certificates}`
**Date of analysis:** 2026-10-22

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n |
| `AG §n` | root `AGENTS.md` section n |
| `SYS §n` / `ARCH` / `ROAD` | `Docs/learnsphere_system_design.md` · `Docs/Architecture.md` · `Docs/Future_Plans.md` |
| `code <path>:<line>` | implementation evidence |
| `derived` | analyst inference, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| `REF p11–12` | Read in full: Enrollment, UserProgress. |
| `REF p17` | Read in full: Leaderboard. |
| `AG §5, §8` | Read in full. |
| `spec §3.3, §3.6, §5, §9` | Read in full. |
| Backend | Enrollment, UserProgress, Leaderboard, Certificate entities; Enrollment/Leaderboard/Certificate service implementations; Enrollment, Leaderboard, Certificate, Student controllers; `UserProgressRepository`, `EnrollmentRepository`. |
| Frontend | `student.service.ts`, `leaderboard.service.ts`, `certificate.service.ts`, course detail and leaderboard components. |
| Does NOT exist | any leaderboard recalculation job/event; unique DB constraints for enrolment/progress; a course-completion endpoint; a progress summary beyond `totalEnrolled`. |

---

## 1. The reference contract

1. **Enrollment** — `enrollment_id`, `user_id` not null, `course_id` not null, `role ∈ {STUDENT, INSTRUCTOR}`, `enrolled_at` immutable; **unique `(user_id, course_id, role)`**; no status/completion/certificate fields (`REF p11`; `AG §5`).
2. **UserProgress** — `progress_id`, `user_id` not null, `content_id` not null, `completed_at` nullable, `progress_percent` `BigDecimal(5,2)`, `seconds_watched`; **unique `(user_id, content_id)`**; upsert; `completed_at` set when `progressPercent >= 90`; overall course completion computed on the fly (`REF p12`; `AG §5`).
3. **Leaderboard** — `leaderboard_id`, `user_id`, `course_id`, `avg_score` `BigDecimal(6,2)`, `global_rank`, `dept_rank`, `updated_at`; **unique `(user_id, course_id)`**; recalculated asynchronously after a `TestAttempt` completes (`REF p17`; `AG §8`).
4. **Certificate** — preserved feature (`AG §4`), not in the reference's 10 domains: issued per user/course/type, unique verification code, public verification (`spec §3.6`, `SCR-21`).

---

## 2. Enrolment is a bare insert

5. `Enrollment` is mapped to `enrollments`, PK `enrollmentId`, but the user is a raw `Long userId`, not a `@ManyToOne` (`code Entity/Enrollment.java:9–18`). The reference's FK relationship and cascade behaviour are absent.
6. `@Table(name = "enrollments")` declares **no unique constraint** on `(user_id, course_id, role)` (`Enrollment.java:7`). `EnrollmentServiceImpl.enrollUser` only sets `enrolledAt` and saves (`:24–27`), and `POST /api/enrollments` accepts the whole entity from the client, filling `userId` from the principal only when it is null (`Controller/EnrollmentController.java:36–44`). **derived:** a caller can enrol another user by sending their `userId`, and can create duplicate rows. AC-10 and the reference's unique rule are both unmet.
7. `DELETE /api/enrollments/{id}` deletes by id with **no ownership check** (`EnrollmentController.java:56–59`); any authenticated user can unenrol any student.
8. `GET /api/courses/{courseId}/enrollments` returns all enrolments for a course with no admin/teacher check (`:61–64`). Course rosters and user ids leak to any authenticated caller.
9. There is no enrolment status, no completion field and no certificate URL — which matches the reference (it explicitly says those fields do not exist, `REF p11` note) but means all derived state must be computed (AC-13).

## 3. Progress has the right fields, the wrong guarantees

10. `UserProgress` has `userId` raw, `content` (`@ManyToOne`), **`lecture`** (`@ManyToOne`), `completedAt`, `progressPercent` as `Integer`, `secondsWatched` (`code Entity/UserProgress.java`). The reference has no `lecture_id`; this is an extension for the player (A-05).
11. The entity declares **no unique constraint** on `(user_id, content_id)` and none on `(user_id, lecture_id)` (`UserProgress.java:6–24`). `POST /api/progress` accepts a full entity and calls `save` (`EnrollmentServiceImpl.updateUserProgress`, `:30–35`). **derived:** repeated posts create duplicate rows; `findByUserIdAndContent_ContentId` then throws `NonUniqueResultException` or returns an arbitrary row. AC-11 is not met.
12. Completion is set at `progressPercent >= 100`, not 90 (`EnrollmentServiceImpl.java:31–33`). The player path also writes 100 (`StudentController.markLectureComplete`: `setProgressPercent(100)`). This matches the audit's "backend 100%" note (`AUD-VP2 §7.1`) but contradicts `REF p12`. `Integer` also contradicts `BigDecimal(5,2)`.
13. `POST /api/progress` trusts a client-supplied `userId` when present, exactly like enrolment (`EnrollmentController.java:70–78`). A student can write progress for another student.
14. Lecture progress is the real completion signal: enrolled-course DTOs count lectures with `progressPercent >= 100` (`StudentController.getEnrolledCourses`), and certificates require every lecture at 100 (`CertificateServiceImpl.assertStandardEligibility`). Content-based progress (the reference model) is not used for completion.
15. `GET /api/progress/course/{courseId}/summary` returns only `{courseId, totalEnrolled}` (`EnrollmentController.java:96–103`), not a per-student progress summary. The reference endpoint is only partially implemented.

## 4. Certificates work, but on lecture progress only

16. `Certificate` stores `verificationCode` unique, `studentName`, `instructorName`, `grade`, `score` (Double), `status`, `issuedAt`, and a type (`STANDARD`/`REMEDIAL`) (`Entity/Certificate.java`).
17. `issueCertificate` is idempotent per `(user, course, type)`; STANDARD requires **all lectures at 100 %**; REMEDIAL requires ≥40 % on a completed remedial test (`CertificateServiceImpl.issueCertificate`, `assertStandardEligibility`, `assertRemedialEligibility`). Codes are `LS-<year>-<8 hex>`.
18. `GET /api/certificates/verify/{code}` is public and returns holder/course/grade/status (`CertificateController`, `SecurityConfig` permit for GET). SCR-06 is implemented.
19. `grade` is hard-coded ("Distinction" / "Passing Grade") and `score` is never populated in the issue path. **derived:** certificate content is not evidence of the actual score.

## 5. Two leaderboards, neither authoritative

20. `GET /api/leaderboard?scope=global|college` is **XP-based**: it sorts `UserAccount.leaderboardPoints` and returns rank/name/points (`LeaderboardServiceImpl.getLeaderboard`). It never reads the `leaderboard` table and never filters by course.
21. `GET /api/courses/{courseId}/leaderboard` returns `LeaderboardRepository.findByCourse_IdOrderByAvgScoreDesc(courseId)` (`LeaderboardController`). The `Leaderboard` entity has the right shape and unique constraint (`Entity/Leaderboard.java`), but **nothing writes to it** — no service, job or event calls `save`.
22. `GET /api/leaderboard/me` returns rows from the same empty table. **derived:** the course leaderboard and "my rank" are empty in a fresh database; the UI's course leaderboard panel will always be empty.
23. `UserAccount` carries `leaderboardPoints`, `attendanceScore` and `royaltyBalance`, all mutable fields with no ledger. **derived:** XP can drift from actual activity and there is no audit trail.
24. `ROAD` claims "Academic XP & Leaderboards — Completed" and `PROG` claims "Gamification & Points … batch, course, and college-wide leaderboards"; the code supports global/college XP and an unwritten course table. This is a documentation/implementation gap (F).

---

## 6. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|
| C1 | `enrollments` has no unique constraint and no published check | duplicate enrolments and enrolment in drafts (AC-10) |
| C2 | `POST /api/enrollments` accepts `userId` from the body | one user can enrol another |
| C3 | `DELETE /api/enrollments/{id}` and the roster endpoint have no ownership check | any authenticated user can unenrol or list a course roster |
| C4 | `user_progress` has no unique constraint; `save` is not an upsert | duplicates, non-deterministic reads, AC-11 fails |
| C5 | `progressPercent` is `Integer`; completion is 100 %, not 90 % | contradicts the reference and the spec's AC-12 |
| C6 | `POST /api/progress` trusts body `userId` | progress spoofing |
| C7 | Course completion is lecture-based, not content-based | the reference's progress model and the player's model disagree |
| C8 | `/progress/course/{id}/summary` only counts enrolments | no real summary for teachers/admins |
| C9 | Certificates require 100 % of lectures and hard-code the grade | content vs. rule mismatch; misleading credential |
| C10 | Course `leaderboard` table is never written; XP ranking is separate | empty course leaderboard, two competing ranks |
| C11 | No async recalculation after `TestAttempt` completion | `AG §8` requirement unimplemented |

## 7. Gaps — what the sources do NOT provide

- **NO FIELD** for enrolment status/completion on `enrollment` (the reference says so explicitly).
- **NO RULE** for what happens when a student is unenrolled after earning progress or a certificate.
- **NO DEFINITION** of XP values (the README lists +10/+25/+50/+5, but nothing in the reference or code enforces them).
- **NO CONTRACT** for certificate revocation beyond `status`; no endpoint sets it.
- **NO SPEC** for whether `dept_rank` is college-scoped or department-scoped; the reference says "same department", the UI says "college-wide" (`PROG`).
- **NO ENDPOINT** to compute overall course completion for one student (only lecture counts in DTOs).

## 8. Open questions

| ID | Question | Feeds |
|---|---|---|
| C-Q1 | Is completion 90 % or 100 %? | Q-08 → A-08 |
| C-Q2 | Which progress model is authoritative: lecture or content? | Q-05 → A-05 |
| C-Q3 | Should enrolment/progress gain DB unique constraints in this phase? | PLAN Wave 3 |
| C-Q4 | Is the course leaderboard required in this phase, or is XP-only acceptable? | PLAN Wave 3 |
| C-Q5 | Should certificate grade/score reflect actual performance? | PLAN Wave 3 |
