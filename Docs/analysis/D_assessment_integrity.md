# D — Analysis: assessment, grading & integrity

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** `spec §3.4`, `spec §5` rows 11–14, `spec §6`, `spec §9` Q-10/Q-12 · `AG §6–7`, `AG §4` (AI quizzes) · `REF p13–16` · backend `Entity/{Test,TestAttempt,Question,StudentAnswer}`, `Service/impl/TestServiceImpl`, `Controller/{Test,Assessment,TeacherAssessment,Ai}Controller`, `Dto/AssessmentDtos`, `Service/NvidiaNimService` · Angular `features/student/assessments/**`, `features/teacher/upload/components/quiz-builder`, `core/models/assessment.model`
**Date of analysis:** 2026-10-22

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n |
| `AG §n` | root `AGENTS.md` section n |
| `SYS §6` / `ROAD` | `Docs/learnsphere_system_design.md` §6 · `Docs/Future_Plans.md` |
| `AUD-VP2` | `Docs/VIDEO_PLAYER_LEARNING_FLOW_AUDIT_V2.md` |
| `code <path>:<line>` | implementation evidence |
| `derived` | analyst inference, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| `REF p13–16` | Read in full: Test, TestAttempt, Question, StudentAnswer. |
| `AG §6–7` | Read in full, incl. "no separate QuestionOption table" and the correct-answer secrecy rule. |
| `spec §3.4, §5, §6, §9` | Read in full. |
| Backend | All four entities; `TestServiceImpl` (full); `TestController`, `AssessmentController`, `TeacherAssessmentController` (full); `AssessmentDtos`; `Question`/`Test` JSON annotations; `AiController`. |
| Frontend | `student/services/assessment.service.ts`, quiz component/review, teacher quiz builder. |
| Does NOT exist | server-side timer, `ABANDONED` transition, scheduled-access check, attempt limit, per-question answer upsert, ExamGuard enforcement, `createdAt` on `test`. |

---

## 1. The reference contract

1. **Test** — `test_id`, `course_id` not null, `title`, `duration_minutes`, `security_policy`, `scheduled_at` nullable, `created_at`. If `scheduled_at` is set the test is inaccessible before it; duration is enforced server-side; no `totalMarks`, `passingMarks`, `maxAttempts` or `isPublished` (`REF p13`; `AG §6`).
2. **TestAttempt** — `attempt_id`, `user_id` not null, `test_id` not null, `started_at`, `completed_at`, `score` `BigDecimal(8,2)`, `status ∈ {IN_PROGRESS, COMPLETED, ABANDONED}`. `IN_PROGRESS → COMPLETED` on submit, `→ ABANDONED` at `startedAt + durationMinutes`. Score computed server-side from answers (`REF p14`; `AG §6`).
3. **Question** — `question_id`, `test_id` not null, `body`, `question_type ∈ {MCQ, TEXT, TF}`, `marks > 0`, `correct_answer`, `extra` (TEXT JSON: options, hints). **No separate options table.** `correct_answer` never sent pre-submission; MCQ options in `extra`; TF answer `true`/`false` (`REF p15`; `AG §7`).
4. **StudentAnswer** — `student_answer_id`, `attempt_id` not null, `question_id` not null, `answer_text`, `marks_awarded` `BigDecimal(8,2)` null until graded; MCQ/TF auto-graded, TEXT manual; sum of marks = attempt score; no `selectedOptionId`/`isCorrect`/`answeredAt` (`REF p16`; `AG §7`).

---

## 2. There are three assessment surfaces

