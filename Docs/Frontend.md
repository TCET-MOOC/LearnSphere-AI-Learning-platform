# LearnSphere Frontend Documentation

## Overview

The LearnSphere client is an **Angular 19** Single-Page Application (SPA) providing dedicated portals for **Students**, **Teachers**, and **Administrators**, designed with modern design principles (Plus Jakarta Sans + Inter typography, tabular numerals, glassmorphic pulse skeletons, and custom Lucide icons).

---

## Directory Structure

Located in `learnSphere/frontend/learnsphere/src/app`:

```
src/app/
├── core/             # Singleton services, JWT interceptor, error interceptor, models
├── features/
│   ├── admin/        # 14 Admin Governance views (Dashboard, Revenue, Reports, Payouts, etc.)
│   ├── student/      # Student views (Dashboard, Courses, Lecture Player, Leaderboard, Notes, etc.)
│   ├── teacher/      # Teacher views (Course Management, Lecture Upload, Students, Trending, etc.)
│   ├── auth/         # Login, Registration, Password Reset views
│   └── landing/      # Public Landing Page with interactive course catalogue
├── shared/           # Reusable components (Sidebar, Topbar, VideoPlayer, AiChatbot, Modals)
├── environments/     # Environment configurations for API endpoints
├── app.config.ts     # Standalone application providers & Lucide icon registry
└── app.routes.ts     # Role-based route definitions with AuthGuard and RoleGuard
```

---

## Key Capabilities

1. **Role-Based Workspaces**:
   - `/student/*`: Enrolled courses, video player with note bookmarks, Academic XP leaderboard, remedial assessments.
   - `/teacher/*`: YouTube playlist ingestion, course management, video uploads, student progress monitoring, royalty dashboard.
   - `/admin/*`: Executive command desk with gold/silver/bronze medals, revenue analytics, faculty payouts, content moderation.

2. **Custom Video Player & Bookmarking**:
   - Custom playback controls, speed selector, fullscreen toggle, note timestamps, and bookmark markers integrated with course curriculum.

3. **Interactive Floating AI Assistant**:
   - `AiChatbotComponent` accessible across all views for on-demand tutoring and answering academic queries.

4. **Design System & Aesthetics**:
   - Distinctive typography using `Plus Jakarta Sans` for headers, `Inter` for body, and `font-feature-settings: 'tnum'` for financial and numerical tables.
   - Fully responsive across desktop, tablet, and mobile breakpoints.
