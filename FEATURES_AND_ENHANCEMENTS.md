# LearnSphere AI — Features & Platform Enhancements

**Author:** Vishank Singh (<singhvishank20@gmail.com>)  
**Project:** LearnSphere AI — Enterprise Learning Management & AI Tutoring Platform  
**Repository:** `https://github.com/TCET-MOOC/LearnSphere-AI-Learning-Platform.git`  
**Tracking & Architecture Specs:** [`TRACKER.md`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/TRACKER.md) · [`AGENTS.md`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/AGENTS.md) · [`Docs/PLAN.md`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/Docs/PLAN.md)  
**Total Contributed Commits:** 55 commits  
**Timeline:** June 2026 – October 2026  

---

## Executive Summary

This document provides a comprehensive technical catalogue of the core features, architectural overhauls, security hardening, and UI/UX enhancements implemented by **Vishank Singh** across the LearnSphere AI platform.

Contributions span across the entire technology stack:
- **Backend Architecture**: Spring Boot 3.3.5 (Java 17/21), Spring Security, Spring Data JPA, Hibernate, PostgreSQL 16, Flyway, Caffeine Cache, and Testcontainers.
- **Microservices & Integrations**: Standalone Razorpay payments microservice, YouTube Data API v3, Google OAuth 2.0 channel publisher, NVIDIA NIM LLM AI integration, and STOMP over WebSocket.
- **Frontend Web Application**: Angular 19+ (Standalone Components, RxJS, Lucide icons, custom glassmorphic styling, unified Design System tokens, and Vitest/Playwright harnesses).
- **Security & Reliability**: In-memory token management, IDOR defense via unified `OwnershipService`, atomic refresh token rotation, RFC 9457 problem+json compliance, and strict Flyway database versioning.

---

## 1. Core Feature Domains & Enhancements

### Domain 1: Video Player & Media Delivery Architecture

* **Unified Dual-Engine Video Player**:
  * Engineered a custom glassmorphic video player supporting both native HTML5 / HLS video streaming and embedded YouTube content via the YouTube Iframe Embed API.
  * Interactive playback controls: play/pause, seeking scrubber with buffered duration indicators, volume slider with mute toggling, time badges, and speed selector (`0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`).
  * Advanced viewport controls: Theater Mode, Picture-in-Picture (PiP), and full-screen toggle with custom overlay styling.
  * Comprehensive keyboard shortcut bindings: `Space` (play/pause), `ArrowLeft`/`ArrowRight` (±5s seek), `ArrowUp`/`ArrowDown` (volume adjustment), `M` (mute), `F` (fullscreen toggle).
  * Real-time watch telemetry: automatically captures watch intervals and syncs progress with backend `UserProgress` tracking.
* **YouTube Data API v3 Ingestion Engine**:
  * Implemented `YouTubeService` and `YouTubeController` with automated ingestion and parsing of entire YouTube playlists and standalone video URLs into course lectures.
  * Metadata extraction: parses video duration, high-resolution thumbnail URLs, video titles, and full video descriptions.
  * Ingests YouTube playlist videos directly as canonical `CourseContent` items with `VIDEO` content type under target modules with contiguous ordering (`W2-09`).
  * In-memory transcript cache using `ConcurrentHashMap` with graceful fallback handling for quota exhaustion and network failures.
* **Canonical Media Storage & Secure Streaming (`W2-12`)**:
  * Multi-source video architecture backed by Flyway migration `V8__canonical_media_source.sql` supporting `UPLOAD`, `YOUTUBE`, and `EXTERNAL` source types.
  * Secure HTTP Range (`206 Partial Content`) streaming engine implemented via `MediaPlaybackService`.
  * Time-limited, tamper-proof HMAC-SHA256 signed stream descriptors preventing direct hotlinking and video source URL leakage.
  * Entitlement verification preventing unauthenticated or unenrolled users from accessing protected course videos.