5. **`/api/assessments`** — the live student UI (`AssessmentController`). It uses `TestService` but returns DTOs; `TestDetailDto` → `QuestionDto` **excludes `correctAnswer`** (`Dto/AssessmentDtos.java`), and `assertOwnedAttempt` checks the attempt belongs to the caller (`Controller/AssessmentController.java:96–103`). This is the safe path.
6. **`/api/tests`** — the reference-shaped API (`TestController`). It returns raw `Question` entities, so `GET /tests/{id}/questions` and `GET /questions/{id}` expose `correctAnswer` to any authenticated caller (`Entity/Question.java:26–28` has no `@JsonIgnore`; `Controller/TestController.getTestQuestions`). It also lets any authenticated user create/update/delete tests and questions. This is an unguarded parallel surface.
7. **`/api/teacher/tests`** — teacher authoring (`TeacherAssessmentController`). It validates course ownership before creating a test/question (`assertOwnedCourse`), but `GET /tests/lecture/{lectureId}` does **not** check ownership (`Controller/TeacherAssessmentController.getTestsByLecture`).
8. **derived:** three surfaces with different guards and shapes means the same rule can be true on one path and false on another. The student UI is safe from the answer leak; the reference API is not.

## 3. Entity drift

9. `Test` adds `lecture_id` and `isRemedial` (needed for the player and remedial certificates) but has **no `created_at`** (`Entity/Test.java:12–33`). The reference requires it.
10. `TestAttempt` stores `userId` as a raw `Long`, `score` as `Integer`, `attemptedAt`, and `status` as a string (`Entity/TestAttempt.java:12–25`). It has **no `startedAt`/`completedAt`**, so a timeout cannot be computed from the entity. `attemptedAt` is the only timestamp and is never updated on completion.
11. `Question` adds an `@ElementCollection` mapped to a `question_options` table (`Entity/Question.java:21–24`) — the reference explicitly forbids a separate options table and requires `extra` JSON (`REF p15`; `AG §7`). `extra` also exists (`:30–31`) but is not used by the UI.
12. `marks` is `Integer` with no validation in `addQuestionToTest`; `marksAwarded`/`score` are `Integer` instead of `BigDecimal(8,2)` (`Entity/StudentAnswer.java`, `TestAttempt.java`). `spec §9` Q-10.
13. `Question.questionType` is a free string; `MCQ`/`MULTIPLE_CHOICE` and everything else are handled in grading, but `TF` is not special-cased. **derived:** a `TF` answer is graded by the MCQ branch if the stored text matches, otherwise it falls into the "any non-empty answer" branch (see §5).

## 4. Attempt lifecycle is incomplete

14. `startTestAttempt` creates `IN_PROGRESS` with `attemptedAt` (`TestServiceImpl.startTestAttempt`). There is no check that the test is `scheduledAt <= now`, no check that the student is enrolled, and no limit on the number of attempts.
15. `finalize` grades whatever answers exist, sets `score` and `COMPLETED` (`TestServiceImpl.calculateAndFinalizeScore`). It never sets `completedAt`, never validates elapsed time, and never transitions to `ABANDONED`.
16. `durationMinutes` and `scheduledAt` are stored but never read by the service. The only timer is in the browser. **derived:** a student can start a test, wait hours, and submit; AC-18 fails.
17. `finalize` can be called repeatedly on an already `COMPLETED` attempt; it re-grades and re-saves. No guard, no idempotency.

## 5. Grading is wrong for free text, and answers are append-only

18. MCQ: trimmed case-insensitive equality against `correctAnswer`; correct → full marks (`TestServiceImpl.java:118–127`). TF is not handled separately, so it only scores correctly if the client sends exactly the stored text.
19. **Every non-MCQ answer earns full marks if non-empty** (`TestServiceImpl.java:129–131`, comment: "no reliable auto-grading, so any non-empty answer earns full marks"). This contradicts `REF p16` and AC-17: TEXT must stay ungraded until an instructor grades it.
20. `submitAnswer` always inserts a new `StudentAnswer` (`TestServiceImpl.submitAnswer`). The student UI submits on each answer change (`assessment.service.ts:44–50`; quiz component calls it per change). `calculateAndFinalizeScore` loops **all** rows and sums the awarded marks (`:107–145`). **derived:** changing an answer twice can award marks twice; a 10-mark MCQ re-answered 3 times can produce 30. This is a critical integrity bug and the most valuable test target.
21. There is no uniqueness constraint on `(attempt_id, question_id)` in `StudentAnswer`, and no upsert.
22. Manual grading exists only on the legacy API: `PATCH /api/answers/{id}/grade` (`TestController.gradeAnswer`) writes `marksAwarded` from the body with no ownership/course check. The live `/api/assessments` path has no instructor grading endpoint.

