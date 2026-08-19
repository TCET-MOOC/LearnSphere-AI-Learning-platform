# LearnSphere Future Plans & Roadmap

This document catalogs the future architectural plans, deferred production integrations, and scaling roadmap for the **LearnSphere AI Platform**.

---

## 🎯 Current Strategy & Architecture Decisions

1. **Video Streaming Architecture**:
   - The platform has standardized on **YouTube Data API v3** for video content delivery. Videos are uploaded as unlisted or public YouTube video assets and streamed through the integrated custom video player.
   - Self-hosted raw video processing pipelines (FFmpeg / HLS transcoding) and direct cloud video hosting are deferred in favor of YouTube's robust, zero-cost CDN and global streaming infrastructure.

2. **AI & Intelligence Tier**:
   - Assessment question extraction, lecture summarization, and interactive tutoring are powered by **NVIDIA NIM** LLM APIs (`meta/llama-3.1-70b-instruct`) with structured JSON validation and resilient fallback heuristics.

---

## ⏳ Deferred Features & Production Integrations

The following enterprise capabilities are deliberately deferred to future platform iterations:

### 1. Real Payment Gateway & Automated Invoicing (Deferred)
- **Status**: Currently simulated via `PaymentController` / `CheckoutComponent`.
- **Target Integration**: **Razorpay** (India / INR) and **Stripe** (International).
- **Future Scope**:
  - Secure checkout sessions with server-side order creation.
  - Webhook endpoint (`/api/payment/webhook`) with HMAC-SHA256 signature verification.
  - Automated GST-compliant PDF invoice generation upon transaction capture.
  - Automated instructor royalty payout disbursement via Razorpay Route / Stripe Connect.
  - Refund processing and credit note issuance for disputed enrollments.

### 2. Transactional Email & Messaging Gateway (Deferred)
- **Status**: Password reset tokens and system alerts are handled in-app and via JSON payloads.
- **Target Integration**: **Spring Boot Mail (SMTP)** / **SendGrid API** / **AWS SES**.
- **Future Scope**:
  - Secure password reset emails with cryptographically signed, expiring reset links.
  - Double opt-in user registration with email verification tokens.
  - Automated course enrollment confirmations and assignment deadline reminders.
  - Weekly faculty digests summarizing student engagement and at-risk alerts.

### 3. Cloud Object Storage for Attachments & PDFs (Deferred)
- **Status**: Non-video attachments, profile images, and handouts are stored on the local disk (`uploads/` directory).
- **Target Integration**: **AWS S3** / **Cloudinary** / **Google Cloud Storage** / **MinIO**.
- **Future Scope**:
  - Pre-signed S3 upload URLs for direct client-to-cloud file uploads.
  - Automated virus/malware scanning on all uploaded PDFs and documents.
  - CDN distribution (CloudFront) for fast document and avatar delivery.

### 4. Custom Private Video Transcoding & HLS Streaming (Deferred)
- **Status**: Replaced by YouTube Data v3 unlisted video integration.
- **Target Integration**: **FFmpeg Worker Container** / **AWS MediaConvert**.
- **Future Scope**:
  - Support for air-gapped or proprietary institutional intranets that prohibit YouTube.
  - Multi-bitrate HLS (`.m3u8` / `.ts`) generation (1080p, 720p, 480p, 360p).
  - DRM encryption (Widevine / FairPlay) for high-value proprietary course content.

### 5. Native In-Browser Live Video Classrooms / WebRTC (Deferred)
- **Status**: Live sessions currently support external room links (Zoom, Google Meet, MS Teams).
- **Target Integration**: **WebRTC** / **Agora SDK** / **Jitsi Meet API**.
- **Future Scope**:
  - Built-in multi-party video conferencing directly in the LearnSphere web app.
  - Interactive digital whiteboard with collaborative drawing tools.
  - In-session polls, break-out rooms, and live student hand-raising queues.

---

## 🚀 Advanced AI & Analytics Roadmap

1. **Course Recommendation & Personalized Learning Paths**:
   - Embed student learning history, department requirements, and quiz performance into vector embeddings to generate personalized course feeds.
2. **Contextual Sentiment & Moderation NLP Model**:
   - Upgrade keyword heuristics to a fine-tuned Transformer model (or LLM classifier) to detect harassment, bullying, and distress in student forums and DMs.
3. **Automated "At-Risk" Student Detection**:
   - Automated Spring `@Scheduled` background worker analyzing quiz failures (<40%), lecture drop-offs, and inactivity to flag students for remedial intervention.
4. **AI Interactive Coding Sandbox**:
   - Integrated Monaco editor with isolated Docker code execution runners for Java, Python, and C++ programming assignments.

---

## 🏛️ Institutional & Scaling Roadmap

1. **College ERP & LMS Integrations**:
   - LTI 1.3 standard compliance for seamless integration with Canvas, Moodle, and Blackboard.
   - Automatic attendance synchronization with university ERP systems based on lecture completion.
2. **Multi-Tenant White-Labeling**:
   - Custom institutional branding (subdomains, color themes, university crests, and custom certificate templates).
3. **Mobile Applications**:
   - Native Flutter / React Native iOS & Android apps utilizing the existing REST APIs.
