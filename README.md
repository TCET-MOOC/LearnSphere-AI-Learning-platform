# LearnSphere — Project Overview

> **Version:** 1.0 (Ideation / Pre-Development)
> **Stack:** Java Spring Boot (Backend) · Angular (Frontend)
> **Last Updated:** June 2026

---

## Table of Contents

1. [What is LearnSphere?](#1-what-is-learnsphere)
2. [Business Model](#2-business-model)
3. [User Roles](#3-user-roles)
4. [Feature Breakdown by Role](#4-feature-breakdown-by-role)
   - [4.1 Student](#41-student)
   - [4.2 Teacher](#42-teacher)
   - [4.3 Admin](#43-admin)
5. [AI & Automation Features](#5-ai--automation-features)
6. [Tech Stack](#6-tech-stack)
7. [UI / UX Design Principles](#7-ui--ux-design-principles)
8. [Page & Route Structure](#8-page--route-structure)
9. [Key Workflows](#9-key-workflows)
10. [Future Roadmap](#10-future-roadmap)

---

## 1. What is LearnSphere?

LearnSphere is a college-first e-learning platform where faculty upload video lectures, notes, quizzes, and assessments for their students. It combines the best of **Udemy** (marketplace model), **NPTEL/SWAYAM** (academic credentialing), and an **LMS** (Learning Management System) — built specifically for Indian engineering and higher-education colleges.

### Core Idea

- A **teacher** from any department uploads course content to the platform.
- **Students from the same college** can access all courses for **free** — regardless of department. An IT student can watch a Mechanical Engineering course, and vice versa.
- **Students from other colleges** can purchase individual courses just like Udemy.
- **Remedial / supplementary / credit-short students** are assigned specific courses whose completion (video + certification exam) counts toward their attendance or credit fulfilment.
- The **college earns revenue** from external student purchases and institutional subscriptions, while **teachers earn royalties** per course — giving both parties a financial and reputational incentive.

---

## 2. Business Model

### Revenue Streams

| Stream | Description | Who Pays |
|---|---|---|
| **External course purchase** | Students from other colleges buy individual courses | External students |
| **College institutional subscription** | College pays a platform fee to give all students free access | College / institution |
| **Remedial certification fee** | Students with attendance shortfall or supplementary exams pay a fee to access remedial cert courses | Student (or college-sponsored) |

### Revenue Split

- Platform retains **20%** of all external sales as a service fee.
- The remaining **80%** is split as **royalty to the teacher** who created the course.
- College earns a **branding and PR benefit** — courses sold to external students act as free marketing for the institution.
- Teachers are paid **monthly** via bank transfer once their balance crosses a minimum threshold (₹500).

### Value Proposition

| Stakeholder | Benefit |
|---|---|
| **Student (same college)** | Free access to all departments' knowledge, remedial credit fulfilment, certifications |
| **Student (external)** | Affordable high-quality academic content from verified faculty |
| **Teacher** | Passive royalty income, wider reach, platform analytics on their content |
| **College** | New revenue stream, free PR via external visibility, better student outcomes |
| **Platform (LearnSphere)** | 20% platform cut, data, and growth via college network effect |

---

## 3. User Roles

LearnSphere has **three primary roles**. Each role has a dedicated dashboard, sidebar navigation, and a distinct set of permissions.

### 3.1 Student
A registered learner affiliated with a college. Can be:
- **Internal student** — enrolled at the same college as the teacher (free access to all courses).
- **External student** — from a different college (must purchase courses).
- **Remedial student** — flagged by their college for attendance/credit issues; required to complete specific courses and certification exams.

### 3.2 Teacher
A verified faculty member affiliated with a registered college. Uploads and manages course content, interacts with students, and earns royalties from course sales.

### 3.3 Admin
A platform-level super user who oversees all colleges, users, courses, revenue, payouts, and content moderation. Has access to AI-powered analytics and moderation tools.

---

## 4. Feature Breakdown by Role

---

### 4.1 Student

The student dashboard has **10 sections** accessible via a left icon sidebar.

#### Dashboard (Home)
- Personalised greeting with the current date
- **4 KPI stat cards:** enrolled courses, attendance score, leaderboard rank, certificates earned
- **Continue learning** panel — shows in-progress courses with individual progress bars and lecture counters
- **Pending assessments** summary with due dates and urgency indicators
- **Recent notifications** feed
- **AI-recommended courses** — suggested based on the student's department, history, and platform trends

#### My Courses
- Filter tabs: All · In Progress · Completed · Not Started · Explore
- **Overall completion progress bar** across all enrolled courses
- Full enrolled course list with per-course progress bars, lecture counts, and completion status
- **Explore more courses** section — shows free cross-department courses available to enroll in (same-college courses shown free; external courses show a price)
- Enroll button per course (single click for free, payment flow for paid)

#### Video Lectures
- **Active video player** with:
  - Scrubber / seek bar with current timestamp and total duration
  - Playback speed control (0.5x – 2x)
  - Subtitle / closed caption toggle
  - Video quality selector (360p / 720p / 1080p / Auto)
  - Side notes panel toggle (opens an in-lecture notes drawer)
  - Bookmark this moment button (saves timestamp to Bookmarks)
  - Download lecture button (if enabled by teacher)
- **Lecture grid** for the selected course showing all lectures with status: Done / Playing Now / Locked (sequential unlock) / Not started
- **Per-lecture comment/discussion section** below the player — students can post questions and see teacher/peer replies without leaving the video page

#### My Notes
- Filter tabs by course
- **New note** button — opens a note editor linked to the current lecture and timestamp
- **Timestamped notes** — each note shows the course, lecture name, and the exact playback timestamp it was created at (e.g. ⏱ 22:14), with a click-to-resume link
- **Tag system** — students can add custom tags (e.g. #formula, #important, #exam)
- Inline **edit and delete** per note
- **Export options:** Export all as PDF · Export by course · Copy to clipboard

#### Assessments
- Filter tabs: All · Pending · Completed · Remedial
- **3 stat cards:** total done, pending count, highest score
- **Pending assessments list** with:
  - Quiz title, course name, question count, time limit, due date
  - Urgency pill (Urgent / Upcoming / Remedial)
  - **Start Now** button
- **Live quiz interface** — multiple choice questions with timer, option selection, and Next button
- **Completed assessments history** with scores, dates, and a review option
- **Remedial certification** card — specific exam unlocking attendance credit, with prominent CTA

#### Bookmarks
- Filter tabs: All · Lectures · Notes · Resources
- **Card grid layout** showing:
  - Saved lecture moments (with thumbnail emoji, course, lecture name, timestamp, and Resume button)
  - Saved notes (with open/edit link)
  - Downloaded resources and PDFs (with download link)
- Remove bookmark option per item

#### Leaderboard
- Filter tabs: My Batch · By Course · All College · Monthly
- **3 stat cards:** current rank, total points, points needed to reach next rank
- **Ranked list** — shows top 3 with medal icons, then a gap indicator, then the logged-in student's row (highlighted in brand purple), then one rank below
- **Points breakdown panel** — explains how points are earned:
  - Complete a lecture → +10 pts
  - Submit an assessment → +25 pts
  - Score above 90% → +50 bonus pts
  - Helpful discussion post → +5 pts

#### Discussion
- Filter tabs by course plus "My Posts" tab
- **New post** button
- **Thread cards** showing:
  - Author avatar, name, course, lecture reference, time ago
  - Post content
  - Unanswered / Answered / Pinned status pill
  - Teacher reply shown indented below (with a left border highlight)
  - Like count, reply count, Reply button
- **Compose box** at the bottom of the feed

#### Certificates
- **3 stat cards:** earned, in progress, remedial pending
- **Earned certificates grid** — each card shows:
  - Course name, issue date, teacher name
  - Download PDF button
  - Share button (LinkedIn / copy link)
- **In-progress certificates** — shows completion bar, remaining lectures needed, and the final exam requirement
- **Remedial certification** row — with Attempt Now CTA and attendance impact note

#### Settings
- **Profile section:** full name, college email (verified badge), password change, profile photo upload
- **Notification toggles:** new lecture uploaded, assessment due reminder, teacher reply, rank change, promotional announcements
- **Preferences:** default video quality, default playback speed, leaderboard visibility (opt-in/out), language
- **Account:** linked payment method (for external course purchases), download my data, sign out

---

### 4.2 Teacher

The teacher dashboard has **11 sections** accessible via a left icon sidebar, with a persistent **Upload Lecture** CTA button in the top bar.

#### Dashboard (Home)
- **4 KPI stat cards:** total students, active courses, average course rating, royalty earned this month
- **My courses** panel — shows each course with student count, rating, and live/draft/pending status
- **Recent student messages** — inbox preview with unread indicators and sender name/preview
- **Quick action buttons:** Upload lecture · Schedule session · Add quiz · Post announcement

#### My Courses
- Filter tabs: All · Live · Draft · Pending · Archived
- Per-course expanded view showing:
  - Enrolled student count, rating, completion rate
  - **Lecture list** with individual publish/draft/processing status and Edit button per lecture
  - Action buttons: Add lecture · Add quiz · Upload notes/PDF · Edit course info · Archive course
- **Draft course card** with completion progress bar, minimum lecture count requirement, and Submit for Review button
- **New Course** button at the top

#### Upload Content
- Tab switcher: Video lecture · Notes/PDF · Quiz/Assessment · Assignment · Resources
- **Video upload zone** — drag-and-drop or file browse (MP4/MOV/AVI, max 2GB)
- **Form fields:** select course, lecture number, lecture title, description
- **AI question extraction toggle** — when enabled, the platform automatically extracts potential quiz questions from the lecture transcript; teacher reviews and approves before they go live
- Action buttons: Upload & Publish · Save as Draft · Schedule Publish
- **Recent uploads** panel — shows last 3 uploads with processing / live status

#### Student Standings
- Filter tabs by course and an At-risk filter
- **3 stat cards:** enrolled, avg. completion, at-risk count
- **Top performers grid** — top 3 students shown as cards with avatar, rank, score %, and lecture count with progress bar
- **Full standings list** — paginated, shows rank, avatar, name, lectures watched, score
- **At-risk students panel** — students below 40% progress or inactive 14+ days, with a **Send Nudge** button (triggers a system notification to the student)

#### Messages
- **Two-panel chat layout:**
  - Left panel: conversation list with student name, message preview, unread dot
  - Right panel: active conversation with full message thread (sent right, received left bubbles), student info header with View Profile and Course Progress buttons, and a reply compose box with Send button

#### Discussion (Moderation)
- Filter tabs by course plus Flagged and Unanswered tabs
- **3 stat cards:** total posts, unanswered count, flagged count
- Per-post action buttons: **Reply · Pin · Mark Resolved · Flag/Remove**
- **AI-flagged posts** shown with red border, hidden content notice, and AI confidence score
- Teacher's own replies shown indented below the original question
- Edit Reply and Mark Resolved options on answered posts

#### Live Session
- **3 stat cards:** sessions this month, next scheduled session date, registrations
- **Schedule new session form:** course selector, session title, date, time, duration, platform (Built-in room / Google Meet link)
- Action buttons: Schedule & Notify Students · Save Draft
- **Upcoming sessions list** — with registered count, edit button, and **Start Now** button
- **Past sessions & recordings** — with attendee count, recording availability, and Share Recording button

#### Trending
- **3 stat cards:** teacher's best platform rank, total platform courses, teacher's avg. rating vs platform avg.
- **Platform-wide trending courses list** — ranked, with teacher's own courses highlighted in brand purple
- **Student sentiment panel for your courses** — bar charts showing positive/neutral/negative % per course
- **Top student comments** — best and critical recent reviews displayed

#### Royalties
- **4 stat cards:** this month earnings, total earned since joining, next payout date, external enrollments count
- **Earnings by course table** — per course, shows enrolled count, external paid count, and monthly earnings
- **Payout history** — each past month with amount and transfer date; current month shown as pending
- **Royalty breakdown bars** — external sales % / college share % / remedial certs %
- Platform fee disclosure note (20% retained)

#### Settings
- **Profile:** name, college email (verified), department, bio/intro (shown on public teacher profile), linked bank account for payouts
- **Notification toggles:** new enrollment, student message, new discussion post, course approved/rejected by admin, payout processed, AI flag on course
- **Course defaults:** AI question extraction on/off, auto-notify students on new lecture, allow discussion per lecture, allow DMs from students
- **Account:** password change, download my data, sign out

---

### 4.3 Admin

The admin dashboard has **10 sections** accessible via a dark-themed left icon sidebar (visually distinct from student/teacher interfaces).

#### Dashboard (Home)
- **4 KPI stat cards:** total revenue, registered users, active courses, open flags
- **Top teachers this month** — top 3 by student count, rating, and royalty earned
- **Top students this month** — top 3 by points
- **Quick action buttons:** Review Courses · Manage Users · Process Payouts · Review Flags
- **Platform health snapshot:** 4-box grid showing positive sentiment %, course approval rate, avg. student completion, open flag count

#### User Management
- Filter tabs: All users · Students · Teachers · Flagged · Blacklisted
- **4 stat cards:** total users, student count, teacher count, blacklisted count
- **Searchable and filterable user table** with:
  - Avatar, name, email, role, department
  - Status pill (Active / Flagged / Blacklisted)
  - Per-row actions: View · Warn · Blacklist (for students) · Suspend (for teachers) · Restore (for blacklisted)
- **User activity breakdown bars:** daily active %, weekly active %, inactive 30d+ %

#### Course Approvals
- Filter tabs: Pending · Approved · Rejected · All Courses
- **3 stat cards:** pending count, approved this month, total live courses
- **Pending review list** — each entry shows course name, teacher, department, lecture count, submission date, with Preview · Approve · Reject buttons
- **Recently approved courses** list with enrollment counts
- **Rejected courses** list with rejection reason noted

#### College Management
- **3 stat cards:** registered colleges, total students, total teachers
- **Registered colleges list** — each entry shows college name, city, student/teacher/course counts, verification status
  - Verified colleges: Manage button
  - Pending colleges: Verify + Reject buttons
- **Teacher affiliation requests** — teachers claiming a college affiliation must have their ID reviewed before being approved; Approve / Reject per request
- **Add College** button at top

#### Revenue
- Filter tabs: This Month · Last 3 Months · This Year · By College · By Course
- **4 stat cards:** total revenue, external paid revenue, college subscription revenue, remedial fees revenue
- **Monthly revenue trend bar chart** — visual month-over-month bars
- **Revenue by source** bar chart (external / college sub / remedial)
- **Top earning courses list** with per-course enrollment and paid student counts

#### Payouts
- **3 stat cards:** total pending payouts, paid this month, platform cut retained
- **Process All Pending** button at top
- **Pending payout list** — each teacher shows name, bank account (masked), course count, amount due, and individual Pay Now button
- **Payout history table** — monthly entries with total amount paid and teacher count; CSV export

#### Reports
- Filter tabs: Engagement · Growth · Course Quality · Revenue
- **4 engagement KPI cards:** avg. session time, videos watched, quizzes attempted, new enrollments
- **Engagement by department** bar chart
- **Platform growth grid** — new students, new teachers, external paid users, certificates issued (with % change vs last month)
- **Scheduled reports panel** — manage auto-generated weekly/monthly email reports sent to admin

#### Flagged Content
- Filter tabs: All · High Risk · Bullying · Spam · Suspicious
- **3 stat cards:** open flags, resolved this month, AI detection accuracy %
- **Flagged item cards** — each shows:
  - User name, content type (comment / DM / review), course, time
  - Risk level pill (High Risk / Medium / Spam)
  - AI confidence score and prior warning count
  - Content hidden by default for high-risk items
  - Actions: Blacklist User · Issue Warning · View Full Post · Dismiss
- High-risk items visually highlighted with red border

#### Sentiment Analysis
- Filter tabs: Platform-wide · By Course · By Teacher · By College
- **3 stat cards:** comments analysed, AI confidence %, negative trend direction
- **3-box overall sentiment score:** Positive % · Neutral % · Negative %
- **Sentiment by course type** bars (Math/Science, Engineering, CS/IT, Remedial)
- **Top negative keywords** with occurrence counts (e.g. "difficult", "confusing", "audio issue")
- **AI action recommendations** — e.g. "Audio quality spike in Thermodynamics Lec 4–6 → Notify teacher"
- **Teacher sentiment scores** table — per-teacher positive % with trend notes

#### Settings
- **Admin profile:** name, email (verified), 2FA status (enforced), password change
- **Platform toggles:**
  - AI abuse detection on/off
  - AI question extraction on/off
  - Sentiment analysis on/off
  - Course approval required before going live
  - Teacher ID verification required
  - External student purchases enabled/disabled
  - Leaderboard public visibility
- **Payout configuration:** platform royalty cut %, payout schedule date, minimum payout threshold
- **Notification preferences:** high-risk flag alerts, new college application, course submitted for review, weekly digest
- **Account tools:** audit log (all admin actions), manage admin accounts (add/remove admins), sign out

---

## 5. AI & Automation Features

LearnSphere integrates AI in two planned phases.

### Phase 1 — MVP (Launch Ready)
| Feature | Description |
|---|---|
| **Rule-based chatbot** | Answers common student queries (course navigation, assessment rules, attendance queries) without human intervention |
| **Auto question extraction** | When a teacher uploads a video, AI processes the transcript and extracts candidate quiz questions; teacher reviews and approves before they go live |

### Phase 2 — Post-Launch
| Feature | Description |
|---|---|
| **Recommendation engine** | Suggests courses to students based on department, watch history, quiz scores, and platform trends |
| **Sentiment analysis** | Runs on all new comments, reviews, and discussion posts; scores positive/neutral/negative and surfaces keyword themes to admin |
| **Abuse & bullying detection** | Flags comments, posts, and DMs that contain harassment, bullying, or inappropriate content; assigns risk score and confidence level; triggers admin review |
| **At-risk student detection** | Identifies students with low progress, no recent activity, or failing quiz scores and surfaces them to the teacher for intervention |

---

## 6. Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java · Spring Boot · Spring Security · Spring Data JPA |
| **Frontend** | Angular · TypeScript · Angular Material / Custom design system |
| **Database** | MySQL / PostgreSQL (relational) |
| **Authentication** | JWT-based auth · Role-based access control (RBAC) — Student / Teacher / Admin |
| **Video storage** | Cloud storage (AWS S3 / Azure Blob) for video files |
| **Video streaming** | Adaptive bitrate streaming (HLS) |
| **Payments** | Razorpay (Indian payment gateway for external course purchases) |
| **AI/ML** | Python microservices (sentiment analysis, recommendation engine, abuse detection) consumed via REST API |
| **Email/Notifications** | SMTP / Firebase Cloud Messaging for in-app and email notifications |
| **Deployment** | Cloud (AWS / Azure) · Docker containers |

---

## 7. UI / UX Design Principles

- **Professional-modern aesthetic** — dark navy topbar for Admin, clean white/light-grey body for Student and Teacher, brand purple (#534AB7) as the primary accent
- **Icon-only sidebar** — collapsed by default with tooltip-on-hover labels; no clutter, maximum content area
- **Role-based visual differentiation** — Admin portal uses a dark sidebar (#12102e) to visually signal elevated privilege
- **Status pills** — consistent colour-coded pills across all roles: Green (Live/Active/Positive), Purple (In Progress/Your item), Amber (Draft/Upcoming/Warning), Red (Urgent/Flagged/Blacklisted), Grey (Inactive/Default)
- **3D icons and micro-animations** — used on the landing page hero and empty states; kept lightweight (CSS/Lottie, no heavy libraries)
- **Distraction-free video player** — clean, dark-background player with minimal chrome; notes panel slides in from the right without interrupting video
- **Responsive design** — mobile-first approach, functional on tablets and desktops
- **Accessibility** — ARIA labels on all icon buttons, keyboard navigable sidebar, sufficient colour contrast ratios

---

## 8. Page & Route Structure

```
/                          → Landing page (public)
/login                     → Role-based login
/register                  → Student / Teacher registration
/verify-college            → College email verification flow

/student/
  dashboard                → Home
  courses                  → My courses + explore
  courses/:id              → Course detail + lecture list
  courses/:id/lecture/:lid → Video player page
  notes                    → All notes
  assessments              → Assessment list
  assessments/:id          → Live quiz / exam
  bookmarks                → Saved items
  leaderboard              → Rankings
  discussion               → Forum
  certificates             → My certs
  settings                 → Profile & preferences

/teacher/
  dashboard                → Home
  courses                  → My courses
  courses/:id              → Course management
  upload                   → Upload content
  students                 → Student standings
  messages                 → 1-on-1 inbox
  discussion               → Forum moderation
  live                     → Live sessions
  trending                 → Platform trends
  royalties                → Earnings & payouts
  settings                 → Profile & preferences

/admin/
  dashboard                → Platform overview
  users                    → User management
  courses                  → Course approvals
  colleges                 → College management
  revenue                  → Revenue analytics
  payouts                  → Payout processing
  reports                  → Platform reports
  flagged                  → Flagged content
  sentiment                → Sentiment analysis
  settings                 → Platform configuration
```

---

## 9. Key Workflows

### Student — Remedial Completion Flow
```
Student flagged for attendance/credit shortage
  → Admin/College assigns remedial course
    → Student receives notification
      → Student watches all required lectures
        → Student attempts certification exam (30 Qs)
          → Pass (≥ 60%) → Certificate issued → Attendance credit unlocked
          → Fail → Student can re-attempt after 24h cooldown
```

### Teacher — Content Upload Flow
```
Teacher uploads video file (drag-and-drop)
  → Teacher fills: course, lecture number, title, description
    → AI processes transcript → Extracts candidate quiz questions
      → Teacher reviews & approves/edits questions
        → Teacher chooses: Publish Now / Save Draft / Schedule
          → If Publish: Admin approval check (if course is new)
            → Approved → All enrolled students notified
```

### External Student — Purchase Flow
```
External student browses platform (no login required for previews)
  → Student clicks Enroll on a paid course
    → Login / Register prompt
      → Email verified as non-college domain → External tag assigned
        → Razorpay payment checkout
          → Payment success → Full course access granted
            → Student tracked in Teacher's external enrollment count
              → Royalty attributed to teacher at month-end payout
```

### Admin — Flag Resolution Flow
```
AI detects offensive/suspicious content (comment / DM / post)
  → Content auto-hidden (high risk) or flagged for review (medium)
    → Admin receives instant notification (high risk) or daily digest (medium)
      → Admin reviews flag with AI confidence score and context
        → Actions available:
            Blacklist user (permanent ban)
            Issue warning (notification sent to user, count tracked)
            Remove content only
            Dismiss (false positive)
```

---

## 10. Future Roadmap

| Phase | Feature |
|---|---|
| **Phase 1 (MVP)** | Student/Teacher/Admin dashboards, video upload & streaming, assessments, leaderboard, discussion, certificates, basic notifications, Razorpay payments, rule-based chatbot, AI question extraction |
| **Phase 2** | Recommendation engine, sentiment analysis, abuse detection, at-risk student alerts, live session (built-in video room), mobile app (iOS + Android) |
| **Phase 3** | Multi-language support, advanced analytics (heatmaps on video engagement), integration with college ERP systems for auto attendance sync, API for third-party college LMS integration |
| **Phase 4** | White-label option (colleges host their own branded LearnSphere instance), public course marketplace open to independent educators (not just college faculty) |

---

## Notes for Developers

- All API endpoints must be **role-protected** using Spring Security. A student JWT must never be able to access teacher or admin endpoints.
- The `college_id` on a user's profile is the key to determining **free vs. paid access**. Cross-college access is always paid unless explicitly overridden by admin.
- Video files are **never served directly** from the backend. All video URLs are signed, time-limited CDN URLs.
- The **remedial flag** on a student's course enrollment record is set by the college admin or the platform admin — teachers cannot set it directly.
- All **AI features** are consumed as internal microservices via REST; the main Spring Boot backend should not contain ML logic directly.
- **Leaderboard points** are computed server-side and should not be client-modifiable. All point events (lecture completion, quiz submission, discussion upvote) are logged in a separate `point_events` table for auditability.

---
# Contributors
<a href="https://github.com/TCET-MOOC/LearnSphere-AI-Learning-platform/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=TCET-MOOC/LearnSphere-AI-Learning-platform" />
</a>

---

*This document is the single source of truth for the LearnSphere project during ideation and early development. Update this file as features evolve.*
