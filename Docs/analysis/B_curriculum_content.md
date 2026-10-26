# B — Analysis: curriculum, content & assignments

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** `spec §3.2`, `spec §5` rows 5–8, `spec §9` Q-03/Q-04/Q-05/Q-12 · `AG §3–4` · `REF p7–10` · backend `Entity/{Course,Module,CourseContent,Assignment,AssignmentSubmission,Lecture}`, `Service/impl/{Course,Module,CourseContent,Assignment}ServiceImpl`, `Controller/{Course,Module,Content,Assignment,YouTube,Upload}Controller` · Angular `features/teacher/courses`, `course-management`, `upload`, `features/student/courses`, `core/services/assignment.service.ts`
**Date of analysis:** 2026-10-22

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n |
| `AG §n` | root `AGENTS.md` section n |
| `SYS §n` / `BE` / `FE` / `ARCH` | `Docs/learnsphere_system_design.md` · `Docs/Backend.md` · `Docs/Frontend.md` · `Docs/Architecture.md` |
| `code <path>:<line>` | implementation evidence |
| `derived` | analyst inference, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| `REF p7–10` | Read in full: Course, Module, CourseContent, Assignment. |
| `AG §3–4` | Read in full, including the YouTube preservation rule. |
| `spec §3.2, §5, §9` | Read in full. |
| Backend | All six entities; Course/Module/CourseContent/Assignment service implementations; Course, Module, Content, Assignment, YouTube, Upload controllers; `SecurityConfig` rule for these paths. |
| Frontend | `teacher.service.ts` (module/content calls), `assignment.service.ts`, `student.service.ts` (module/content/lecture calls), `course-detail.component.ts`, route table. |
| Does NOT exist | no soft-delete column, no `course_content` ↔ `lecture` link, no content ordering UI contract, no assignment file-upload endpoint. |

---

## 1. The reference contract

1. **Course** — `course_id`, `title` not null, `description`, `image_url`, `department_id` FK, `creator_id` FK, `is_published` default false, `created_at`. Only Instructor/Admin create or update. A course cannot be enrolled unless published. Soft-delete/archive to preserve enrolment history (`REF p7`; `AG §3`).
2. **Module** — `module_id`, `course_id` not null, `title`, `position`, `created_at`; ordered by `position`; reorder updates many rows; positions re-normalised on delete (`REF p8`; `AG §3`).
3. **CourseContent** — `content_id`, `module_id` not null, `content_type ∈ {VIDEO, PDF, QUIZ}`, `title`, `file_path`, `duration_seconds`, `position`, `uploaded_at`; ordered by position (`REF p9`; `AG §4`).
4. **Assignment** — `assignment_id`, `module_id` not null, `title` not null, `description`, `max_marks > 0`, `due_date` future on creation, `created_at`; one submission per student (`REF p10`; `AG §4`).
5. **AssignmentSubmission** — unique `(student_id, assignment_id)`, at least one of `submission_text`/`file_url`, marks null until graded (`REF p18`; `AG §8`).

---

## 2. There are two parallel curriculum models

6. **`Lecture`** is the original playback unit: `lectures` table, FK `course_id`, fields `title`, `number`, `videoUrl`, `duration`, `isDownloadable`, `status ∈ {DRAFT, PUBLISHED}` (`code Entity/Lecture.java`). It is not in the reference's 10 domains but is required by the preserved YouTube/player features (`AG §4`).
7. **`Module → CourseContent`** is the reference-conformant curriculum (`code Entity/Module.java`, `CourseContent.java`). It exists, is exposed by `ModuleController`/`ContentController`, and the Angular teacher service writes to it (`teacher.service.ts:69–90`).
8. The student course-detail page loads **both**: `lectures` and `modules` (`code course-detail.component.ts:38–40`), and the lecture player consumes `Lecture` (`student.service.ts:65–90`). **derived:** content added to a `Module` is not automatically playable; there is no link between a `CourseContent` row and a `Lecture` row. A teacher can create curriculum that the student player never sees.
9. There is no endpoint that returns a module's content as a playable lecture, and `CourseContent.filePath` is a free string. **derived:** the dual model is the single biggest structural risk in this area (F3, A-05).

## 3. Course mapping and publication

