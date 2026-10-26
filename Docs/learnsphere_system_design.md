# LearnSphere AI — System Design Document

> A deep-dive into the architecture, data flow, design decisions, and trade-offs of the LearnSphere AI Online Learning Platform.

---

## 1. High-Level Architecture Overview

LearnSphere is a **distributed, 3-tier web application** composed of three independently deployable services communicating over HTTP/REST and WebSockets.

```mermaid
graph TD
    Browser["🌐 Browser\n(Angular 19+)"]

    subgraph Services
        Backend["⚙️ Main Backend\nSpring Boot 3.3.5\nPort :8080"]
        Payments["💳 Payments Microservice\nSpring Boot\nPort :8081"]
        NvidiaAI["🤖 NVIDIA NIM API\n(External LLM)"]
        Razorpay["💰 Razorpay\n(External Payment)"]
    end

    DB[("🗄️ PostgreSQL\nMain Database")]
    Uploads["📁 File Storage\n/uploads (local)"]

    Browser -- "REST (HTTP/JSON)\nJWT Bearer" --> Backend
    Browser -- "WebSocket /ws/**" --> Backend
    Browser -- "REST" --> Payments
    Backend -- "JPA / Hibernate" --> DB
    Backend -- "HTTP POST" --> NvidiaAI
    Payments -- "Razorpay SDK" --> Razorpay
    Payments -- "Internal API /internal/**" --> Backend
    Backend -- "File I/O" --> Uploads
```

---

## 2. Frontend Architecture (Angular 19)

### 2.1 Feature-Based Module Structure

The frontend follows a **clean 3-layer architecture**:

```
src/app/
├── core/               ← Singleton services, guards, interceptors, models
│   ├── auth/           ← AuthService, auth.models, auth.guards
│   ├── guards/         ← AuthGuard, RoleGuard
│   ├── interceptors/   ← JWT Interceptor (attaches Bearer token)
│   ├── models/         ← Shared TypeScript interfaces
│   └── services/       ← ApiService, StorageService, WebSocketService, etc.
│
├── features/           ← Role-based feature modules (lazy loaded)
│   ├── auth/           ← Login, Register, Forgot Password pages
│   ├── student/        ← Dashboard, Course Player, Progress, Profile
│   ├── teacher/        ← Course Builder, Lectures, Assessments, Students
│   ├── admin/          ← User Management, Reports, Moderation
│   ├── public/         ← Course Catalog, Landing Page
│   └── payment/        ← Checkout, Order History
│
├── shared/             ← Reusable UI components (buttons, modals, cards)
└── store/              ← State management (signals/BehaviorSubjects)
```

### 2.2 Authentication Flow (Frontend)

```mermaid
sequenceDiagram
    participant U as User
    participant A as Angular App
    participant S as StorageService
    participant API as Spring Backend

    U->>A: Enter credentials & submit
    A->>API: POST /api/auth/login
    API-->>A: { token, user }
    A->>S: setToken(JWT), setUser(user)
    A->>A: currentUserSubject.next(user)
    A->>U: Redirect to role-based dashboard
    Note over A,S: Token stored in memory/localStorage
    Note over A: All future requests via JWT Interceptor
```

### 2.3 Role-Based Routing

Three distinct portal views are guarded by `RoleGuard`:

| Role | Dashboard Route | Capabilities |
|------|----------------|--------------|
| `STUDENT` | `/student/dashboard` | Browse, enroll, watch, take tests |
| `TEACHER` | `/teacher/dashboard` | Create courses, manage content, grade |
| `ADMIN` | `/admin/dashboard` | User management, reports, moderation |

---

## 3. Backend Architecture (Spring Boot 3.3.5)

### 3.1 Layered Package Structure