* **Opt-In YouTube Channel Publishing (`W2-13`)**:
  * Integrated Google OAuth 2.0 flow for instructors to securely connect their YouTube channels.
  * Implemented `YouTubePublishService` with single-use HMAC-SHA256 state tokens, replay protection, and AES-128 credential encryption.
  * Asynchronous, idempotent background media publishing jobs (`media_publish_jobs`) with conflict resolution (`409 Conflict` on concurrent requests) and local upload fallbacks.

---

### Domain 2: 3-Tier Curriculum Decomposition & Lifecycle Management

* **3-Tier Hierarchy Model**:
  * Conformed course architecture to strict enterprise hierarchy: $\text{Course} \longrightarrow \text{Module} \longrightarrow \text{CourseContent} / \text{Assignment}$.
  * Department Association (`W2-01`): Added mandatory `courses.department_id` foreign key with backfill migrations and department validation on course creation.
  * Module & Content Contiguous Ordering (`W2-04`): Enforced strict $1..n$ positional ordering without gaps for modules and course contents; provided dedicated reordering endpoints (`PUT /api/modules/{id}/reorder`, `PUT /api/contents/{id}/reorder`) and automatic position renormalization upon deletion.
  * Legacy Lecture Migration (`W2-05`): Migrated legacy 1:1 lectures into canonical `CourseContent` (`VIDEO`) via Flyway `V5__legacy_lecture_migration.sql`, mapped legacy IDs, and retired deprecated mutation endpoints with informative `400 Bad Request` messages.
* **Course State Machine & Archival (`W2-02`, `W2-03`)**:
  * Formalized lifecycle states: `DRAFT` $\rightarrow$ `PENDING` $\rightarrow$ `LIVE` $\rightarrow$ `ARCHIVED`.
  * Replaced unsafe cascading deletions with safe soft-delete archiving and restoration endpoints, preserving student enrollment histories, test attempts, and progress records.
  * Admin review workflow: dedicated admin approval (`PUT /api/admin/courses/{id}/approve`) and rejection with revision notes.
* **Catalogue Reference Caching & ETags (`W2-11`)**:
  * Integrated high-performance Caffeine cache (`spring-boot-starter-cache`) configured with 60s TTL and a maximum of 1,000 items.
  * Cached public catalog summaries, course details, departments, and departmental course listings.
  * Generation-based deterministic ETags supporting conditional HTTP requests (`304 Not Modified` on matching `If-None-Match`).
  * Enforced strict `Cache-Control: private, no-store` bypass for drafts, pending courses, and staff views, with automated cache invalidation on any course/module/content mutation.
* **Assignment Lifecycle & Validation (`W2-06`)**:
  * Enforced strict validation: non-blank title, strictly positive `maxMarks > 0`, and future `dueDate` validation.
  * Submission rules: mandatory non-blank submission text or valid file attachment; IDOR checks preventing unauthorized access.
* **Notes & Bookmarks Rebinding (`W2-08`)**:
  * Rebound student personal notes and bookmarks from legacy lectures to canonical `CourseContent` via Flyway `V7__notes_bookmarks_content_rebind.sql`.
  * Validated timestamp boundaries: non-negative and clamped within `[0, durationSeconds]`.

---

### Domain 3: Authentication, Session Management & Enterprise Security

* **Memory-Only Client Session Architecture (`W1-06`, `AUD-15`)**:
  * Completely purged legacy `localStorage` token storage (`ls_token`, `ls_user`) to eliminate XSS-based credential theft.
  * Implemented reactive Angular `AuthStore` maintaining access tokens strictly in browser memory.
  * Configured `tokenInterceptor` to attach Bearer tokens from memory and enable `withCredentials: true`.
  * Configured `errorInterceptor` to intercept HTTP 401 Unauthorized responses, trigger silent token refresh, and retry the failed request transparently.
