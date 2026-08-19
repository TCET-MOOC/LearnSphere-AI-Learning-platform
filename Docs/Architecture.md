# LearnSphere Architecture Documentation

## High-Level Architecture

LearnSphere follows a modern decoupled web application architecture combining an **Angular 19** Single-Page Application (SPA) with a **Dual Spring Boot** microservice backend, **PostgreSQL** relational persistence, **YouTube Data API v3** for streaming, and **NVIDIA NIM** for LLM intelligence.

```
+-------------------------------------------------------------+
|               Client Tier (Angular 19 SPA)                  |
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
   - Angular 19 standalone components, Lucide icons, responsive SCSS design tokens, and tabular numeric typography.
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