```
com.MOOC.OnlineLearningPlatfrom/
├── Config/         ← SecurityConfig, WebSocketConfig, WebConfig, DataSeeder
├── Controller/     ← 45 REST Controllers (API surface layer)
├── Service/        ← Business logic (interfaces + impl)
├── Repository/     ← Spring Data JPA repositories
├── Entity/         ← 36 JPA entities
├── Dto/            ← Request/Response DTOs (decoupled from entities)
├── Security/       ← JwtService, JwtAuthFilter, CustomUserDetails
└── Exception/      ← Global exception handlers
```

### 3.2 Security Architecture

```mermaid
graph LR
    Request["Incoming HTTP Request"]
    CORS["CORS Filter\n(Allowed Origins)"]
    JWT["JwtAuthFilter\n(Validate Bearer Token)"]
    Auth["Spring Security\nAuthorize by Role"]
    Controller["Controller Method"]

    Request --> CORS --> JWT --> Auth --> Controller
```

**Key security decisions:**
- **Stateless sessions**: `SessionCreationPolicy.STATELESS` — no server-side session storage
- **BCrypt passwords**: All passwords hashed with BCrypt encoder
- **JWT claims**: `userId`, `roles`, `dept_id` embedded in token
- **Role-based URL guards**: `/api/admin/**` → `ROLE_ADMIN`, `/api/teacher/**` → `ROLE_TEACHER`
- **Public endpoints**: Auth, course browsing, certificates verify, file uploads, AI endpoints

### 3.3 WebSocket Support

Real-time communication powered by Spring WebSocket:
- **Endpoint**: `/ws/**` (STOMP over SockJS)
- **Auth**: `WebSocketAuthInterceptor` validates JWT before handshake
- **Use cases**: Live chat in sessions, real-time notifications

---

## 4. Domain Model (Entity Relationship)

### 4.1 Core Curriculum Decomposition

```mermaid
erDiagram
    Course ||--o{ Module : "has"
    Module ||--o{ CourseContent : "contains"
    Module ||--o{ Assignment : "has"
    Course ||--o{ Test : "has"
    Test ||--o{ Question : "contains"
    TestAttempt ||--o{ StudentAnswer : "records"
    UserAccount ||--o{ TestAttempt : "makes"
    UserAccount ||--o{ Enrollment : "has"
    Course ||--o{ Enrollment : "enrolls"
    UserAccount ||--o{ UserProgress : "tracks"
    CourseContent ||--o{ UserProgress : "tracked by"
```

### 4.2 All 36 Entities at a Glance

| Domain | Entities |
|--------|----------|
| **Auth & Identity** | `UserAccount`, `UserRole`, `Role`, `Department`, `College`, `AffiliationRequest` |
| **Curriculum** | `Course`, `Module`, `CourseContent`, `Lecture` |
| **Assessment** | `Test`, `TestAttempt`, `Question`, `StudentAnswer` |
| **Assignments** | `Assignment`, `AssignmentSubmission` |
| **Progress & Certs** | `UserProgress`, `Certificate`, `Leaderboard` |
| **Social** | `DiscussionThread`, `DiscussionPost`, `Post`, `Feedback`, `Announcement` |
| **Communication** | `Conversation`, `Message`, `Notification` |
| **Payments** | `Payment`, `Payout`, `Royalty` |
| **Utility** | `Bookmark`, `Note`, `FlaggedContent`, `LiveSession`, `PasswordResetToken` |

---

## 5. API Design

### 5.1 REST Endpoint Surface (45 Controllers)