* **Refresh Token Rotation & Revocation (`W1-05`)**:
  * Persisted refresh tokens in PostgreSQL via Flyway `V3__refresh_tokens.sql` with token family tracking and status flags (`ACTIVE`, `ROTATED`, `REVOKED`).
  * 7-day token issuance using secure `HttpOnly`, `SameSite=Lax`, `Path=/api/auth` browser cookies.
  * Strict atomic token rotation: reusing a rotated token triggers immediate replay attack detection, instantly revoking the entire token family.
  * Server-side logout revocation clearing database session state and wiping cookies.
* **JWT Claims & Lifetime Standardization (`W1-04`)**:
  * Shortened access token validity to 15 minutes (900,000 ms).
  * Standardized token claims payload: `userId`, `roles`, and `dept_id`.
  * Enforced strict issuer (`learnsphere`) and audience (`learnsphere-web`) validation in `JwtService` and `JwtAuthFilter`.
* **Account Lifecycle States (`W1-03`)**:
  * Constrained account lifecycle to `ACTIVE`, `FLAGGED`, and `BLACKLISTED`.
  * Immediate rejection of logins and active JWT tokens for `BLACKLISTED` accounts (`401 Unauthorized`).
  * Read-only restriction for `FLAGGED` accounts via `FlaggedUserInterceptor` (mutating HTTP verbs return `403 Forbidden`).
  * Dynamically derived `isActive` boolean on user DTOs as `status != BLACKLISTED`.
  * Append-only admin audit log query endpoint (`GET /api/admin/audit`) with `PageResponse` pagination and `Cache-Control: private, no-store`.
* **Role Vocabulary & Registration Hardening (`W1-01`, `W1-02`)**:
  * Frozen role vocabulary: `ADMIN`, `TEACHER`, `STUDENT`.
  * Canonicalized role parsing case-insensitively, aliasing legacy `INSTRUCTOR` to canonical `TEACHER`.
  * Locked down role management endpoints (`/api/roles/**`) strictly to administrators.
  * Public registration hardened to allow only `STUDENT` account creation; self-registration with `ADMIN` or `TEACHER` is rejected with `400 Bad Request`.
  * Teacher onboarding converted into an application workflow with verified college affiliation (`POST /api/auth/verify-college`) and mandatory administrator approval.
* **Unified IDOR Ownership Defense (`W1-08`)**:
  * Centralized `OwnershipService` implementing authorization matrices across all domain controllers: `requireCourseStaff`, `requireModuleStaff`, `requireContentStaff`, `requireAssignmentStaff`, `requireTestStaff`, `requireAttemptOwnerOrStaff`, `requireSubmissionOwnerOrStaff`, `requireEnrolled`, and `requireSelf`.
  * To prevent resource enumeration attacks, unauthorized access attempts by foreign users return RFC 9457 `404 Not Found` rather than `403 Forbidden`.
* **WebSocket Handshake & Topic Authorization (`W1-09`)**:
  * `WebSocketAuthInterceptor` validating JWT tokens and checking account active status upon STOMP `CONNECT`.
  * Destination-level `SUBSCRIBE` authorization:
    * `/topic/admin/**` $\rightarrow$ strictly `ADMIN` role.
    * `/topic/course/{courseId}/**` & `/topic/live-sessions/{sessionId}/**` $\rightarrow$ enrolled students or course staff/admin.
    * `/user/...` $\rightarrow$ strictly matching authenticated user principal.
  * Server-side principal enforcement in `LiveChatWebSocketController`, overriding client-supplied user IDs.
* **Private File Storage Port (`W2-07`)**:
  * Built `StorageService` port and `LocalStorageService` with 200MB size limits, MIME type verification, and file magic-byte inspection.
  * Replaced public static `/api/uploads/**` directory with private storage mapped via opaque 32-character hex keys (`StoredFile`).
  * Protected file access endpoint (`GET /api/files/{key}`) with ownership checks and `Cache-Control: private, no-store`.

