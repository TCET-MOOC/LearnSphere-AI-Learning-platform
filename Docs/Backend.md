# LearnSphere Backend Documentation

## Overview

The backend comprises two decoupled **Java Spring Boot 3.3.5** services serving REST APIs and WebSockets to the Angular client.

---

## Services & Directories

### 1. Main Backend Service (`learnsphere-backend/`)
- **Port**: `8080` (Base URL: `http://localhost:8080/api`)
- **Core Responsibilities**:
  - **Authentication & Security**: Custom JWT filter (`JwtAuthFilter`), BCrypt password encoder, and role-based security filters (`STUDENT`, `TEACHER`, `ADMIN`).
  - **Curriculum & Ingestion**: YouTube Data API v3 playlist and video ingestion (`YouTubeController`, `CourseController`).
  - **Student Engagement**: Progress tracking, lecture bookmarks, note-taking, and assessment submissions (`StudentController`, `AssessmentController`).
  - **AI Tutoring & Quizzes**: NVIDIA NIM LLM API client (`NvidiaNimService`, `AiController`).
  - **Institutional Administration**: 14 admin management endpoints for revenue analytics, user governance, content moderation, and college affiliations (`UserAdminController`, `RevenueController`, `ReportsController`, `ModerationController`).
  - **Real-Time Communication**: WebSocket STOMP messaging and announcements (`WebSocketConfig`, `MessagingController`, `AnnouncementController`).

### 2. Payments Microservice (`learnsphere-payments-service/`)
- **Port**: `8081` (Base URL: `http://localhost:8081/api`)
- **Core Responsibilities**:
  - Course checkout session creation and transaction processing.
  - Automated 70% Faculty / 30% Platform royalty split calculation.
  - Faculty pending payout ledger and disbursement processing (`PayoutAdminController`).

---

## Tech Stack

- **Language & Runtime**: Java 17 / 21
- **Framework**: Spring Boot 3.3.5
- **ORM / Persistence**: Spring Data JPA / Hibernate 6.5
- **Database**: PostgreSQL
- **Security**: Spring Security + JJWT (0.11.5)
- **External APIs**: YouTube Data API v3, NVIDIA NIM (LLaMA 3.1 70B Instruct)
