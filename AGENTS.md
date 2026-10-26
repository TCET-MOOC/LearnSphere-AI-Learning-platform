# LearnSphere AI — Developer & Agent Guide (AGENTS.md)

> **MANDATORY INSTRUCTION FOR ALL AI AGENTS & DEVELOPERS:**  
> The organizational reference document [`LMS_Entity_Reference.pdf`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/LMS_Entity_Reference.pdf) is the **authoritative architectural specification and single source of truth** for database schemas, entity naming, relationships, business rules, and API endpoint contracts for this project.
> All new feature development, API implementations, database migrations, and frontend refactors **MUST** conform to the standards outlined below.

---

## 1. Architectural Philosophy & System Overview

LearnSphere AI is an enterprise-grade learning management platform comprising:
- **Backend**: Spring Boot 3.3.5 (Java 17/21, Spring Data JPA, PostgreSQL)
- **Frontend**: Angular 19+ (Standalone Components, RxJS, Modern Glassmorphic UI)
- **Payments Microservice**: Spring Boot service handling Razorpay checkout and faculty royalties
- **AI Tutoring**: NVIDIA NIM LLM integration for automated quiz generation and transcript summarization

All backend models follow **strict 3-tier curriculum decomposition**:
$$\text{Course} \longrightarrow \text{Module} \longrightarrow \text{CourseContent / Assignment}$$

---

## 2. Default Domain Specifications (Sections 1 – 10)

### Section 1: Department & Role
* **Department** (`department`):
  * Fields: `department_id` (`Integer` PK, auto-increment), `name` (`VARCHAR`, UNIQUE, NOT NULL), `created_at` (`LocalDateTime`, `@PrePersist`, not updatable).
  * Business Rules: `departmentId` is `Integer` (not `Long`). Name must be unique. Read-heavy entity; cache lookups where appropriate.
  * Endpoints:
    * `GET /api/departments` — List all departments
    * `POST /api/departments` — Create department (Admin only)
    * `GET /api/departments/:id` — Get department by ID
    * `PUT /api/departments/:id` — Update department name
    * `DELETE /api/departments/:id` — Delete department (Admin only)
    * `GET /api/departments/:id/courses` — Get all courses in a department
* **Role** (`role`):
  * Fields: `role_id` (`Integer` PK, auto-increment), `name` (`VARCHAR`, UNIQUE, NOT NULL: `ADMIN`, `INSTRUCTOR`, `STUDENT`).
  * Business Rules: Initial roles are seeded at startup (`DataInitializer`), not created by users.
  * Endpoints:
    * `GET /api/roles` (Admin only)
    * `POST /api/roles` (Admin only)
    * `DELETE /api/roles/:id` (Admin only)

---

### Section 2: User Account & User Role
* **UserAccount** (`user_account`):
  * Fields: `user_id` (`Long` PK), `full_name` (`VARCHAR`), `email` (`VARCHAR`, UNIQUE, NOT NULL), `password` (Bcrypt, `@JsonIgnore`), `dept_id` (`FK` to `department`), `is_active` (`Boolean`, default `true`), `created_at` (`LocalDateTime`), `last_login` (`LocalDateTime`).
  * Business Rules: Deactivated accounts (`isActive=false`) rejected at login. `lastLogin` updated on successful login. Password never exposed in responses.
* **UserRole** (`user_role`):
  * Fields: `user_role_id` (`Long` PK), `user_id` (`FK` to `user_account`), `role_id` (`FK` to `role`), `assigned_at` (`LocalDateTime`).
  * Constraints: Unique constraint on `(user_id, role_id)`.
* **Endpoints**:
  * `POST /api/auth/register` — Register a new user
  * `POST /api/auth/login` — Login, returns JWT access token + refresh token
  * `POST /api/auth/refresh` — Refresh access token
  * `POST /api/auth/logout` — Invalidate refresh token
  * `GET /api/users/me` — Get own profile
  * `PUT /api/users/me` — Update own profile
  * `GET /api/users/:id` — Admin: get any user profile
  * `PATCH /api/users/:id/status` — Admin: activate/deactivate account
  * `GET /api/users/:id/roles` — List roles for a user
  * `POST /api/users/:id/roles` — Assign role (Admin only)
  * `DELETE /api/users/:id/roles/:roleId` — Remove role (Admin only)

---

