# LearnSphere: YouTube Integration, Custom Live Chat & Automated Content Censorship Plan

> **Document Version:** 1.0  
> **Target Platform:** LearnSphere AI (Spring Boot Backend & Angular Frontend)  
> **Status:** Design & Implementation Blueprint  

---

## Table of Contents
1. [Executive Summary](#1-executive-summary)
2. [YouTube Data API v3 Architecture](#2-youtube-data-api-v3-architecture)
   - 2.1 [Zero-Cost Video Streaming & Adaptive Bitrate](#21-zero-cost-video-streaming--adaptive-bitrate)
   - 2.2 [1-Click Playlist & Course Importer](#22-1-click-playlist--course-importer)
   - 2.3 [Automatic Transcript Extraction for AI Question Extractor](#23-automatic-transcript-extraction-for-ai-question-extractor)
   - 2.4 [Platform-Exclusive Video Privacy (Hidden from Public YouTube)](#24-platform-exclusive-video-privacy-hidden-from-public-youtube)
   - 2.5 [Platform-Scoped Search (Courses & Verified Faculty Only)](#25-platform-scoped-search-courses--verified-faculty-only)
3. [Custom Live Stream Chat & Video Comment Engine](#3-custom-live-stream-chat--video-comment-engine)
   - 3.1 [Real-time Virtual Classroom Live Chat](#31-real-time-virtual-classroom-live-chat)
   - 3.2 [Timestamped Video Lecture Discussions](#32-timestamped-video-lecture-discussions)
4. [Automated Content Censorship & Anti-Bullying Pipeline](#4-automated-content-censorship--anti-bullying-pipeline)
   - 4.1 [Fast In-Memory Profanity & Harassment Censor](#41-fast-in-memory-profanity--harassment-censor)
   - 4.2 [Multi-Tier Moderation & 3-Strike Penalty System](#42-multi-tier-moderation--3-strike-penalty-system)
   - 4.3 [Admin Content Moderation Hub](#43-admin-content-moderation-hub)
5. [Database Schema Enhancements](#5-database-schema-enhancements)
6. [Phased Implementation Roadmap](#6-phased-implementation-roadmap)

---

## 1. Executive Summary

This architecture establishes a high-performance, cost-effective media and communication pipeline for **LearnSphere**:
1. **Leveraging YouTube as a Headless Streaming & Transcript Engine**: Eliminates expensive cloud video storage, transcoding, and CDN egress bills while keeping course videos **strictly private and exclusive to LearnSphere**.
2. **Platform-Scoped Educational Search**: Ensures search queries strictly resolve to LearnSphere courses, faculty profiles, and college-affiliated curriculum rather than arbitrary YouTube content.
3. **Independent Real-Time Communication**: Provides custom interactive live chat for live masterclasses and threaded timestamped comments below lecture videos.
4. **Automated Content Censorship & Moderation**: Masks abusive/bullying language in real time, blocks severe harassment, enforces automatic user timeouts, and feeds repeat violations into the Admin Moderation Queue.

---

## 2. YouTube Data API v3 Architecture

```
+---------------------------------------------------------------------------------------------------------------+
|                                                LearnSphere Platform                                           |
+---------------------------------------------------------------------------------------------------------------+
       |                                                 |                                              |
       v                                                 v                                              v
 [1-Click Course Importer]                     [Headless Video Player]                      [AI Transcript Pipeline]
 • Paste YouTube Playlist URL                  • Embedded via YouTube IFrame API            • Download Captions (.vtt)
 • Auto-extracts titles, durations,            • Configured: modestbranding=1, rel=0        • Feed into AI Question Extractor
   descriptions, and HD thumbnails             • Sequential watch enforcement (no skipping) • Power in-lecture keyword search
 • Generates ready-to-publish course           • Hidden from public YouTube search
```

### 2.1 Zero-Cost Video Streaming & Adaptive Bitrate
* **How it works**: Videos uploaded by faculty are hosted on YouTube and embedded via the YouTube IFrame API inside LearnSphere’s custom lecture player (`VideoPlayerComponent`).
* **Benefits**:
  - **Zero Bandwidth Bills**: Saves thousands of dollars in cloud egress (AWS S3/CloudFront) costs.
  - **Adaptive Bitrate Streaming**: Automatically delivers 360p, 480p, 720p, 1080p, and 4K streams based on the student's internet connection.
  - **Device Compatibility**: Native hardware-accelerated playback across all iOS, Android, and desktop browsers.

### 2.2 1-Click Playlist & Course Importer
* **Workflow**:
  1. Teacher enters a YouTube Playlist URL (e.g., `https://www.youtube.com/playlist?list=PL...`) into the Course Management studio.
  2. Spring Boot backend queries YouTube Data API `playlistItems.list`.
  3. Automatically creates the course curriculum with:
     - Ordered lecture numbers and titles
     - Clean descriptions and timestamps
     - High-resolution thumbnail image URLs
     - Parsed duration in seconds (`contentDetails.duration` parsed from ISO 8601 `PT15M32S` to `932s`)

### 2.3 Automatic Transcript Extraction for AI Question Extractor
* **Workflow**:
  1. For any uploaded/imported video, LearnSphere requests the closed caption track via the YouTube Captions API / Transcript endpoint.
  2. The raw text transcript is passed to the **AI Question Extractor**.
  3. The AI generates 4-option multiple choice questions (MCQs) with correct answer keys and explanations for instant course assessment creation.

### 2.4 Platform-Exclusive Video Privacy (Hidden from Public YouTube)
To ensure the course content remains an exclusive value proposition on LearnSphere and is not discoverable for free on YouTube:

| Security Layer | Implementation Detail |
| :--- | :--- |
| **Video Privacy Mode** | All course videos are uploaded with privacy status **`UNLISTED`**. They will **never appear in YouTube search results, user recommendations, or channel feeds**. |
| **Embedded Player Lockdown** | Configured with `modestbranding: 1` (hides prominent YouTube logo) and `rel: 0` (strictly prevents YouTube from recommending external random videos). |
| **Click Interception** | LearnSphere video player chrome sits over the iframe to prevent students from clicking through to external YouTube URLs. |
| **Enterprise Domain-Lock (Optional)** | For strict anti-piracy, videos can be uploaded to **Cloudflare Stream** or **Bunny.net Stream** with domain whitelisting allowing playback *only* on `*.learnsphere.edu`. |

### 2.5 Platform-Scoped Search (Courses & Verified Faculty Only)
* The platform search bar searches the **internal LearnSphere database index**:
  - **Course Title, Department, and Description** (`course.title`, `course.department`)
  - **Verified Faculty Profiles** (`user_account.full_name`, `user_account.department`)
  - **Curriculum Tags & Topics** (`#algorithms`, `#thermodynamics`, `#data-science`)
* If querying YouTube channels, the API request is strictly scoped with `channelId=YOUR_COLLEGE_CHANNEL_ID` so no unrelated external videos ever appear in student searches.

---

## 3. Custom Live Stream Chat & Video Comment Engine

LearnSphere operates its own native communication system, separate from YouTube's public comments.

```
+---------------------------------------------------------------------------------------------------------------+
|                                      LearnSphere Communication Channels                                       |
+---------------------------------------------------------------------------------------------------------------+
                                  /                                           \
                                 v                                             v
               [1. Live Classroom Chat]                           [2. Video Lecture Discussions]
               • Real-time peer interaction                       • Threaded discussion per video
               • Faculty badge & faculty pin                      • Linked to exact playback timestamps
               • Raise-hand question queue                        • "Mark Resolved" & "Teacher Answered"
               • Real-time profanity masking                      • AI-flagged toxic comment folding
```

### 3.1 Real-time Virtual Classroom Live Chat (`StudentLiveRoomComponent`)
* Located directly adjacent to the live video broadcast.
* **Faculty Controls**: Faculty can delete offensive messages, pin critical questions, and mute disruptive students with 1 click.
* **Student Features**: Real-time chat messages, dedicated Q&A filter tab, hand-raise notification to instructor, and live note-taking.

### 3.2 Timestamped Video Lecture Discussions (`LectureDiscussionComponent`)
* Threaded comment section below asynchronous video lectures.
* Every comment can link to an exact lecture timestamp (e.g., `⏱ 14:22`), allowing peers and teachers to click and jump directly to that moment in the video.
* Filter by: *All Comments*, *Unanswered Questions*, *Faculty Replied*, and *My Notes*.

---

## 4. Automated Content Censorship & Anti-Bullying Pipeline

To maintain a respectful, academic environment, all chat messages and comments pass through an automated censorship engine before public display.

```
+---------------------------------------------------------------------------------------------------------------+
|                             Student Submits Comment / Live Chat Message                                        |
+---------------------------------------------------------------------------------------------------------------+
                                                       |
                                                       v
                                  +------------------------------------------+
                                  |    Layer 1: Fast Sub-Millisecond Censor  |
                                  |    (Aho-Corasick & Leetspeak Trie)       |
                                  +------------------------------------------+
                                                       |
                    +----------------------------------+----------------------------------+
                    |                                                                     |
         [Academic / Clean Text]                                            [Abuse / Profanity Detected]
                    |                                                                     |
                    v                                                                     v
     +------------------------------+                                     +-------------------------------+
     | Broadcast Instantly to Room  |                                     | Layer 2: Severity Classifier  |
     | Saved as Status: CLEAN       |                                     +-------------------------------+
     +------------------------------+                                                     |
                                                     +------------------------------------+------------------------------------+
                                                     |                                                                         |
                                              [Mild Profanity]                                                  [Severe Bullying / Harassment]
                                                     |                                                                         |
                                                     v                                                                         v
                                    +----------------------------------+                      +----------------------------------+
                                    | • Mask text: "You are an ***"    |                      | • Message Blocked from Room      |
                                    | • Deliver censored version       |                      | • In-App Warning Toast           |
                                    | • User Strike Counter: +1        |                      | • Auto 15-Minute Chat Timeout    |
                                    +----------------------------------+                      | • Auto-Create Admin Flag Ticket  |
                                                                                              +----------------------------------+
                                                                                                               |
                                                                                                               v
                                                                                              +----------------------------------+
                                                                                              | Admin Moderation Hub             |
                                                                                              | (/admin/flagged)                 |
                                                                                              | • Blacklist / Suspend Offender   |
                                                                                              +----------------------------------+
```

### 4.1 Fast In-Memory Profanity & Harassment Censor
* **Performance**: Sub-2ms execution time so live broadcast chat never stutters.
* **Obfuscation & Leetspeak Normalizer**:
  - Catches character substitutions: `@` $\to$ `a`, `$` $\to$ `s`, `!` $\to$ `i`, `0` $\to$ `o`, `1` $\to$ `l`.
  - Catches spacing and punctuation tricks: `f.u.c.k`, `s-h-i-t`, `b i t c h`.
* **Output**: Generates `maskedText` replacing prohibited words with asterisks or `[censored]` while retaining message readability for mild occurrences.

### 4.2 Multi-Tier Moderation & 3-Strike Penalty System

| Strike Level | Trigger | Action Taken |
| :--- | :--- | :--- |
| **Strike 1 (Mild)** | Profanity or offensive slang | Message is masked (`***`). Student sees non-intrusive etiquette warning. Strike counter recorded. |
| **Strike 2 (Moderate)** | Direct insult or repeated profanity within 24h | Message blocked. Student placed on **15-minute chat timeout** (`chatMutedUntil = NOW() + 15m`). |
| **Strike 3 (Severe)** | Targeted bullying, harassment, hate speech, threats | Message blocked immediately. Account flagged with **HIGH RISK** in Admin Moderation Queue. Faculty & Admin notified. |

### 4.3 Admin Content Moderation Hub (`/admin/flagged`)
* Admins can inspect the **original message vs. masked message**, student's prior warning history, course context, and AI confidence score.
* **One-Click Administrative Actions**:
  - **Issue Formal Academic Warning**
  - **Mute Student from All Chats (7 Days)**
  - **Blacklist / Suspend User Account**
  - **Dismiss (False Positive)**

---

## 5. Database Schema Enhancements

```sql
-- 1. Video Lecture Discussions Moderation
ALTER TABLE discussion_post
  ADD COLUMN is_censored BOOLEAN DEFAULT FALSE,
  ADD COLUMN masked_body TEXT,
  ADD COLUMN toxicity_score DOUBLE PRECISION DEFAULT 0.0,
  ADD COLUMN flagged_reason VARCHAR(50);

-- 2. Native Live Stream Classroom Chat Messages
CREATE TABLE live_chat_message (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES live_session(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES user_account(user_id) ON DELETE CASCADE,
  raw_text TEXT NOT NULL,
  masked_text TEXT NOT NULL,
  is_blocked BOOLEAN DEFAULT FALSE,
  is_teacher_message BOOLEAN DEFAULT FALSE,
  is_question BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Student Penalty & Timeout Tracking
ALTER TABLE user_account
  ADD COLUMN warning_count INT DEFAULT 0,
  ADD COLUMN chat_muted_until TIMESTAMP WITH TIME ZONE;
```

---

## 6. Phased Implementation Roadmap & Status Tracker

```
+---------------------------------------------------------------------------------------------------+
|                                Implementation Milestones Status                                    |
+---------------------------------------------------------------------------------------------------+
  [x] Phase 1: Fast Censorship & Content Filter Engine (COMPLETED)
      • Built ProfanityFilterService & ProfanityFilterServiceImpl with leetspeak/symbol normalization.
      • Integrated real-time censorship & auto-flagging into DiscussionServiceImpl (video comments).
      • Created /api/censor endpoint in CensorController.

  [x] Phase 2: YouTube API Importer & Headless Player Integration (COMPLETED)
      • Built YouTubeService & YouTubeServiceImpl for 1-click playlist parsing (extracts titles, 
        durations, high-res thumbnails, and transcript extraction).
      • Created YouTubeController (/api/teacher/youtube/import-playlist & /transcript/{videoId}).
      • Implemented 1-Click YouTube Playlist Importer Modal in CourseManagementComponent.
      • Configured privacy embed parameters (modestbranding=1, rel=0, iv_load_policy=3).

  [x] Phase 3: Live Chat Real-Time Filtering & 15-Minute Timeout Engine (COMPLETED)
      • Integrated real-time /api/censor verification in StudentLiveRoomComponent.
      • Implemented 2-strike penalty logic with 15-minute student chat timeout (chatMutedUntil).
      • Enforced in-classroom etiquette warning notifications.

  [x] Phase 4: Admin Moderation Hub & Disciplinary Actions (COMPLETED)
      • Connected AdminFlaggedComponent for inspecting auto-flagged discussion and chat incidents.
      • Supported filtering by High Risk, Bullying, Spam, and Suspicious categories with one-click
        Resolve & Dismiss actions.
```

---

## 7. Verified Codebase Artifacts

| Component | File Path | Status |
| :--- | :--- | :--- |
| **Censor Service Interface** | [ProfanityFilterService.java](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/src/main/java/com/MOOC/OnlineLearningPlatfrom/Service/ProfanityFilterService.java) | `[x] Done` |
| **Censor Service Implementation** | [ProfanityFilterServiceImpl.java](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/src/main/java/com/MOOC/OnlineLearningPlatfrom/Service/impl/ProfanityFilterServiceImpl.java) | `[x] Done` |
| **Censor REST Endpoint** | [CensorController.java](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/src/main/java/com/MOOC/OnlineLearningPlatfrom/Controller/CensorController.java) | `[x] Done` |
| **Discussion Censorship** | [DiscussionServiceImpl.java](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/src/main/java/com/MOOC/OnlineLearningPlatfrom/Service/impl/DiscussionServiceImpl.java) | `[x] Done` |
| **YouTube Service Interface** | [YouTubeService.java](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/src/main/java/com/MOOC/OnlineLearningPlatfrom/Service/YouTubeService.java) | `[x] Done` |
| **YouTube Service Implementation** | [YouTubeServiceImpl.java](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/src/main/java/com/MOOC/OnlineLearningPlatfrom/Service/impl/YouTubeServiceImpl.java) | `[x] Done` |
| **YouTube REST Endpoint** | [YouTubeController.java](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/src/main/java/com/MOOC/OnlineLearningPlatfrom/Controller/YouTubeController.java) | `[x] Done` |
| **YouTube 1-Click UI Importer** | [course-management.component.html](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/learnSphere/frontend/learnsphere/src/app/features/teacher/courses/course-management/course-management.component.html) | `[x] Done` |
| **Live Chat Moderation** | [live-room.component.ts](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/learnSphere/frontend/learnsphere/src/app/features/student/live/live-room.component.ts) | `[x] Done` |
| **Admin Moderation Hub** | [flagged.component.ts](file:///c:/Users/singh/OneDrive/Desktop/LearnSphere-AI---An-Online-Learning-Platform/learnSphere/frontend/learnsphere/src/app/features/admin/flagged/flagged.component.ts) | `[x] Done` |

---

*All phases have been implemented, integrated, and verified with 100% successful backend and frontend builds.*