10. `Course` is mapped to `courses` with PK `id`, not `course_id` (`code Entity/Course.java:14–18`). It exposes compatibility accessors: `getCourseId()`, `getCreator()`/`setCreator()` over `teacher`, `getImageUrl()` over `thumbnail`, and `getIsPublished()` derived from `status == LIVE` (`Course.java:20–64`).
11. `department` is a plain `String`, not the reference's `department_id` FK (`Course.java:38`). **derived:** there is no referential integrity between a course and a department, and the department filter is a string match.
12. Publication is `Status ∈ {DRAFT, PENDING, LIVE, ARCHIVED}` (`Course.java:67–71`), with `DRAFT` default. `is_published` is a derived boolean, not a column (`spec §9` Q-04).
13. `CourseController` maps create/update/delete to both `/api/courses…` and `/api/teacher/courses…` (`Controller/CourseController.java`). Create sets `teacher = principal.user` and forces `DRAFT` (`CourseServiceImpl.createCourse`), but **does not check that the principal is a teacher/admin**. Update/delete call `assertOwner` (admin bypass).
14. Enrollment does **not** check `status == LIVE`; `EnrollmentServiceImpl.enrollUser` only sets `enrolledAt` and saves (`code Service/impl/EnrollmentServiceImpl.java:24–27`). AC-10 is unimplemented.
15. Delete is a **hard delete** with a hand-written JDBC cascade across notes, bookmarks, discussions, messages, tests, answers, payments, certificates, royalties, live sessions and enrollments (`CourseServiceImpl.deleteCourse`). `ARCHIVED` exists but nothing archives. The reference's soft-delete rule is not implemented.

## 4. Modules and content

16. `Module` matches the reference closely (`module_id`, `course_id` not null, `title`, `position`, `created_at`) and is mapped to `module` (`Entity/Module.java`).
17. Create assigns `position = existing.size() + 1` when absent; delete re-normalises remaining modules to `1..n` (`ModuleServiceImpl.createModule`, `deleteModule`). AC-08 is implemented.
18. `CourseContent` matches the reference's fields and validates `contentType ∈ {VIDEO, PDF, QUIZ}` (`CourseContentServiceImpl.addContentToModule`). Create assigns `position = existing.size() + 1`.
19. Content delete does **not** re-normalise positions (`CourseContentServiceImpl.deleteContent` only deletes). **derived:** content ordering can develop gaps; the module rule was copied but not the content rule.
20. `ModuleController` and `ContentController` have **no ownership or role check** — see A §2.6. Any authenticated user can add/update/delete curriculum (`Controller/ModuleController.java`, `Controller/ContentController.java`).
21. There is no content-reorder endpoint; order is only set through `PUT /api/modules/{id}` and `PUT /api/contents/{id}`. The reference expects reordering across records; the frontend has no drag-and-drop contract for content.

## 5. Assignments

22. `Assignment` matches the reference: `assignment_id`, `module_id` not null, `title` not null, `description`, `max_marks` default 100, `due_date`, `created_at` (`Entity/Assignment.java`). Create validates non-blank title, `maxMarks > 0`, and a future `dueDate` (`AssignmentServiceImpl.createAssignment`). AC-09 is implemented on create.
23. Update validates `maxMarks` only when supplied and > 0, and does **not** validate that a new `dueDate` is in the future (`AssignmentServiceImpl.updateAssignment`). **derived:** a past date can be written after creation; the rule is create-only.
24. Submission requires at least one of `submissionText`/`fileUrl`, rejects blank, and upserts the existing row for the student (`AssignmentServiceImpl.submitAssignment`). The entity declares a unique constraint on `(student_id, assignment_id)` (`Entity/AssignmentSubmission.java`). The `AG §8` submission rule is implemented; AC-22 covers thread/post/review editing, not submissions.
25. Grading rejects marks outside `0..maxMarks` and stores feedback (`AssignmentServiceImpl.gradeSubmission`). There is no `gradedAt` (the reference note suggests adding one).
26. `GET /api/modules/{id}/assignments` and `GET /api/courses/{id}/assignments` exist, but the frontend also calls `GET /api/courses/{id}/assignments` from the course detail (`assignment.service.ts:20–22`), which the backend exposes via `AssignmentController.getAssignmentsByCourse` (`:22–25`). This is a real, wired flow.