### Section 3: Course & Module
* **Course** (`course`):
  * Fields: `course_id` (`Long` PK), `title` (`VARCHAR`, NOT NULL), `description` (`TEXT`), `image_url` (`TEXT`), `department_id` (`FK` to `department`), `creator_id` (`FK` to `user_account`), `is_published` (`Boolean`, default `false`), `created_at` (`LocalDateTime`).
  * Business Rules: Only Instructor/Admin roles create or update courses. A course cannot be enrolled in unless `is_published = true`. Soft-delete strategy to preserve enrollment history.
* **Module** (`module`):
  * Fields: `module_id` (`Long` PK), `course_id` (`FK` to `course`, NOT NULL), `title` (`VARCHAR`), `position` (`Integer`), `created_at` (`LocalDateTime`).
  * Business Rules: Ordered by `position`. Reorder operations update positions across records. Renormalize positions on deletion to eliminate gaps.
* **Endpoints**:
  * `GET /api/courses` — List all published courses
  * `POST /api/courses` — Create new course
  * `GET /api/courses/:id` — Get course by ID
  * `PUT /api/courses/:id` — Update course
  * `DELETE /api/courses/:id` — Delete/archive course
  * `GET /api/courses/:id/modules` — Get all modules in a course
  * `GET /api/modules/:id` — Get module by ID
  * `POST /api/courses/:id/modules` — Create module under course
  * `PUT /api/modules/:id` — Update module
  * `DELETE /api/modules/:id` — Delete module
  * `GET /api/modules/:id/contents` — Get all content items in module

---

### Section 4: Course Content & Assignment
* **CourseContent** (`course_content`):
  * Fields: `content_id` (`Long` PK), `module_id` (`FK` to `module`, NOT NULL), `content_type` (`VARCHAR`: `VIDEO`, `PDF`, `QUIZ`), `title` (`VARCHAR`), `file_path` (`TEXT` — cloud storage / S3 key), `duration_seconds` (`Integer`), `position` (`Integer`), `uploaded_at` (`LocalDateTime`).
  * Business Rules: Ordered by `position`. `durationSeconds` enables video resume and progress bar.
* **Assignment** (`assignment`):
  * Fields: `assignment_id` (`Long` PK), `module_id` (`FK` to `module`, NOT NULL), `title` (`VARCHAR`, NOT NULL), `description` (`TEXT`), `max_marks` (`Integer` > 0), `due_date` (`LocalDateTime` in UTC), `created_at` (`LocalDateTime`).
  * Business Rules: Belongs to `Module`. `dueDate` validated as a future date on creation.
* **Endpoints**:
  * `GET /api/contents/:id` — Get content item
  * `POST /api/modules/:id/contents` — Add content to a module
  * `PUT /api/contents/:id` — Update content metadata
  * `DELETE /api/contents/:id` — Delete content item
  * `GET /api/assignments/:id` — Get assignment details
  * `POST /api/modules/:id/assignments` — Create assignment under module
  * `PUT /api/assignments/:id` — Update assignment
  * `DELETE /api/assignments/:id` — Delete assignment
  * `GET /api/assignments/:id/submissions` — List submissions for assignment

---

### Section 5: Enrollment & User Progress
* **Enrollment** (`enrollment`):
  * Fields: `enrollment_id` (`Long` PK), `user_id` (`FK` to `user_account`, NOT NULL), `course_id` (`FK` to `course`, NOT NULL), `role` (`VARCHAR`: `STUDENT` or `INSTRUCTOR`), `enrolled_at` (`LocalDateTime`).
  * Constraints: Unique constraint on `(user_id, course_id, role)`.
* **UserProgress** (`user_progress`):
  * Fields: `progress_id` (`Long` PK), `user_id` (`FK` to `user_account`, NOT NULL), `content_id` (`FK` to `course_content`, NOT NULL), `completed_at` (`LocalDateTime`, nullable), `progress_percent` (`BigDecimal(5,2)`), `seconds_watched` (`Integer`).
  * Constraints: Unique constraint on `(user_id, content_id)`.
  * Business Rules: Upsert semantics (update if existing, else insert). `completedAt` set when `progressPercent >= 90%` (or `100%`). Overall course completion is computed dynamically on the fly.
* **Endpoints**:
  * `POST /api/enrollments` — Enroll in a course
  * `GET /api/enrollments/me` — Get my enrollments
  * `DELETE /api/enrollments/:id` — Unenroll from a course
  * `GET /api/courses/:id/enrollments` — Admin: all enrollments for a course
  * `POST /api/progress` — Upsert progress for content item
  * `GET /api/progress/me` — Get all my progress records
  * `GET /api/progress/content/:id` — My progress on specific content
  * `GET /api/progress/course/:id/summary` — Admin: progress summary across all students

