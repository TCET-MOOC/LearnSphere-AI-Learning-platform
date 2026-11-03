# LearnSphere Frontend Documentation

> **Corrected 2026-11-03 (W7-01, ADR-06):** the frontend is Angular 21.2 (this document first said version 19); the toolchain section below is new. The rest of this document is the source revision frozen on 2026-10-22 (`Docs/source/MANIFEST.md`); where it differs from `Docs/source/spec.md` or the code, those win (ADR-01).

## Overview

The LearnSphere client is an **Angular 21.2** Single-Page Application (SPA) providing dedicated portals for **Students**, **Teachers**, and **Administrators**, designed with modern design principles (Plus Jakarta Sans + Inter typography, tabular numerals, glassmorphic pulse skeletons, and custom Lucide icons).

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

---

## Toolchain and upgrade policy

Added 2026-11-03 by W7-01 (`[G-F1]`; ADR-06 in `Docs/PLAN.md`).

| Tool | Version | Where it is pinned |
|---|---|---|
| Angular (framework, CDK, Material, CLI, build) | 21.2.x | `package.json` `~21.2.x` ranges; exact versions in `package-lock.json` |
| TypeScript | 5.9.x | `package.json` `~5.9.2` |
| Node.js | 22 LTS (24 also accepted) | `.nvmrc`, `engines.node` (`^22.12.0 \|\| ^24.0.0`), CI `NODE_VERSION`, `Dockerfile` `node:22-alpine` |
| npm | 10.9 | `packageManager` |
| Change detection | zone.js 0.16 with `provideZoneChangeDetection` | `app.config.ts` (see `TRACKER.md` §4, 2026-11-01) |

`npm run check:toolchain` (`scripts/check-toolchain.mjs`, run in CI) fails if an Angular package or TypeScript allows more than patch updates, if the lockfile is not on Angular 21.2, if `.nvmrc`, CI and the Dockerfile name different Node majors, or if these docs claim another Angular version.

Upgrade policy:

1. **Patch releases** (21.2.x) may land in any story: `npm update`, then the fast suite (`npm test`, `npm run lint:styles`, `npm run build`, `npm run check:prod-urls`).
2. **Minor and major releases** (21.3, 22) are their own story. Use `ng update @angular/core @angular/cli` (plus `@angular/cdk @angular/material`), one major at a time; never edit the ranges by hand. Update the ranges, the `ANGULAR_LINE` in `scripts/check-toolchain.mjs`, this table and `Docs/Architecture.md` in the same commit.
3. **The gate for an upgrade** is the full frontend suite: unit tests, style lint, production build, `check:prod-urls`, Playwright chromium and the visual suite (`npm run e2e:visual`). A visual diff caused by the upgrade is a regression to fix, not a baseline to re-capture (NFR-01).
4. **Node.js** follows the LTS line Angular supports. Change `.nvmrc`, CI `NODE_VERSION` and the Dockerfile together.
5. Record each minor/major upgrade in `TRACKER.md`.