---

### Domain 4: Interactive Learning, AI & Real-Time Collaboration

* **Role-Based Direct Messaging System**:
  * Created dedicated messaging views for Teachers, Students, and Admins (`ab10f05`, `9e97fca`).
  * Features active conversation lists, real-time message history, recipient selection, and responsive chat views.
  * Seamless header integration: dynamic topbar message icon with unread badges and persistent routing.
* **Interactive Lecture Experience & Quizzes**:
  * Synchronized interactive transcript viewer allowing students to jump to exact video timestamps by clicking spoken text lines.
  * Post-Video Quizzes: Interactive quiz modal automatically triggered when video playback reaches completion, featuring real-time client-side and server-side scoring.
  * Interactive timestamped lecture note-taking and bookmarking panel attached directly to video playback coordinates.
* **Real-Time Live Session Chat**:
  * Built live classroom chat powered by WebSockets / STOMP.
  * Automated profanity and abuse filter (`ProfanityFilterService`) providing automated censorship and message filtering.
* **NVIDIA NIM AI Chatbot**:
  * Integrated NVIDIA NIM large language model (LLM) tutoring assistant for real-time contextual student Q&A, syllabus explanations, and automated quiz generation.

---

### Domain 5: Admin Suite, Governance & Operational Remediations

* **Admin UI Redesign & Gamification**:
  * Redesigned Admin dashboard with custom podium badges featuring Gold, Silver, and Bronze medals for top performers (`784b444`).
  * Replaced raw unicode characters and hardcoded styles with unified Lucide icon components and Design System tokens.
* **User Management & Audit Remediations**:
  * Real-time search by name, email, department, and role with live debouncing.
  * Slide-out inspection drawer modal displaying detailed user telemetry, enrollment counts, and activity logs.
  * Confirmation modal dialogs for blacklisting, restoring, and role changes.
* **Financial Operations & Concurrency Fixes**:
  * Payouts and revenue disbursement workflows protected by confirmation dialogs.
  * Optimized repository queries (`findByStatusWithRelations`, `sumAmountByStatus`) with read-only transactions to eliminate database deadlocks.
  * Isolated automated content moderation scanning into `REQUIRES_NEW` transactions to prevent read/write query contention.
* **Institutional & Communication Management**:
  * Institutional college management: migrated styles to design tokens, added institution detail and creation modals.
  * Broadcast announcement composer modal with target audience (All, Students, Teachers) and category filters.

---

### Domain 6: Payments Microservice & Student Purchases

* **Standalone Payments Architecture**:
  * Modularized payments into a dedicated Spring Boot microservice (`learnsphere-payments-service`) running on port 8081 with its own PostgreSQL database (`learnsphere_payments`).
  * Flyway database baseline for orders and payment transactions.
* **Razorpay Gateway Integration**:
  * Order creation with HMAC-SHA256 signature verification to prevent payment tampering.
  * Automated teacher royalty calculation and split payouts.
* **Student Purchases Portal**:
  * Dedicated frontend purchases interface (`/student/purchases`) with payment history, status filters (Success, Failed, Pending), and invoice generation/download.

---

### Domain 7: Enrollment & Progress Integrity

* **Enrollment Uniqueness & Course State Enforcement (`W3-01`)**:
  * Applied Flyway migration `V10__unique_enrollment.sql` creating unique constraint `uq_enrollments_user_course_role` on `(user_id, course_id, role)`.
  * Restricted enrollments strictly to `LIVE` courses (non-LIVE courses return `400 Bad Request`).
  * Enforced self-only enrollment for students (foreign `user_id` submissions return `403 Forbidden`).
* **Roster Access & Unenrollment Authorization (`W3-02`)**:
  * Protected class roster endpoint (`GET /api/courses/{courseId}/enrollments`), restricting access to course staff and admins while returning 404 for unauthorized users.
  * Unenrollment (`DELETE /api/enrollments/{id}`) strictly limited to the enrolled student self or admins.