---

### Section 6: Test & Test Attempt
* **Test** (`test`):
  * Fields: `test_id` (`Long` PK), `course_id` (`FK` to `course`, NOT NULL), `title` (`VARCHAR`), `duration_minutes` (`Integer`), `security_policy` (`VARCHAR` for ExamGuard), `scheduled_at` (`LocalDateTime`, nullable), `created_at` (`LocalDateTime`).
  * Business Rules: If `scheduledAt` set, test is not accessible beforehand. `durationMinutes` enforced server-side.
* **TestAttempt** (`test_attempt`):
  * Fields: `attempt_id` (`Long` PK), `user_id` (`FK` to `user_account`, NOT NULL), `test_id` (`FK` to `test`, NOT NULL), `started_at` (`LocalDateTime`), `completed_at` (`LocalDateTime`), `score` (`BigDecimal(8,2)`), `status` (`VARCHAR`: `IN_PROGRESS`, `COMPLETED`, `ABANDONED`).
  * Business Rules: Transitions: `IN_PROGRESS` ➔ `COMPLETED` on submit, or `IN_PROGRESS` ➔ `ABANDONED` on timeout (`startedAt + durationMinutes`). Scores calculated server-side from student answers.
* **Endpoints**:
  * `GET /api/tests/:id` — Get test details
  * `POST /api/courses/:id/tests` — Create test under course
  * `PUT /api/tests/:id` — Update test
  * `DELETE /api/tests/:id` — Delete test
  * `GET /api/tests/:id/questions` — Get all questions in test
  * `GET /api/courses/:id/tests` — List all tests in course
  * `POST /api/tests/:id/attempts` — Start a new attempt
  * `GET /api/attempts/:id` — Get attempt details + answers
  * `POST /api/attempts/:id/submit` — Submit/complete attempt
  * `GET /api/tests/:id/attempts/me` — Get my attempts for a test
  * `GET /api/tests/:id/attempts` — Admin: all attempts for a test

---

### Section 7: Question & Student Answer
* **Question** (`question`):
  * Fields: `question_id` (`Long` PK), `test_id` (`FK` to `test`, NOT NULL), `body` (`TEXT`), `question_type` (`VARCHAR`: `MCQ`, `TEXT`, `TF`), `marks` (`Integer` > 0), `correct_answer` (`TEXT`), `extra` (`TEXT` JSON column storing options array, hints, etc.).
  * Business Rules: **No separate QuestionOption table**. For MCQ, `extra` contains JSON array; `correctAnswer` must match one option. Never expose `correctAnswer` to client pre-submission.
* **StudentAnswer** (`student_answer`):
  * Fields: `student_answer_id` (`Long` PK), `attempt_id` (`FK` to `test_attempt`, NOT NULL), `question_id` (`FK` to `question`, NOT NULL), `answer_text` (`TEXT`), `marks_awarded` (`BigDecimal(8,2)`).
  * Business Rules: Auto-graded server-side on submission for MCQ/TF; TEXT questions remain null until instructor manual grading.
* **Endpoints**:
  * `GET /api/questions/:id` — Get question (correctAnswer hidden for students)
  * `POST /api/tests/:id/questions` — Add question to test
  * `PUT /api/questions/:id` — Update question
  * `DELETE /api/questions/:id` — Delete question
  * `GET /api/tests/:id/questions` — Get all questions in test
  * `POST /api/attempts/:id/answers` — Submit answers for attempt
  * `GET /api/attempts/:id/answers` — Get all answers (instructor/admin)
  * `PATCH /api/answers/:id/grade` — Instructor manual grading

---

### Section 8: Leaderboard & Assignment Submission
* **Leaderboard** (`leaderboard`):
  * Fields: `leaderboard_id` (`Long` PK), `user_id` (`FK` to `user_account`, NOT NULL), `course_id` (`FK` to `course`, NOT NULL), `avg_score` (`BigDecimal(6,2)`), `global_rank` (`Integer`), `dept_rank` (`Integer`), `updated_at` (`LocalDateTime`).
  * Constraints: Unique constraint on `(user_id, course_id)`.
  * Business Rules: Recalculated asynchronously via event/job after `TestAttempt` completion.
