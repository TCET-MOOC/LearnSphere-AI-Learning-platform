# LearnSphere Roadmap & Future Plans

This document catalogs completed milestones, active integrations, and the deferred enterprise scaling roadmap for the **LearnSphere AI Platform**.

---

## 🎯 Current Strategy & Architecture Decisions

1. **Video Storage & Streaming Architecture (✅ Completed)**:
   - **Video Storage**: Standardized on **YouTube Data API v3** for video content delivery, playlist ingestion, and zero-cost cloud CDN hosting (`YouTubeService` / `YouTubeController`).
   - **Live Video Streaming**: Standardized on **YouTube Data API v3** for live broadcast sessions, custom player embeds, and live chat sync.
   - Custom in-browser video player with speed controls, timestamp bookmarking, lecture tracking, and fullscreen mode.

2. **Payment Processing & Financial Settlement (✅ Completed)**:
   - **Payment Gateway**: Integrated **Razorpay Test Gateway** (`PaymentController`, `learnsphere-payments-service/` on port 8081).
   - Automated checkout flow, payment verification, and 70% Faculty / 30% Platform royalty split calculation and disbursement ledger.

3. **AI & Intelligence Tier (✅ Completed)**:
   - **NVIDIA NIM LLM API** (`meta/llama-3.1-70b-instruct`) powers automated quiz generation, remedial assistance, lecture summarization, and interactive tutoring via `NvidiaNimService`.
   - Floating AI assistant widget (`AiChatbotComponent`) active across student, teacher, and admin portals.

4. **Dual Spring Boot Architecture (✅ Completed)**:
   - **Main Backend Service** (`learnsphere-backend/`, Port 8080): Authentication, course catalog, student progress, quiz submissions, sentiment telemetry, and institutional administration.
   - **Payments Microservice** (`learnsphere-payments-service/`, Port 8081): Checkout sessions, Razorpay test gateway integration, 70/30 faculty split calculation, and disbursement ledger.

---

## 📊 Roadmap Status Matrix

| Capability | Category | Status | Details |
|---|---|:---:|---|
| **Video Storage** | Storage & CDN | ✅ **Completed** | **YouTube Data API v3** (Playlist/video hosting & global CDN delivery). |
| **Live Video Streaming** | Live Delivery | ✅ **Completed** | **YouTube Data API v3** (Live broadcast streams & custom player embed). |
| **Payment Gateway** | Fintech | ✅ **Completed** | **Razorpay Test Gateway** with 70% Faculty / 30% Platform split ledger. |
| **NVIDIA NIM AI Tutor & Quiz Generator** | Artificial Intelligence | ✅ **Completed** | Production LLM inference for quizzes, remediation, and Q&A. |
| **Academic XP & Leaderboards** | Gamification | ✅ **Completed** | Multi-scope rankings (Global, College, Course) with podium medals. |
| **Admin Executive Command Suite** | Governance | ✅ **Completed** | 14 routes: Revenue ledger, reports, moderation queue, user governance. |
| **Payments Microservice** | Microservice | ✅ **Completed** | Standalone service on port 8081 with automated royalty ledger. |
| **Transactional Email / SMTP Gateway** | Notifications | ⏳ **Deferred / Remaining** | SendGrid / AWS SES for password resets and weekly digests. |
| **Cloud Storage for PDFs/Docs** | Document Storage | ⏳ **Deferred / Remaining** | AWS S3 pre-signed upload URLs for handouts and assignment attachments. |
| **Fine-Tuned NLP Sentiment Classifier** | AI / Moderation | ⏳ **Deferred / Remaining** | Upgrading keyword lexicon to custom transformer classifier. |
| **Automated At-Risk Intervention Worker** | Student Analytics | 🚀 **Future Scope** | Cron-based background worker analyzing <40% scores to trigger alerts. |
| **In-Browser Interactive Coding Sandbox** | Hands-On Learning | 🚀 **Future Scope** | Isolated Docker execution runners for Python/Java programming labs. |
| **College ERP / LMS (LTI 1.3) Integration** | Enterprise | 🚀 **Future Scope** | Canvas/Moodle sync and automatic university ERP attendance export. |
| **Native Mobile Apps (iOS & Android)** | Mobile Delivery | 🚀 **Future Scope** | Flutter / React Native clients utilizing existing REST APIs. |

---

## ⏳ Detailed Breakdown of Remaining Integrations

### 1. Transactional Email & Messaging Gateway (Remaining)
- **Current State**: Password resets and system alerts are handled in-app and via JSON payloads.
- **Target Integration**: **Spring Boot Mail (SMTP)** / **SendGrid API** / **AWS SES**.
- **Future Scope**:
  - Secure password reset emails with cryptographically signed, expiring reset links.
  - Double opt-in user registration with email verification tokens.
  - Automated course enrollment confirmations and assignment deadline reminders.

### 2. Cloud Storage for PDFs & Attachments (Remaining)
- **Current State**: Course video storage is handled by YouTube Data API v3. Non-video attachments, profile images, and handouts are stored on the local disk (`uploads/` directory).
- **Target Integration**: **AWS S3** / **Cloudinary** / **Google Cloud Storage**.
- **Future Scope**:
  - Pre-signed S3 upload URLs for direct client-to-cloud PDF/handout uploads.
  - Automated virus/malware scanning on all uploaded PDFs and documents.
  - CDN distribution (CloudFront) for document and avatar delivery.