```mermaid
graph LR
    subgraph Auth & Users
        auth["/api/auth/**"]
        users["/api/users/**"]
        roles["/api/roles/**"]
    end

    subgraph Curriculum
        courses["/api/courses/**"]
        modules["/api/modules/**"]
        contents["/api/contents/**"]
        lectures["/api/.../lectures/**"]
    end

    subgraph Assessment
        tests["/api/tests/**"]
        attempts["/api/attempts/**"]
        questions["/api/questions/**"]
        answers["/api/answers/**"]
    end

    subgraph AI Features
        ai["/api/ai/**\nextract-questions\nsummarize-transcript"]
        chatbot["/api/chatbot/**"]
    end

    subgraph Social & Communication
        threads["/api/threads/**"]
        posts["/api/posts/**"]
        feedback["/api/.../feedback/**"]
        discussions["/api/discussions/**"]
        messaging["/api/messaging/**"]
    end

    subgraph Payments
        payments["/api/payments/**"]
        royalty["/api/royalty/**"]
        revenue["/api/revenue/**"]
    end

    subgraph Admin
        admin["/api/admin/**"]
        moderation["/api/moderation/**"]
        reports["/api/reports/**"]
    end
```

### 5.2 HTTP Communication Pattern

```
Angular Service → ApiService.get/post/put/delete()
                → Attaches Authorization: Bearer <token> via Interceptor
                → Calls Spring Backend REST endpoint
                → Spring returns DTO (never raw Entity)
                → Angular maps to TypeScript interface
```

---

## 6. AI Integration (NVIDIA NIM)

### 6.1 Design Pattern — Graceful Degradation

```mermaid
flowchart TD
    A[Client sends transcript] --> B{NVIDIA NIM\nconfigured?}
    B -- Yes --> C[Call NvidiaNimService\nHTTP POST to NIM API]
    C -- Success --> D[Return AI-generated questions\naiPowered: true]
    C -- Failure --> E[Fall through to fallback]
    B -- No --> E
    E --> F[Return curated\nfallback questions\naiPowered: false]
```

**Key AI Endpoints:**
| Endpoint | Purpose |
|----------|---------|
| `POST /api/ai/extract-questions` | Generate MCQ quiz from video transcript |
| `POST /api/ai/summarize-transcript` | Summarize lecture content in any language |
| `GET /api/ai/status` | Check if NVIDIA NIM is live |
| `POST /api/chatbot/**` | Conversational AI tutoring |

---

## 7. Payments Architecture

### 7.1 Two-Service Payment Flow

```mermaid
sequenceDiagram
    participant S as Student (Browser)
    participant P as Payments Service (:8081)
    participant R as Razorpay
    participant B as Backend (:8080)
    participant DB as PostgreSQL

    S->>P: POST /payments/initiate {courseId, userId}
    P->>R: Create Razorpay Order
    R-->>P: orderId, amount
    P-->>S: Razorpay checkout params
    S->>R: Complete payment (Razorpay JS SDK)
    R-->>S: payment_id, signature
    S->>P: POST /payments/verify {payment_id, signature}
    P->>P: Verify HMAC signature
    P->>B: POST /internal/enrollment/confirm
    B->>DB: Create Enrollment record
    B-->>P: Enrollment confirmed
    P-->>S: Payment success
```

### 7.2 Royalty System
- After payment, instructor royalties tracked in `Royalty` entity
- Admin manages payouts via `PayoutAdminController`
- Revenue analytics via `RevenueController`

---

## 8. Real-Time Features

### 8.1 WebSocket Architecture

```mermaid
graph LR
    Angular["Angular\nWebSocketService\n(SockJS + STOMP)"]
    Auth["WebSocketAuthInterceptor\n(JWT validation)"]
    WS["Spring WebSocket\nSTOMP Broker"]
    Chat["LiveChatWebSocketController"]
    Notif["NotificationController"]

    Angular -- "ws://localhost:8080/ws" --> Auth
    Auth --> WS
    WS --> Chat
    WS --> Notif
    Chat -- "STOMP /topic/session/{id}" --> Angular
    Notif -- "STOMP /user/queue/notifications" --> Angular
```

**Real-time use cases:**
- 💬 Live session chat between instructor and students
- 🔔 Instant notifications (enrollment confirmed, grade posted, etc.)

---

## 9. Data Flow: Student Watches a Video

This end-to-end flow illustrates how all layers connect:

