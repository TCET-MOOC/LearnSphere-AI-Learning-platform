# LearnSphere Architecture Documentation

> **Corrected 2026-11-03 (W7-01, ADR-06):** the frontend is Angular 21.2 (this document first said version 19). Toolchain and upgrade policy: [`Docs/Frontend.md`](Frontend.md#toolchain-and-upgrade-policy). The rest of this document is the source revision frozen on 2026-10-22 (`Docs/source/MANIFEST.md`); where it differs from `Docs/source/spec.md` or the code, those win (ADR-01).

## High-Level Architecture

LearnSphere follows a modern decoupled web application architecture combining an **Angular 21.2** Single-Page Application (SPA) with a **Dual Spring Boot** microservice backend, **PostgreSQL** relational persistence, **YouTube Data API v3** for streaming, and **NVIDIA NIM** for LLM intelligence.

```
+-------------------------------------------------------------+
|              Client Tier (Angular 21.2 SPA)                 |
|  - Student Portal  - Teacher Workspace  - Admin Command     |
+------------------------------+------------------------------+
                               | (HTTP / REST / WebSocket)
                               v
+------------------------------+------------------------------+
|                    Application Backend Tier                 |
|                                                             |
|  [Main Service: Port 8080]       [Payments Service: 8081]   |
|  - Auth & RBAC (JWT)             - Checkout Processing      |
|  - Course Catalog & YouTube v3   - 70/30 Royalty Split      |
|  - Quizzes & Assessments         - Teacher Payout Ledger    |
|  - Moderation & Sentiment        - Financial Aggregates     |
|  - NVIDIA NIM LLM Integration                               |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|              Data Persistence & External Services           |
|  - PostgreSQL Database (Tables for users, courses, etc.)    |
|  - YouTube Data API v3 (Video ingestion & streaming)        |
|  - NVIDIA NIM API (LLaMA 3.1 70B Instruct for AI tutoring)  |
+-------------------------------------------------------------+
```

---

## Service Breakdown

1. **Frontend (`learnSphere/frontend/learnsphere/`)**:
   - Angular 21.2 standalone components, Lucide icons, responsive SCSS design tokens, and tabular numeric typography.
2. **Main Backend (`learnsphere-backend/`, Port 8080)**:
   - Spring Boot 3.3.5, Spring Security with stateless JWT, Spring Data JPA, and WebSocket STOMP.
3. **Payments Microservice (`learnsphere-payments-service/`, Port 8081)**:
   - Dedicated Spring Boot service managing checkout transactions, 70% faculty / 30% platform split logic, and pending teacher disbursement records.
4. **AI & Intelligence Tier**:
   - NVIDIA NIM Cloud API integration (`NvidiaNimService`) for quiz generation and in-app AI tutoring.
5. **Database**:
   - Relational PostgreSQL database (`MOOC` / default port 5432).

---

## Security Model

- **Authentication**: Stateless JWT tokens passed via `Authorization: Bearer <token>` header.
- **Authorization**: Role-Based Access Control (`ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_ADMIN`).
- **Passwords**: BCrypt salted password hashing.

---

## Organizational Reference Standard & Single Source of Truth

As established in [`AGENTS.md`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/AGENTS.md) and [`LMS_Entity_Reference.pdf`](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/LMS_Entity_Reference.pdf), all future database models, entity mappings, business rules, and API routes strictly conform to the 10 domain specifications:
1. **Department & Role** (`department`, `role`)
2. **User Account & User Role** (`user_account`, `user_role`)
3. **Course & Module** (`course`, `module`)
4. **Course Content & Assignment** (`course_content`, `assignment`)
5. **Enrollment & User Progress** (`enrollment`, `user_progress`)
6. **Test & Test Attempt** (`test`, `test_attempt`)
7. **Question & Student Answer** (`question`, `student_answer`)
8. **Leaderboard & Assignment Submission** (`leaderboard`, `assignment_submission`)
9. **Feedback & Progress Detail** (`feedback`)
10. **Discussion Thread & Post** (`discussion_thread`, `post`)

