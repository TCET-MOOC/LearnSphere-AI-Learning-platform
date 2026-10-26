# LearnSphere AI — API catalogue

Companion to [`../PLAN.md`](../PLAN.md) §6.2 (API conventions), [`waves.md`](waves.md) and [`schema-usage.md`](schema-usage.md). Generated from the controller annotations on 2026-10-22, reconciled with the target design and the 2026-10-24 [`audit.md`](../../audit.md).

**Conventions**

- Main service base path `/api`; internal service path `/internal`; payments microservice host `http://localhost:8081` with API root `/api/payments`.
- JSON camelCase; ISO-8601 UTC timestamps; errors `application/problem+json` with a stable `code` (ADR-10).
- List endpoints use `?page=0&size=20&sort=field,asc` and `{content, page, size, totalElements, totalPages}` after the wave that touches them.
- Money, answer and publish writes are idempotent by natural key: `gatewayPaymentId` for payments, `(attemptId, questionId)` for answers, `(userId, contentId)` for progress, `(contentId, idempotencyKey)` for YouTube publish jobs.
- Cache headers are part of the contract (NFR-03): published catalogue/reference reads return `ETag` + `Cache-Control: public, max-age=60, stale-while-revalidate=30`; auth, profile, progress, attempts/answers, uploaded-file downloads, media/live playback descriptors, signed URLs, YouTube connection/publish jobs, payments/receipts/royalties, admin lists and per-user reads return `Cache-Control: private, no-store`. A write that changes a cached public read invalidates it after commit; tests cover hit, 304 and invalidation (W2-11/W2-12/W2-13/W3-13/W4-11/W5-10).
- Foreign ids return **404**, not 403, once ownership enforcement lands (W1-08).
- `A-xx` = assumption in `PLAN.md` §3; `BR-xx` = business rule in §6.6; `AC-xx` = acceptance criterion in `spec.md`; `AUD-xx` = audit remediation item in `Docs/PLAN.md` §11.
- No endpoint is implemented without a failing test first (unit + integration for write paths); the `Target` value includes the test gate.

**Error contract (P0-09)**