* **User Progress Integrity & Upsert (`W3-03`)**:
  * Applied Flyway migration `V11__user_progress_unique_and_decimal.sql` enforcing unique constraint `uq_user_progress_user_content` on `(user_id, content_id)`.
  * Migrated `progress_percent` column to `NUMERIC(5,2)` precision.
  * Implemented atomic upsert semantics in `EnrollmentServiceImpl` (updating existing records instead of generating duplicate entries).
  * Clamped progress values to valid range `[0.00, 100.00]` with scale 2.

---

### Domain 8: Design System & Visual Identity Overhaul

* **"Verified Academic" Design Identity (`W2-16`, `ADR-16`, `DS-08`)**:
  * Established an institutional verified academic identity motif: ink/paper color palettes, gold/emerald/indigo verification seals, and transcript-grade tabular numerals.
  * Centralized design tokens in `_tokens.scss` (`--verified-ink`, `--verified-paper`, `--verified-seal-gold`, `--verified-seal-emerald`, `--verified-stamp-border`).
  * Created reusable utility classes: `.verified-seal-badge`, `.verified-stamp`, and tabular numeral styles in `_typography.scss`.
  * Rebuilt the public certificate verification page (`/verify-certificate/:id`) to design tokens, eliminating 23 hex literals and 11 radius literals.
* **Public Surfaces Rebuild (`W2-17`, `DS-09`)**:
  * Complete tokenized rebuild of public-facing pages: Landing Page, Login, Register, Forgot Password, and College Verification.
  * Integrated institutional verified seal badge into the Landing Page hero section.
  * Replaced CSS fallback values with canonical design tokens, eliminating raw hex codes and literal border radii.
* **Icon Unification to Lucide (`W2-15`, `DS-07`)**:
  * Removed Google Material Icons font stylesheet from `index.html`.
  * Replaced all legacy `<mat-icon>` tags across student and teacher dashboards with canonical Lucide Angular components (`<lucide-icon>`).
  * Standardized icon sizes to token scales (14px, 16px, 18px, 20px, 24px) and added `aria-hidden="true"` to decorative icons.
* **Stylelint Ratchet & CSS Governance (`W1-12`, `W2-14`, `DS-01`, `DS-06`)**:
  * Established an automated Stylelint baseline ratchet script (`scripts/stylelint-ratchet.mjs`) preventing the introduction of any new raw hex colors, hardcoded radii, or transition literals.
  * Tightened metric ceilings with each wave, burning down technical CSS debt.

---

### Domain 9: DevOps, CI/CD & Production-Grade Engineering

* **Test-First Rigor & Testcontainers Harness (`P0-02`)**:
  * Replaced stale tests with a robust integration test harness running against disposable `postgres:16-alpine` Docker containers via `PostgresIntegrationTest`.
  * Configured Vitest test runner for Angular frontend components.
  * Enforced strict red/green test-first development evidence recording.
* **Multi-Stage CI/CD Pipeline (`P0-03`)**:
  * Implemented GitHub Actions workflow (`.github/workflows/ci.yml`) triggering on pushes and pull requests.
  * Automated test matrix: backend Testcontainers suite, payments service build, frontend unit tests, frontend production build, and Playwright smoke tests.
  * Final `CI gate` job requiring all four sub-jobs to pass before merge approval.
* **Flyway Schema Migrations (`P0-04`)**:
  * Introduced Flyway database baselines (`V1__baseline.sql`) across backend (38 tables) and payments service (2 tables).
  * Enforced `spring.jpa.hibernate.ddl-auto=validate`, ensuring database schemas are driven strictly through versioned, reviewable SQL migrations.
* **Full Local Docker Compose Stack (`P0-05`)**:
  * Multi-service `docker-compose.yml` orchestrating PostgreSQL 16 (with automated dual-database creation for `MOOC` and `learnsphere_payments`), Spring Boot backend, payments microservice, and nginx-served Angular SPA.
  * Configured automated healthcheck probes and SPA routing fallbacks.