* **AssignmentSubmission** (`assignment_submission`):
  * Fields: `submission_id` (`Long` PK), `assignment_id` (`FK` to `assignment`, NOT NULL), `student_id` (`FK` to `user_account`, NOT NULL), `submission_text` (`TEXT`), `file_url` (`TEXT`), `marks_awarded` (`Integer`), `feedback` (`TEXT`), `submitted_at` (`LocalDateTime`).
  * Constraints: Unique constraint on `(student_id, assignment_id)`. At least one of `submissionText` or `fileUrl` must be non-null.
* **Endpoints**:
  * `GET /api/courses/:id/leaderboard` — Get ranked leaderboard for a course
  * `GET /api/leaderboard/me` — Get my leaderboard entries across courses
  * `POST /api/assignments/:id/submissions` — Submit assignment
  * `GET /api/submissions/:id` — Get submission details
  * `GET /api/assignments/:id/submissions` — Instructor: list all submissions
  * `PATCH /api/submissions/:id/grade` — Instructor: set marksAwarded + feedback
  * `GET /api/submissions/me` — Student: get all my submissions

---

### Section 9: Feedback & Progress Detail
* **Feedback** (`feedback`):
  * Fields: `feedback_id` (`Long` PK), `user_id` (`FK` to `user_account`, NOT NULL), `course_id` (`FK` to `course`, NOT NULL), `rating` (`Short` 1–5), `message` (`TEXT`, optional), `created_at` (`LocalDateTime`).
  * Constraints: Unique constraint on `(user_id, course_id)`.
  * Business Rules: Average rating computed via repository query (`@Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.course.courseId = :courseId")`).
* **Endpoints**:
  * `POST /api/courses/:id/feedback` — Submit feedback for course
  * `GET /api/courses/:id/feedback` — Get all feedback for course
  * `GET /api/feedback/:id` — Get specific feedback entry
  * `DELETE /api/feedback/:id` — Delete feedback (admin/author only)
  * `GET /api/courses/:id/feedback/avg` — Get average rating for course

---

### Section 10: Discussion Thread & Post
* **DiscussionThread** (`discussion_thread`):
  * Fields: `thread_id` (`Long` PK), `course_id` (`FK` to `course`), `user_id` (`FK` to `user_account`), `title` (`VARCHAR`, NOT NULL), `content` (`TEXT`, NOT NULL), `created_at` (`LocalDateTime`), `updated_at` (`LocalDateTime`).
* **Post** (`post`):
  * Fields: `post_id` (`Long` PK), `thread_id` (`FK` to `discussion_thread`, NOT NULL), `user_id` (`FK` to `user_account`, NOT NULL), `body` (`TEXT`, NOT NULL), `parent_post_id` (`FK` to `post`, nullable for nested replies), `created_at` (`LocalDateTime`).
  * Business Rules: Thread deletion cascade-deletes or soft-deletes its posts. Replies sorted by `createdAt ASC` and nested under `parentPost`.
* **Endpoints**:
  * `GET /api/threads/:id` — Get thread + posts
  * `POST /api/courses/:id/threads` — Create thread in course
  * `PUT /api/threads/:id` — Update thread (creator/admin)
  * `DELETE /api/threads/:id` — Delete thread + its posts
  * `GET /api/courses/:id/threads` — List all threads in course
  * `POST /api/threads/:id/posts` — Create post in thread
  * `PUT /api/posts/:id` — Edit post (author/admin)
  * `DELETE /api/posts/:id` — Delete post
  * `GET /api/threads/:id/posts` — Get all posts in thread

---

## 3. JWT & Security Standards

* **Access Token**:
  * Validity: 15 minutes.
  * Claims: `userId`, `roles`, `dept_id`.
  * Client Storage: In-memory only in Angular (`AuthService`). **Never store access tokens in `localStorage`**.
* **Refresh Token**:
  * Validity: 7 days.
  * Storage: Stored in HttpOnly cookie or database session table.
  * Endpoint: `POST /api/auth/refresh` silently rotates access tokens.

---

## 4. Preservation of Non-PDF Features

When building or refactoring, do **NOT** delete the following capabilities; integrate them with the new entities:
1. **YouTube Ingestion**: Import playlists into `Module` and populate videos as `CourseContent` (`contentType = 'VIDEO'`).
2. **NVIDIA NIM AI Quizzes**: Output generated questions into `Question` with options in the `extra` JSON field.
3. **Razorpay Payments**: Course purchases link to `course_id` and `user_id`.
4. **College Affiliations**: College records remain attached to `user_account` and `department`.
5. **Live Sessions**: Attached to `course_id` and `user_account` (instructor).
6. **Notes & Bookmarks**: Attached to `content_id` (`CourseContent`).