- Every error is `application/problem+json` with `type`, `title`, `status`, `detail`, `instance`, stable `code` and `traceId`. The same `traceId` is returned in the `X-Trace-Id` response header and in the JSON logs.
- `errors` is present for `VALIDATION_FAILED` as `[{ "field": "...", "message": "..." }]`, one message per field, sorted by field. Clients branch on `code`, never on `detail`.
- The stable codes are: `VALIDATION_FAILED` (400), `MALFORMED_REQUEST` (400), `INVALID_PARAMETER` (400), `BAD_REQUEST` (400), `INVALID_CREDENTIALS` (401), `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `METHOD_NOT_ALLOWED` (405), `CONFLICT` (409), `PAYLOAD_TOO_LARGE` (413), `UNSUPPORTED_MEDIA_TYPE` (415), `INTERNAL_ERROR` (500). Unknown exceptions return `INTERNAL_ERROR` with a generic detail; messages and stack traces are never leaked.
- The security filter chain uses the same body for pre-controller `401`/`403` failures (`ApiSecurityErrorHandler`), so an unauthenticated protected request is `401 UNAUTHORIZED`, not an empty `403`.
- The payments service keeps its legacy error bodies until W5 hardens those endpoints. W1+ endpoints are in the backend and use this contract from the first commit.

```json
{
  "type": "https://learnsphere.ai/problems/validation-failed",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/auth/login",
  "code": "VALIDATION_FAILED",
  "traceId": "8a4f7f6e-6b1a-4f2e-9a0e-2b6f0f7c1d23",
  "errors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

**Pagination envelope**

- New list endpoints accept `?page=0&size=20&sort=field,asc` (zero-based; default size 20, maximum 100) and return `{content, page, size, totalElements, totalPages}` via `Dto/PageResponse`.
- Existing list endpoints keep their legacy array shape until the wave that owns them migrates them.

**OpenAPI**

- Local/demo/test expose `GET /v3/api-docs` and `/swagger-ui.html`; `prod` disables both (`springdoc.*.enabled=false`).
- `OpenApiDocumentTests` generates `target/openapi.json` during the normal backend suite. CI fails if the file is missing and uploads it as the `openapi-document` artifact.
- The document includes the shared `Problem` and `PageResponse` schemas and the `bearerAuth` security scheme, even before the first W1 endpoint references them.

**Permission codes**

| Code | Meaning |
|---|---|
| `public` | No authentication |
| `auth` | Any authenticated user |
| `self` | Authenticated; own resource only |
| `student` | Student role |
| `enrolled` | Enrolled student, course staff, or admin |
| `staff` | Course teacher/owner or admin |
| `teacher` | Teacher role (admin override) |
| `admin` | Admin role |
| `internal` | Service-to-service key + service identity |

**Target codes**

| Code | Meaning |
|---|---|
| `Live` | Contract is implemented and may be kept; hardening is still allowed |
| `Harden` | Endpoint exists but must change before the wave gate (ownership, idempotency, DTO, validation) |
| `Retire` | Remove or replace; do not build new clients on it |
| `New` | Planned endpoint, not in the current code |

> Aliases (`/api/courses` and `/api/teacher/courses`, public and teacher YouTube imports, etc.) are listed as separate rows because they are separate mappings. A row is one method + one path.

---

### Identity, colleges and reference data

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| POST | `/api/auth/forgot-password` | public | Harden |  |
| POST | `/api/auth/login` | public | Harden | 24 h token; no refresh |
| POST | `/api/auth/logout` | public | Harden |  |
| GET | `/api/auth/me` | auth | Harden | `private, no-store` (NFR-03) |
| POST | `/api/auth/refresh` | public | Harden | reissues from access token; no rotation |
| POST | `/api/auth/register` | public | Harden | accepts TEACHER/ADMIN + collegeId |
| POST | `/api/auth/reset-password` | public | Harden |  |
| POST | `/api/auth/verify-college` | auth | Harden |  |
| GET | `/api/colleges` | public | Harden | returns PENDING colleges too |
| GET | `/api/departments` | public | Live | Public cache + `ETag`; invalidated on department write (W2-11; NFR-03) |
| POST | `/api/departments` | admin | Harden | Invalidates department/catalogue cache after commit |
| DELETE | `/api/departments/{id}` | admin | Harden | Invalidates department/catalogue cache after commit |
| GET | `/api/departments/{id}` | public | Live | Public cache + `ETag`; invalidated on department write (W2-11) |
| PUT | `/api/departments/{id}` | admin | Harden | Invalidates department/catalogue cache after commit |
| GET | `/api/departments/{id}/courses` | public | Live | Public `LIVE` catalogue only; cache + `ETag`; invalidated on course write (W2-11) |
| GET | `/api/roles` | admin | Harden |  |
| POST | `/api/roles` | admin | Harden |  |
| DELETE | `/api/roles/{id}` | admin | Harden |  |
| GET | `/api/roles/{id}` | admin | Harden |  |
| PUT | `/api/roles/{id}` | admin | Harden |  |

### Users, roles and affiliations

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| GET | `/api/admin/colleges` | admin | Harden |  |
| POST | `/api/admin/colleges` | admin | Harden |  |
| DELETE | `/api/admin/colleges/{collegeId}` | admin | Harden |  |
| PUT | `/api/admin/colleges/{collegeId}/verify` | admin | Harden |  |
| GET | `/api/admin/colleges/affiliations` | admin | Harden |  |
| PUT | `/api/admin/colleges/affiliations/{requestId}/approve` | admin | Harden |  |
| PUT | `/api/admin/colleges/affiliations/{requestId}/reject` | admin | Harden |  |
| GET | `/api/admin/users` | admin | Harden |  |
| PUT | `/api/admin/users/{id}/status` | admin | Harden |  |
| GET | `/api/admin/users/activity-summary` | admin | Harden |  |
| GET | `/api/admin/users/top-teachers` | admin | Harden |  |
| GET | `/api/user-roles` | admin | Harden |  |
| POST | `/api/user-roles` | admin | Harden |  |
| DELETE | `/api/user-roles/{id}` | admin | Harden |  |
| GET | `/api/user-roles/{id}` | admin | Harden |  |
| GET | `/api/users` | admin | Retire | Generic collection; use `/api/admin/users` (audit D6/E1; W1-11) |
| POST | `/api/users` | admin | Retire | Accepts arbitrary entity/password; use admin flow (audit D6; W1-11) |
| DELETE | `/api/users/{id}` | admin | Retire | Generic delete; no hard-delete of accounts (audit D6; W1-11) |
| GET | `/api/users/{id}` | admin | Harden | Admin read alias only after W1-11; no entity/password fields |
| PUT | `/api/users/{id}` | admin | Retire | Generic safe-patch can change e-mail/department; use `/users/me` or admin flow (audit D6; W1-11) |
| GET | `/api/users/{id}/roles` | admin | Harden | Admin-only; audited |
| POST | `/api/users/{id}/roles` | admin | Harden | Admin-only; unique constraint; audited (W1-07/W1-11) |
| DELETE | `/api/users/{id}/roles/{roleId}` | admin | Harden | Admin-only; audited (W1-07/W1-11) |
| PATCH | `/api/users/{id}/status` | admin | Retire | Ambiguous `isActive` mapping; use `/api/admin/users/{id}/status` (audit D8; W1-03/W1-11) |
| GET | `/api/users/me` | self | Harden | Self only; real profile read; `private, no-store` (NFR-03) |
| PUT | `/api/users/me` | self | Harden | Self only; profile save used by all roles (BR-24); invalidates public teacher-name cache if the teacher is visible in a course |

### Curriculum and media

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| PUT | `/api/admin/courses/{id}/status` | admin | Harden |  |
| DELETE | `/api/assignments/{id}` | teacher | Harden |  |
| GET | `/api/assignments/{id}` | enrolled | Live |  |
| PUT | `/api/assignments/{id}` | teacher | Harden |  |
| GET | `/api/assignments/{id}/submissions` | staff | Live |  |
| POST | `/api/assignments/{id}/submissions` | student | Harden |  |
| DELETE | `/api/contents/{id}` | teacher | Harden |  |
| GET | `/api/contents/{contentId}/playback` | entitled | New | VIDEO only; `UPLOAD` → `{kind:'UPLOAD', url, expiresAt}`; `YOUTUBE` → `{kind:'YOUTUBE', videoId, embedUrl}`; `EXTERNAL` → `{kind:'EXTERNAL', url}`; students need published+enrolled, staff may preview own draft; no storage key/path; `private, no-store` (W2-12; BR-26) |
| GET | `/api/contents/{id}` | enrolled | Harden | Metadata only; playback is a separate descriptor; no raw storage path (W2-12) |
| PUT | `/api/contents/{id}` | teacher | Harden | Validate `YOUTUBE`/`UPLOAD`/`EXTERNAL` source descriptor; explicit switch to an already-published YouTube copy or back to local; reject mixed sources (W2-12/W2-13) |
| GET | `/api/courses` | public | Live | Published catalogue only; cache + `ETag`; invalidated on publish/update/archive (W2-11; NFR-03) |
| GET | `/api/courses/trending` | public | Live | Public trending list (`TrendingController`); snapshot cache ≤ 60 s, invalidated on enrolment/rating writes (W8-06) |
| POST | `/api/courses` | teacher | Harden | Invalidates published catalogue cache after commit |
| GET | `/api/courses/{courseId}/modules` | public | Live |  |
| POST | `/api/courses/{courseId}/modules` | teacher | Harden |  |
| DELETE | `/api/courses/{id}` | teacher | Harden |  |
| GET | `/api/courses/{id}` | public | Live | `LIVE` detail only; public cache + `ETag`; draft/staff view bypasses (W2-11; NFR-03) |
| PUT | `/api/courses/{id}` | teacher | Harden | Invalidates course detail/catalogue cache after commit |
| GET | `/api/courses/{id}/assignments` | enrolled | Live |  |
| GET | `/api/courses/{id}/lectures` | enrolled | Harden | Legacy Lecture surface; player metadata/video URL requires server enrolment check (AUD-08; W3-11); migrate to content (A-05) |
| DELETE | `/api/modules/{id}` | teacher | Harden |  |
| GET | `/api/modules/{id}` | enrolled | Live |  |
| PUT | `/api/modules/{id}` | teacher | Harden |  |
| GET | `/api/modules/{id}/assignments` | enrolled | Live |  |
| POST | `/api/modules/{id}/assignments` | teacher | Harden |  |
| GET | `/api/modules/{id}/contents` | enrolled | Live |  |
| POST | `/api/modules/{id}/contents` | teacher | Harden |  |
| GET | `/api/submissions/{id}` | self | Live |  |
| PATCH | `/api/submissions/{id}/grade` | staff | Live |  |
| GET | `/api/submissions/me` | self | Live |  |
| GET | `/api/teacher/courses` | teacher | Live |  |
| POST | `/api/teacher/courses` | teacher | Live |  |
| DELETE | `/api/teacher/courses/{id}` | teacher | Live |  |
| PUT | `/api/teacher/courses/{id}` | teacher | Harden | ownership check missing (B7) |
| POST | `/api/teacher/courses/{id}/bulk-lectures` | teacher | Live |  |
| POST | `/api/teacher/courses/{id}/lectures` | teacher | Harden | legacy Lecture; migrate (A-05) |
| DELETE | `/api/teacher/lectures/{id}` | teacher | Harden | legacy Lecture; migrate (A-05) |
| PUT | `/api/teacher/lectures/{id}` | teacher | Harden | legacy Lecture; migrate (A-05) |
| GET | `/api/files/{key}` | owner/entitled | New | Authorized resource/assignment download; opaque key, no directory listing, Range; `private, no-store` (W2-07; BR-26) |
| POST | `/api/teacher/upload/resource` | teacher | Harden | 201 `{fileKey}`; private storage; no public URL (W2-07; BR-26) |
| POST | `/api/teacher/upload/video` | teacher | Harden | 201 `{mediaKey}`; MP4/WebM MIME + magic-byte check, 200 MB, private storage (W2-12; A-25) |
| GET | `/api/teacher/media/youtube/connection` | teacher | New | Connection status/title only; never tokens (W2-13) |
| DELETE | `/api/teacher/media/youtube/connection` | teacher | New | Revoke local channel connection and token (W2-13) |
| GET | `/api/teacher/media/youtube/callback` | public | New | OAuth redirect; signed single-use state; no access token in response (W2-13) |
| POST | `/api/teacher/media/youtube/connect` | teacher | New | Start OAuth; returns authorization URL; upload scope; audit (W2-13) |
| GET | `/api/teacher/media/youtube/jobs/{jobId}` | teacher | New | Own publish job status; no tokens; `private, no-store` (W2-13) |
| POST | `/api/teacher/contents/{contentId}/youtube-publish` | teacher | New | 202 `{jobId}`; idempotency key; 409 active duplicate; 404 foreign content; local source remains fallback (W2-13) |
| POST | `/api/teacher/youtube/import-playlist` | teacher | Harden | Import only; canonical content, owner, quota (W2-09/W8-04) |
| GET | `/api/teacher/youtube/transcript/{videoId}` | teacher | Live |  |
| POST | `/api/youtube/import-playlist` | auth | Retire |  |
| GET | `/api/youtube/transcript/{videoId}` | auth | Retire |  |

### Enrolment, progress and credentials

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| GET | `/api/certificates` | self | Live |  |
| POST | `/api/certificates/issue` | self | Harden | lecture-based; hard-coded grade |
| GET | `/api/certificates/verify/{code}` | public | Live |  |
| GET | `/api/courses/{courseId}/enrollments` | staff | Harden | no ownership check |
| GET | `/api/courses/{courseId}/leaderboard` | enrolled | Harden | Materialised table; `private, no-store`, no shared cache (W3-07/W3-13) |
| POST | `/api/enrollments` | self | Harden | accepts body userId; no unique key |
| DELETE | `/api/enrollments/{id}` | self | Harden |  |
| GET | `/api/enrollments/me` | self | Harden |  |
| GET | `/api/leaderboard` | auth | Live |  |
| GET | `/api/teacher/courses/{courseId}/standings` | teacher | Harden | `TeacherStudentsController`; scoped to own course |
| POST | `/api/teacher/courses/{courseId}/nudge/{studentId}` | teacher | Harden | `TeacherStudentsController`; notification + audit |
| GET | `/api/leaderboard/me` | auth | Live |  |
| POST | `/api/progress` | self | Harden | trusts body userId; no upsert |
| GET | `/api/progress/content/{contentId}` | self | Harden |  |
| GET | `/api/progress/course/{courseId}/summary` | staff | Harden |  |
| GET | `/api/progress/me` | self | Harden |  |
| GET | `/api/student/courses` | student | Harden |  |
| GET | `/api/student/courses/{courseId}/lectures/{lectureId}` | enrolled | Harden | Player metadata + video URL only after server enrolment check (AUD-08; W3-11) |
| GET | `/api/student/courses/{courseId}/progress` | student | Harden | |
| GET | `/api/student/courses/{id}` | enrolled | Harden | Course player context; no first-course fallback (AUD-08; W3-11) |
| POST | `/api/student/courses/{id}/enroll` | student | Harden |  |
| POST | `/api/student/lectures/{id}/complete` | student | Harden |  |
| GET | `/api/student/lectures/{id}/progress` | student | Harden |  |
| POST | `/api/student/lectures/{id}/uncomplete` | student | Harden |  |
| GET | `/api/student/live-sessions` | student | Harden | List only; no playback descriptor (W5-10) |
| GET | `/api/student/live-sessions/{id}` | student | Harden | Enrolled/course-staff/admin only; `{status,sourceType,videoId or playbackUrl,startedAt}` only while `LIVE`; no stream key; `private, no-store` (W5-10; BR-27) |

### Assessment

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| PATCH | `/api/answers/{id}/grade` | teacher | Harden |  |
| GET | `/api/assessments` | self | Harden |  |
| GET | `/api/assessments/{id}` | self | Harden |  |
| GET | `/api/assessments/content/{contentId}` | self | New | `{testId,title,required,passThreshold,maxAttempts,attemptsUsed,bestScore,passed,questionCount}`; no answers; `private, no-store` (W4-11; BR-25) |
| POST | `/api/assessments/{id}/start` | self | Harden | Enforce content-bound unlock, schedule, attempt limit and enrolment (W4-07/W4-11) |
| GET | `/api/assessments/attempts/{attemptId}` | self | Harden |  |
| POST | `/api/assessments/attempts/{attemptId}/answers` | self | Harden |  |
| POST | `/api/assessments/attempts/{attemptId}/finalize` | self | Harden | Server computes pass; required post-video quiz completes content only when watch ≥90 % and the attempt passes; idempotent (W4-11) |
| GET | `/api/assessments/lecture/{lectureId}` | self | Harden | Legacy alias; W4-11 moves clients to content; do not extend |
| GET | `/api/attempts/{attemptId}` | auth | Retire |  |
| GET | `/api/attempts/{attemptId}/answers` | auth | Retire |  |
| POST | `/api/attempts/{attemptId}/answers` | auth | Retire | append-only; duplicate inflation |
| POST | `/api/attempts/{attemptId}/finalize` | auth | Retire |  |
| POST | `/api/attempts/{attemptId}/submit` | auth | Retire |  |
| GET | `/api/courses/{courseId}/tests` | enrolled | Live |  |
| POST | `/api/courses/{courseId}/tests` | teacher | Harden |  |
| DELETE | `/api/questions/{id}` | auth | Retire | leaks correctAnswer |
| GET | `/api/questions/{id}` | auth | Retire | leaks correctAnswer |
| PUT | `/api/questions/{id}` | auth | Retire | leaks correctAnswer |
| POST | `/api/teacher/tests` | teacher | Harden | Accept `contentId`, `placement`, `requiredForCompletion`, `passThreshold`, `maxAttempts`; validate content ownership (W4-11) |
| DELETE | `/api/teacher/tests/{testId}` | teacher | Harden |  |
| GET | `/api/teacher/tests/{testId}` | teacher | Harden |  |
| PUT | `/api/teacher/tests/{testId}` | teacher | Harden | Update content binding/delivery fields; cannot move to foreign content; audit (W4-11) |
| POST | `/api/teacher/tests/{testId}/questions` | teacher | Harden |  |
| POST | `/api/teacher/tests/{testId}/questions/bulk` | teacher | Harden |  |
| GET | `/api/teacher/tests/content/{contentId}` | teacher | New | Post-video quiz attached to owned content (W4-11) |
| GET | `/api/teacher/tests/course/{courseId}` | teacher | Harden |  |
| GET | `/api/teacher/tests/lecture/{lectureId}` | teacher | Harden | Legacy alias; W4-11 moves clients to content |
| GET | `/api/tests` | auth | Retire |  |
| POST | `/api/tests` | auth | Retire |  |
| DELETE | `/api/tests/{testId}` | auth | Retire |  |
| GET | `/api/tests/{testId}` | auth | Retire |  |
| PUT | `/api/tests/{testId}` | auth | Retire |  |
| GET | `/api/tests/{testId}/attempts` | auth | Retire |  |
| POST | `/api/tests/{testId}/attempts` | auth | Retire |  |
| GET | `/api/tests/{testId}/attempts/me` | auth | Retire |  |
| GET | `/api/tests/{testId}/questions` | auth | Retire | leaks correctAnswer |
| POST | `/api/tests/{testId}/questions` | auth | Retire | leaks correctAnswer |

### Engagement and community

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| GET | `/api/admin/flagged` | admin | Harden |  |
| PUT | `/api/admin/flagged/{id}/dismiss` | admin | Harden |  |
| PUT | `/api/admin/flagged/{id}/resolve` | admin | Harden |  |
| GET | `/api/admin/sentiment` | admin | Harden |  |
| GET | `/api/announcements` | auth | Harden | scoping not enforced |
| POST | `/api/announcements` | teacher | Harden | global today; must be course-scoped |
| DELETE | `/api/announcements/{id}` | teacher | Harden |  |
| PUT | `/api/announcements/{id}` | teacher | Harden |  |
| PUT | `/api/announcements/{id}/pin` | teacher | Harden |  |
| GET | `/api/bookmarks` | self | Harden | rebind to content |
| POST | `/api/bookmarks` | self | Harden | rebind to content |
| DELETE | `/api/bookmarks/{id}` | self | Harden |  |
| DELETE | `/api/bookmarks/lecture/{lectureId}` | self | Harden |  |
| GET | `/api/bookmarks/lecture/{lectureId}` | self | Harden |  |
| POST | `/api/censor` | auth | Live |  |
| GET | `/api/conversations` | self | Live |  |
| POST | `/api/conversations` | self | Live |  |
| GET | `/api/conversations/{id}/messages` | self | Live |  |
| POST | `/api/conversations/{id}/messages` | self | Live |  |
| GET | `/api/courses/{courseId}/discussion` | enrolled | Retire | legacy model; retire (A-13) |
| GET | `/api/courses/{courseId}/feedback` | public | Live |  |
| POST | `/api/courses/{courseId}/feedback` | enrolled | Harden |  |
| GET | `/api/courses/{courseId}/feedback/avg` | public | Live | Public aggregate cache ≤ 60 s; invalidated on feedback write (W5-09; NFR-03) |
| GET | `/api/courses/{courseId}/threads` | enrolled | Harden | canonical model; UI still legacy |
| POST | `/api/courses/{courseId}/threads` | enrolled | Harden | canonical model; UI still legacy |
| POST | `/api/discussion` | enrolled | Retire |  |
| DELETE | `/api/discussion/{id}` | enrolled | Retire |  |
| DELETE | `/api/feedback/{id}` | self | Harden |  |
| GET | `/api/feedback/{id}` | self | Harden |  |
| GET | `/api/lectures/{lectureId}/discussion` | enrolled | Retire |  |
| GET | `/api/notes` | self | Harden | rebind to content |
| POST | `/api/notes` | self | Harden | rebind to content |
| DELETE | `/api/notes/{id}` | self | Harden |  |
| PUT | `/api/notes/{id}` | self | Harden |  |
| GET | `/api/notifications` | self | Live |  |
| PUT | `/api/notifications/{id}/read` | self | Live |  |
| PUT | `/api/notifications/read-all` | self | Live |  |
| GET | `/api/notifications/unread-count` | self | Live |  |
| DELETE | `/api/posts/{id}` | enrolled | Harden |  |
| PUT | `/api/posts/{id}` | enrolled | Harden |  |
| GET | `/api/teacher/live-sessions` | teacher | Live |  |
| POST | `/api/teacher/live-sessions` | teacher | Harden | Accept/validate `sourceType` + HTTPS HLS/YouTube `sourceUrl`; reject RTMP/stream keys; no placeholder join link (W5-10; A-26) |
| PUT | `/api/teacher/live-sessions/{id}/end` | teacher | Harden | Hides playback descriptor; no simulated stream (W5-10) |
| PUT | `/api/teacher/live-sessions/{id}/source` | teacher | New | Set/replace HTTPS HLS manifest or YouTube live id before start; owner-only; reject RTMP/stream key (W5-10) |
| PUT | `/api/teacher/live-sessions/{id}/start` | teacher | Harden | Requires a validated source; returns status, never a stream key (W5-10) |
| DELETE | `/api/threads/{id}` | enrolled | Harden |  |
| GET | `/api/threads/{id}` | enrolled | Harden |  |
| PUT | `/api/threads/{id}` | enrolled | Harden |  |
| GET | `/api/threads/{id}/posts` | enrolled | Harden |  |
| POST | `/api/threads/{id}/posts` | enrolled | Harden |  |

### Commerce, royalties and reports

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| GET | `/api/admin/payouts` | admin | Harden |  |
| POST | `/api/admin/payouts/{teacherId}/pay` | admin | Harden |  |
| POST | `/api/admin/payouts/process-all` | admin | Harden | no lock; DB-only settlement |
| GET | `/api/admin/reports/summary` | admin | Harden |  |
| GET | `/api/admin/revenue` | admin | Harden |  |
| POST | `/api/payments/checkout` | self | Retire | Simulated rail; retire (A-06; AUD-06) |
| GET | `/api/payments/history` | self | Harden | Ledger truth: no fabricated `SUCCESS` rows; retain pending/failed; normalise status and amount unit; `private, no-store` (AUD-02; W6-06; NFR-03) |
| GET | `/api/payments/status` | self | Retire | Simulated-rail status; do not build on it (AUD-06) |
| POST | `/api/payments/verify` | self | Retire | Simulated rail; retire (A-06; AUD-06) |
| GET | `/api/teacher/payouts` | teacher | Harden |  |
| GET | `/api/teacher/royalties` | teacher | Harden | auto-creates royalties on read |
| POST | `/internal/enroll` | internal | Harden | Recompute price; require gateway payment id; idempotent ledger/royalty write (AUD-05; A-19) |

### Artificial intelligence

| Method | Path | Perm | Target | Notes |
|---|---|---|---|---|
| POST | `/api/ai/extract-questions` | teacher | Harden |  |
| GET | `/api/ai/status` | auth | Live |  |
| POST | `/api/ai/summarize-transcript` | teacher | Harden |  |
| POST | `/api/chatbot/ask` | auth | Harden |  |


---

## Planned endpoints

These are the only new endpoints this phase adds. They are ordered by wave.

| Wave | Method | Path | Perm | Target | Notes |
|---|---|---|---|---|---|
| W1-03 | GET | `/api/admin/audit` | admin | New | Filter by actor/resource/date; append-only (§6.10) |
| W2-03 | POST | `/api/courses/{id}/restore` | staff | New | Restore an archived course; admin override; audit |
| W2-04 | PUT | `/api/modules/{id}/reorder` | staff | New | `{position}` or ordered id list; re-normalise 1..n (AC-07/08) |
| W2-07 | GET | `/api/files/{key}` | owner/entitled | New | Authorized upload download; opaque key; no directory listing; `private, no-store` (W2-07; BR-26) |
| W2-12 | GET | `/api/contents/{contentId}/playback` | entitled | New | Signed local URL or YouTube embed; no path/key; `private, no-store` (A-25; BR-26) |
| W2-13 | POST | `/api/teacher/media/youtube/connect` | teacher | New | Start OAuth; signed state; upload scope (W2-13) |
| W2-13 | GET | `/api/teacher/media/youtube/callback` | public | New | OAuth redirect; single-use state; no token response (W2-13) |
| W2-13 | GET | `/api/teacher/media/youtube/connection` | teacher | New | Status only; no token (W2-13) |
| W2-13 | DELETE | `/api/teacher/media/youtube/connection` | teacher | New | Revoke connection/token (W2-13) |
| W2-13 | POST | `/api/teacher/contents/{contentId}/youtube-publish` | teacher | New | 202 `{jobId}`; idempotent unlisted `videos.insert`; 409 active duplicate (W2-13) |
| W2-13 | GET | `/api/teacher/media/youtube/jobs/{jobId}` | teacher | New | Own job status; `private, no-store` (W2-13) |
| W3-10 | GET | `/api/student/courses/{courseId}/resume` | enrolled | New | Next incomplete content for Continue/resume; 404 when complete; no client-side first-lecture fallback; `private, no-store` (AUD-07; BR-23; W3-13) |
| W4-11 | GET | `/api/assessments/content/{contentId}` | self | New | Player quiz summary; no answers; `private, no-store` (A-27; W4-11) |
| W4-11 | GET | `/api/teacher/tests/content/{contentId}` | teacher | New | Attached quiz for owned content (W4-11) |
| W4-11 | PUT | `/api/teacher/tests/{testId}` | teacher | Harden | Update binding/delivery fields; reject foreign content (W4-11) |
| W5-10 | PUT | `/api/teacher/live-sessions/{id}/source` | teacher | New | Set/validate HTTPS HLS/YouTube source; reject RTMP/stream key (W5-10) |
| W5-10 | SUBSCRIBE | `/topic/live-sessions/{sessionId}/status` | enrolled | New | Start/end/source-status event; destination authorized (W5-10; A-16) |
| W6-06 | GET | `/api/admin/ledger/reconciliation` | admin | New | Compare gateway captures vs main ledger; no fabricated rows (A-21) |
| W6-09 | GET | `/api/teacher/payouts/statement` | teacher | New | Own monthly statement + payout history (A-22) |
| W6-11 | POST | `/api/admin/entitlements` | admin | New | Grant/revoke same-college override; audit (A-20) |
| W6-11 | DELETE | `/api/admin/entitlements/{id}` | admin | New | Revoke an override; audit |

---

## Payments microservice

Base host `http://localhost:8081`; API root `/api/payments` (`learnsphere-payments-service`). Until W6-02 it has **no authentication**. Every existing row below is `Harden`/`Retire` except the webhook; `/orders/me` is new. The service must derive the user from auth, never from a path/body id (`audit.md` B11/E2; AUD-04).

| Method | Path | Screen | Perm (target) | Target | Notes |
|---|---|---|---|---|---|
| POST | `/api/payments/orders` | SCR-07 | student (self) | Harden | Trusts body `userId` today; must derive the user from auth and load the course price server-side; no ₹1,000 fallback (AUD-04) |
| POST | `/api/payments/orders/verify` | SCR-08 | student (self) | Harden | Signature check is fail-open today; must fail closed + idempotent (AC-19; AUD-03) |
| POST | `/api/payments/orders/simulate-success` | — | none | Retire | Unauthenticated, replayable; remove and replace with a `test`-profile fake (W6-04; AUD-04) |
| GET | `/api/payments/orders/{userId}` | SCR-22 | self | Retire | Leaks any user's orders; replace with `/orders/me` (AUD-04) |
| GET | `/api/payments/orders/me` | SCR-22 | self | New | Own orders only; paginated; used by payment history (W6-02/W6-14) |
| POST | `/api/payments/webhook` | — | Razorpay signature | Live | Correct fail-closed, idempotent path (AC-20; AUD-03) |

**Webhook events:** `payment.captured` → verify signature, check `razorpay_payment_id` uniqueness, call `/internal/enroll` once; `payment.failed` → mark order failed, no enrolment. Unknown events are ignored with a 200.

---

## WebSocket (STOMP)

Handshake endpoints (existing): `/ws` (SockJS fallback) and `/ws-native` (native WebSocket). `CONNECT` is authenticated by `Authorization: Bearer <token>` or a `token` native header (`WebSocketAuthInterceptor`). Today only `CONNECT` is checked; `SUBSCRIBE` is not authorized (A-16, `[E-F15]`).

| Frame | Destination | Direction | Perm (target) | Target | Notes |
|---|---|---|---|---|---|
| CONNECT | `/ws`, `/ws-native` | client → server | auth | Live | Token validated on handshake; invalid token currently leaves the session anonymous, not rejected |
| SEND | `/app/live-sessions/{sessionId}/chat` | client → server | enrolled | Harden | Server broadcasts to `/topic/live-sessions/{sessionId}/chat`; no enrolment check today |
| SUBSCRIBE | `/topic/live-sessions/{sessionId}/chat` | server → client | enrolled | Harden | Any authenticated socket can subscribe today |
| SEND | `/app/live-sessions/{sessionId}/reaction` | client → server | enrolled | Harden | Broadcasts to `/topic/live-sessions/{sessionId}/reactions` |
| SUBSCRIBE | `/topic/live-sessions/{sessionId}/reactions` | server → client | enrolled | Harden | Validate course enrolment/staff before allowing |
| SUBSCRIBE | `/topic/live-sessions/{sessionId}/status` | server → client | enrolled | New | Start/end/source-status event; destination authorized (W5-10) |
| SUBSCRIBE | `/user/queue/notifications` | server → client | self | New | Notification outbox delivery (W5-06) |
| SUBSCRIBE | `/topic/course/{courseId}` | server → client | enrolled | New | Course announcements/live start (A-17) |
| SUBSCRIBE | `/topic/admin/**` | server → client | admin | New | Admin alerts; reject for non-admin (A-16) |

**Rule:** after W1-09 every `SUBSCRIBE` is checked against the destination and the principal; a mismatch closes the session with an error frame. No client-supplied `userId` is trusted in a payload.