* **Observability & Actuator Probes (`P0-06`)**:
  * Configured Spring Boot Actuator health endpoints with Kubernetes-compatible liveness and readiness probes.
  * Structured JSON logging via Logstash Logback encoder with distributed Trace ID propagation between backend and payments microservices.
* **API Standards & Conventions (`P0-09`)**:
  * Standardized error handling on RFC 9457 `application/problem+json` envelopes.
  * Implemented standardized cursor and offset pagination responses via `PageResponse<T>`.
  * Integrated Springdoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`).

---

## 2. Complete Git Commit Traceability Matrix

The following table records every commit authored by **Vishank Singh**, organized chronologically:

| Commit | Date | Area / Story | Commit Subject & Description |
|:---|:---:|:---:|:---|
| [`9e97fca`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/9e97fca) | 2026-06-21 | Messaging | `feat: add teacher messages page with conversation list and chat view` |
| [`ab10f05`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/ab10f05) | 2026-06-21 | Messaging | `feat: add messages pages for all roles (student, admin) with topbar navigation` |
| [`c54f31d`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/c54f31d) | 2026-06-29 | Navigation | `fix: re-add routerLink to topbar message icon` |
| [`103bda3`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/103bda3) | 2026-06-29 | Navigation | `re routing the messages icon` |
| [`56a0cc9`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/56a0cc9) | 2026-08-16 | Git / Sync | `Merge branch 'main' of github.com/TCET-MOOC/LearnSphere-AI-Learning-platform` |
| [`25815a6`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/25815a6) | 2026-08-16 | Git / Sync | `Merge branch 'main' into test-vis` |
| [`8000ccd`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/8000ccd) | 2026-08-19 | Full Platform | `feat: full platform enhancements — YouTube Data API, Video Player, Courses, Razorpay microservice, and Admin audit remediation` |
| [`784b444`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/784b444) | 2026-08-19 | Architecture | `feat(admin,backend,docs): complete admin UI redesign, podium medals, backend modularization, and updated architecture documentation` |
| [`920e21a`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/920e21a) | 2026-10-26 | Product Snapshot | `Snapshot: pre-plan product work in progress (not a story)` |
| [`c09c1d8`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/c09c1d8) | 2026-10-26 | Foundations (`P0-01`) | `P0-01: Freeze the source pack and docs tree` |
| [`d8e5940`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/d8e5940) | 2026-10-26 | Foundations (`P0-02`) | `P0-02: Replace the stale tests with a real harness + test-first evidence format` |
| [`3d7e242`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/3d7e242) | 2026-10-26 | Foundations (`P0-03`) | `P0-03: GitHub Actions CI` |
| [`a74bb03`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/a74bb03) | 2026-10-26 | Foundations (`P0-04`) | `P0-04: Flyway baseline + ddl-auto=validate` |
| [`a94b28e`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/a94b28e) | 2026-10-26 | Foundations (`P0-05`) | `P0-05: Docker Compose local stack` |
| [`123134e`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/123134e) | 2026-10-26 | Foundations (`P0-06`) | `P0-06: Actuator health + structured logs` |
| [`5dd2d43`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/5dd2d43) | 2026-10-26 | Foundations (`P0-07`) | `P0-07: Config hygiene (root duplicate build, env vars, profiles)` |
| [`9b40b7c`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/9b40b7c) | 2026-10-26 | Foundations (`P0-08`) | `P0-08: Seed profile separation + reset script` |
| [`f1d0e82`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/f1d0e82) | 2026-10-26 | Foundations (`P0-09`) | `P0-09: API conventions (problem+json, pagination, OpenAPI)` |
| [`bb75c6e`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/bb75c6e) | 2026-10-26 | Security (`W1-11`) | `W1-11: Lock down legacy /api/users/** (AUD-14)` |
| [`b771e39`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/b771e39) | 2026-10-26 | Design System (`W1-12`) | `W1-12: Visual baseline harness + token inventory + stylelint ratchet (DS-01)` |
| [`995d1bf`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/995d1bf) | 2026-10-26 | Tracking | `TRACKER: record the story split, reopen W1-01, track CLAUDE.md` |
| [`5bbe26c`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/5bbe26c) | 2026-10-26 | Milestone | `Phase 0 complete` |
| [`3aa3c7f`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/3aa3c7f) | 2026-10-27 | Identity (`W1-01`) | `W1-01: Freeze role vocabulary and seed roles` |
| [`36235cb`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/36235cb) | 2026-10-27 | Identity (`W1-02`) | `W1-02: Registration hardening (STUDENT only, teacher application)` |
| [`a6deeef`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/a6deeef) | 2026-10-27 | Identity (`W1-03`) | `W1-03: Account lifecycle ACTIVE/FLAGGED/BLACKLISTED` |
| [`79365d9`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/79365d9) | 2026-10-27 | Auth (`W1-04`) | `W1-04: JWT claims and lifetime` |
| [`9437268`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/9437268) | 2026-10-27 | Auth (`W1-05`) | `W1-05: Refresh tokens, rotation, logout revocation` |
| [`80cf3a5`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/80cf3a5) | 2026-10-27 | Auth (`W1-06`) | `W1-06: Client session: memory-only access token` |
| [`685f8f4`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/685f8f4) | 2026-10-27 | Security (`W1-08`) | `W1-08: One ownership layer across controllers (IDOR protection)` |
| [`970dcc4`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/970dcc4) | 2026-10-27 | Security (`W1-09`) | `W1-09: WebSocket handshake + SUBSCRIBE authorization` |
| [`af8668c`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/af8668c) | 2026-10-27 | Security (`W1-10`) | `W1-10: Auth/IDOR regression suite` |
| [`0acbde2`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/0acbde2) | 2026-10-27 | UI / Theme (`W1-13`) | `W1-13: Theme correctness: public routes light, no hardcoded surfaces (DS-02)` |
| [`c75ec2d`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/c75ec2d) | 2026-10-27 | UI / Type (`W1-14`) | `W1-14: Typography classes and heading semantics (DS-03)` |
| [`a268a34`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/a268a34) | 2026-10-27 | UI / Landing (`W1-15`) | `W1-15: Landing truth, nav and dead links, no restyle (DS-04)` |
| [`38f5597`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/38f5597) | 2026-10-27 | UI / Auth (`W1-16`) | `W1-16: Auth screens on tokens (DS-05)` |
| [`73d7b92`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/73d7b92) | 2026-10-27 | Curriculum (`W2-01`) | `W2-01: Add courses.department_id FK, backfill, and validate department on course creation` |
| [`e2dc073`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/e2dc073) | 2026-10-27 | Curriculum (`W2-02`) | `W2-02: Course state machine (DRAFT/PENDING/LIVE/ARCHIVED), isPublished projection, and admin approval` |
| [`d587ce3`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/d587ce3) | 2026-10-27 | Curriculum (`W2-03`) | `W2-03: Course archive/soft-delete and restore, replacing hand-written cascade` |
| [`413d3f8`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/413d3f8) | 2026-10-27 | Curriculum (`W2-04`) | `feat(curriculum): module and content ordering, reorder endpoints, and delete renormalisation (W2-04)` |
| [`459894e`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/459894e) | 2026-10-27 | Curriculum (`W2-05`) | `feat(curriculum): migrate Lecture 1:1 to CourseContent and retire legacy authoring (W2-05)` |
| [`ebfc48e`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/ebfc48e) | 2026-10-27 | Curriculum (`W2-06`) | `feat(curriculum): assignment validation on create and update (W2-06)` |
| [`1bba6f0`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/1bba6f0) | 2026-10-27 | Media / Storage (`W2-07`) | `feat(curriculum): storage port, upload safety, opaque file keys, and remove public uploads (W2-07)` |
| [`89e290c`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/89e290c) | 2026-10-27 | Curriculum (`W2-08`) | `feat(curriculum): rebind notes and bookmarks to CourseContent (W2-08)` |
| [`de8a128`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/de8a128) | 2026-10-27 | Media (`W2-09`) | `feat(curriculum): YouTube import into canonical content, transcript cache, and quota handling (W2-09)` |
| [`aa7c307`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/aa7c307) | 2026-10-27 | Testing (`W2-10`) | `test(curriculum): curriculum contract suite for ordering, states, validation, and storage (W2-10)` |
| [`23c77fc`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/23c77fc) | 2026-10-28 | Performance (`W2-11`) | `feat(curriculum): implement public catalogue reference cache and ETag invalidation (W2-11)` |
| [`762c571`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/762c571) | 2026-10-28 | Media (`W2-12`) | `W2-12: Canonical media source + secure local playback` |
| [`9c1d833`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/9c1d833) | 2026-10-28 | Media (`W2-13`) | `W2-13: Opt-in YouTube channel publish (OAuth + idempotent upload)` |
| [`8eb6895`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/8eb6895) | 2026-10-28 | Design System (`W2-14`) | `W2-14: Token consolidation + lint ratchet to zero-new (DS-06)` |
| [`14fe981`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/14fe981) | 2026-10-28 | Design System (`W2-15`) | `W2-15: Icon unification: Lucide only (DS-07)` |
| [`7b014cb`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/7b014cb) | 2026-10-28 | Design System (`W2-16`) | `W2-16: Identity direction + proof page (DS-08)` |
| [`a125912`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/a125912) | 2026-10-28 | Design System (`W2-17`) | `W2-17: Public surfaces rebuild on approved direction (DS-09)` |
| [`146dd7f`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/146dd7f) | 2026-10-28 | Enrollment (`W3-01`) | `W3-01: Enrolment unique, self-only, published check` |
| [`ff79d6b`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/ff79d6b) | 2026-10-28 | Enrollment (`W3-02`) | `W3-02: Roster and unenrol authorization` |
| [`9a2360c`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/commit/9a2360c) | 2026-10-28 | Progress (`W3-03`) | `W3-03: Progress unique + upsert + BigDecimal` |

---

## 3. Architectural Impact & Quality Metrics

1. **Test-First Coverage & Reliability**:
   - Backend test suite expanded to **248+ integration tests** running against disposable Testcontainers PostgreSQL 16.
   - Payments microservice backed by **10+ integration tests**.
   - Frontend unit test suite expanded to **40+ Vitest tests** covering auth, session interceptors, verified identity, landing page, and UI components.
   - End-to-end user journey verification via automated Playwright smoke tests (`npm run e2e`).

2. **Database Schema Integrity**:
   - 11 Flyway migration versions created and validated (`V1` baseline through `V11`), establishing strict schema validation (`ddl-auto=validate`).
   - Key constraints introduced:
     - `uq_enrollments_user_course_role` on `enrollment(user_id, course_id, role)`.
     - `uq_user_progress_user_content` on `user_progress(user_id, content_id)`.
     - Partial unique index on `course_content(legacy_lecture_id)`.
     - `courses.department_id` foreign key with cascade protections.
     - Token family tracking on `refresh_tokens`.

3. **UI/UX Consistency & Accessibility**:
   - Completely unified icon ecosystem on **Lucide icons**, removing heavy third-party font icon bundles.
   - Design System tokenization: eliminated hundreds of raw hex color literals, hardcoded border radii, and unrestricted CSS transitions through automated Stylelint ratchets.
   - Full dark/light mode theme responsiveness with dedicated public route theme normalization.