```mermaid
sequenceDiagram
    participant S as Student
    participant A as Angular
    participant B as Backend
    participant DB as PostgreSQL
    participant AI as NVIDIA NIM

    S->>A: Click "Play" on lecture
    A->>B: GET /api/courses/{id}/lectures (with JWT)
    B->>DB: Query CourseContent by module_id
    DB-->>B: CourseContent list
    B-->>A: LectureResponseDto list
    A->>S: Render video player (YouTube/file URL)

    S->>A: Video playing... 90% watched
    A->>B: POST /api/progress {contentId, progressPercent: 90}
    B->>DB: UPSERT UserProgress (completedAt set if ≥90%)
    DB-->>B: Progress saved
    B-->>A: UserProgress response

    S->>A: Click "Generate Quiz"
    A->>B: POST /api/ai/extract-questions {transcript}
    B->>AI: POST https://api.nvidia.com/... 
    AI-->>B: [{question, options, correctIndex}]
    B-->>A: Questions with aiPowered: true
    A->>S: Display interactive quiz
```

---

## 10. Key Design Decisions & Trade-offs

| Decision | Choice Made | Trade-off |
|----------|-------------|-----------|
| **Monolith vs Microservices** | Hybrid — main monolith + payments microservice | Simplicity for core + isolation for payment sensitivity |
| **Session Management** | Stateless JWT (no server sessions) | Scalable horizontally, but token revocation is harder |
| **ORM Strategy** | Spring Data JPA + Hibernate | Rapid development, but N+1 queries risk on deep associations |
| **File Storage** | Local `/uploads` directory | Simple to start, not horizontally scalable (should move to S3) |
| **AI Reliability** | Graceful fallback from NVIDIA NIM to curated Q&A | Always works even if AI API is down |
| **WebSocket Auth** | Custom interceptor (not HTTP session) | Stateless consistency, slightly more complex setup |
| **Frontend State** | BehaviorSubjects (RxJS) | Reactive, simpler than NgRx but less powerful for complex state |
| **Database** | PostgreSQL (single DB) | ACID compliance, relational integrity for all 36 entities |

---

## 11. Scalability Bottlenecks & Future Improvements

### Current Limitations
| Area | Issue | Solution |
|------|-------|----------|
| **File Storage** | Local disk `/uploads` — not shareable across instances | Migrate to AWS S3 / GCS |
| **Leaderboard** | Computed synchronously after test submit | Move to async event-driven (Kafka/RabbitMQ) |
| **Session state** | Single backend instance for WS connections | Add Redis pub/sub for multi-instance WS |
| **AI Calls** | Synchronous HTTP call to NVIDIA NIM blocks thread | Use async `@Async` or reactive WebClient |
| **DB connections** | Default HikariCP pool | Tune pool size for production load |

### Recommended Next Steps
1. 🗃️ Add **Redis** for caching hot data (course lists, leaderboards)
2. 📨 Add **async messaging** (Kafka) for leaderboard recalculation
3. ☁️ Migrate file uploads to **AWS S3**
4. 🔄 Add **token refresh** endpoint and silent rotation in Angular
5. 📊 Add **Prometheus + Grafana** for observability

---

## 12. Tech Stack Summary

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend Framework | Angular (Standalone Components) | 19+ |
| Frontend Language | TypeScript | Latest |
| Frontend Styling | SCSS (Glassmorphic UI) | - |
| Backend Framework | Spring Boot | 3.3.5 |
| Backend Language | Java | 17/21 |
| ORM | Spring Data JPA + Hibernate | - |
| Database | PostgreSQL | - |
| Auth | JWT (jjwt library) | - |
| Password Hashing | BCrypt | - |
| Real-time | WebSocket (STOMP over SockJS) | - |
| AI | NVIDIA NIM LLM API | External |
| Payments | Razorpay SDK | External |
| Video Import | YouTube Data API | External |
| Build Tool (BE) | Maven | - |
| Build Tool (FE) | Angular CLI / npm | - |