## 6. AI generation and ExamGuard

23. `AiController` exposes `POST /api/ai/extract-questions` and `/summarize-transcript` publicly (`SecurityConfig` permits `/api/ai/**`), and `NvidiaNimService` falls back to curated questions when NVIDIA NIM is unconfigured or fails (`SYS §6`). This satisfies the preserved AI quiz rule (`AG §4`).
24. The AI response is consumed by the teacher quiz builder and persisted through `TeacherAssessmentController` bulk-add. **derived:** AI questions land in `question_options`, not `extra`, so the reference's storage rule is broken at the point of creation.
25. `securityPolicy` is a free string on `Test`; nothing reads it. There is no ExamGuard client or enforcement in this repo. `ROAD` does not list ExamGuard as complete.

---

## 7. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|
| D1 | `correctAnswer` is exposed by the legacy `/api/tests/{id}/questions` and `/api/questions/{id}` endpoints | students can read answers before submitting |
| D2 | The legacy `/api/tests` write endpoints require only authentication | any student can author/delete tests and questions |
| D3 | `TEXT` answers are auto-awarded full marks | violates AC-17; makes free-text assessment meaningless |
| D4 | Answers are append-only per question, and the final score sums all rows | duplicate answers inflate the score (critical) |
| D5 | `TestAttempt` has no started/completed timestamps; no `ABANDONED`; no server timer | AC-18 fails; attempts never expire |
| D6 | `scheduledAt` is never enforced | tests open early via API |
| D7 | `duration_minutes` is only enforced by the browser | trivially bypassed |
| D8 | `Question` uses `question_options` table instead of `extra` JSON | violates `REF p15` / `AG §7` |
| D9 | `score`/`marksAwarded`/`progressPercent` use `Integer` | precision drift against `BigDecimal` contract |
| D10 | No attempt limit / enrolment check before start | a student can attempt any test id |
| D11 | `Test` lacks `created_at` | reference field missing |
| D12 | Three assessment surfaces with different guards | rules diverge; audit surface grows |
| D13 | ExamGuard is only a stored string | the advertised integrity feature is unimplemented |

## 8. Gaps — what the sources do NOT provide

- **NO RULE** for how many attempts a student may take (`maxAttempts` is explicitly not a field).
- **NO CONTRACT** for partial credit on MCQ (all-or-nothing is implied by the reference's grading note, but not stated).
- **NO API** for an instructor to grade TEXT answers on the live `/api/assessments` surface.
- **NO SPEC** for `securityPolicy` values or their behaviour.
- **NO SCHEMA** for storing option-level choices; the reference insists on `extra` JSON but does not define its shape.
- **NO REQUIREMENT** for negative marking or shuffling.

## 9. Open questions

| ID | Question | Feeds |
|---|---|---|
| D-Q1 | Is TEXT auto-grading ever allowed? | AC-17 / BR-12 |
| D-Q2 | Do we keep the legacy `/api/tests` surface or retire it in favour of `/api/assessments`? | PLAN Wave 4 |
| D-Q3 | Is `extra` the canonical option store, with `question_options` migrated? | Q-12 → A-12 |
| D-Q4 | Should attempts be one-per-test or unlimited? | W4-07 (policy enforced on start) |
| D-Q5 | Is ExamGuard in scope for this phase? | `spec §8` (later) |