## 6. Ingestion and uploads

27. YouTube playlist import is implemented in `YouTubeController` (`POST /api/teacher/youtube/import-playlist`, `GET /api/teacher/youtube/transcript/{videoId}`) and `YouTubeService`; the teacher upload UI also builds quizzes (`teacher/upload/components/quiz-builder.component.ts`). This is the preserved YouTube capability (`AG §4`).
28. File upload is local: `POST /api/teacher/upload/video` and `/resource` with a 200 MB multipart limit (`Controller/UploadController.java`; `application.properties:20–21`). Files land under `app.upload.dir=uploads`, served as `/api/uploads/**` (permitAll). The reference wants cloud storage keys, not filesystem paths (`REF p9` note).
29. `AI extract-questions` is called from the upload flow and writes questions via the assessment services (`Controller/AiController.java`, `NvidiaNimService`). **derived:** AI-generated options are persisted in `question_options` (see D), not `extra`, which conflicts with the reference (`spec §9` Q-12).

## 7. Frontend wiring

30. Teacher course management calls modules and contents (`teacher.service.ts:69–90`), and the student course detail calls `GET /courses/{id}/modules` and `GET /modules/{id}/contents` (`student.service.ts:51–61`). So the reference curriculum is visible in the UI even though playback uses `Lecture`.
31. The assignment service covers create/update/delete, submissions, my submissions, and grading (`core/services/assignment.service.ts`). The course detail fetches assignments for the course.
32. The player's progress and completion are **lecture-based** (`StudentController` + `UserProgress.lecture`), not module/content-based (`spec §9` Q-05). **derived:** progress reported on the reference entities and progress reported on the player are two different data sets.

---

## 8. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|
| B1 | `Lecture` and `Module→CourseContent` are parallel, unlinked curriculum models | content authored in the new model may be invisible to the player; needs a bridge or a decision (A-05) |
| B2 | `Course.department` is a string; `creator`/`imageUrl`/`isPublished` are accessor aliases over different columns | department integrity and API contracts are soft; clients see aliases, DB sees others |
| B3 | Course publication is `status`, not `is_published` | AC-10 can be derived, but enrollment does not enforce it |
| B4 | Enrollment does not check publication | unpublished courses can be enrolled via API |
| B5 | Course delete is a hard, hand-written cascade | violates the archive/soft-delete rule; high regression risk |
| B6 | Content delete does not re-normalise positions | ordering gaps; UI may show 1,3,4 |
| B7 | Module/content/assignment writes have no role or ownership checks | students can author curriculum (see A) |
| B8 | Assignment update does not validate the new `dueDate` | a past deadline can be written |
| B9 | Uploads are local disk paths under `/uploads` | not portable, not cloud-ready; reference expects storage keys |
| B10 | AI-generated options use `question_options`, not `extra` | conflicts with the reference's no-options-table rule (A-12) |

## 9. Gaps — what the sources do NOT provide

- **NO LINK** between `course_content` and `lecture`; no contract for which one the player should read.
- **NO COLUMN** for `is_published` on the running `courses` table; only `status`.
- **NO FK** `courses.department_id → departments`; only a string.
- **NO REQUIREMENT** for content reordering UX or an endpoint; only "ordered by position".
- **NO CONTRACT** for assignment attachments: `file_url` is a plain string and there is no upload endpoint for submissions.
- **NO POLICY** for what happens to enrollments, progress and certificates when a course is hard-deleted (the manual cascade is an implementation choice, not a source rule).

## 10. Open questions

| ID | Question | Feeds |
|---|---|---|
| B-Q1 | Which curriculum model is the source of truth for playback — `Lecture` or `CourseContent`? | Q-05 → A-05 |
| B-Q2 | Do we add `department_id` FK now or keep the string? | Q-03 → A-03 |
| B-Q3 | Is `status` the publication contract, with `is_published` derived? | Q-04 → A-04 |
| B-Q4 | Should course delete be replaced by archive/soft-delete before any further feature work? | PLAN F, Wave 2 |
| B-Q5 | Where do assignment submission files live? | Wave 2/Hardening |
