# LearnSphere — Angular Project Directory Guide

> **Purpose:** This document explains every folder and file in the LearnSphere Angular project. Before creating any new file, read the relevant section here. The goal is to keep the codebase structured — most features can be built by editing files that already exist in this directory. Do not create new files outside this structure without team discussion.
>
> **Stack:** Angular · TypeScript · NgRx · Angular Material · SCSS
> **Roles:** Student · Teacher · Admin

---

## Table of Contents

1. [Project Root](#1-project-root)
2. [src/environments](#2-srcenvironments)
3. [src/styles](#3-srcstyles)
4. [src/app/core](#4-srcappcore)
   - [auth](#41-coreauth)
   - [services](#42-coreservices)
   - [interceptors](#43-coreinterceptors)
   - [guards](#44-coreguards)
   - [models](#45-coremodels)
5. [src/app/shared](#5-srcappshared)
   - [components](#51-sharedcomponents)
   - [pipes](#52-sharedpipes)
   - [directives](#53-shareddirectives)
6. [src/app/features/auth](#6-srcappfeaturesauth)
7. [src/app/features/student](#7-srcappfeaturesstudent)
8. [src/app/features/teacher](#8-srcappfeaturesteacher)
9. [src/app/features/admin](#9-srcappfeaturesadmin)
10. [src/app/features/payment](#10-srcappfeaturespayment)
11. [src/app/store](#11-srcappstore)
12. [src/assets](#12-srcassets)
13. [Root App Files](#13-root-app-files)
14. [Rules for Contributors](#14-rules-for-contributors)

---

## 1. Project Root

These files sit at the root of the `learnsphere/` workspace. Do not modify them unless you know exactly what you are changing.

```
learnsphere/
├── angular.json
├── tsconfig.json
├── package.json
├── .eslintrc.json
├── .prettierrc
└── README.md
```

| File | Purpose | When to edit |
|---|---|---|
| `angular.json` | Angular workspace configuration. Defines build targets, serve options, test configuration, and the global SCSS stylesheets that are loaded before any component styles. | Only when adding a new global asset path, changing the output directory, or configuring a new build environment. |
| `tsconfig.json` | TypeScript compiler configuration. Has strict mode enabled. Also defines path aliases so you can import with `@core/...`, `@shared/...`, `@student/...`, `@teacher/...`, `@admin/...` instead of long relative paths. | When adding a new path alias for a new feature module. |
| `package.json` | Lists all npm dependencies and dev dependencies. Key packages: Angular, NgRx, Angular Material, HLS.js (video streaming), Razorpay SDK, Lottie (animations). | When installing a new npm package — run `npm install <package>` and this file updates automatically. |
| `.eslintrc.json` | ESLint rules for Angular. Enforces consistent code style across the team. | Rarely — only if the team agrees to change a linting rule. |
| `.prettierrc` | Prettier formatting rules: single quotes, 100-character line width, trailing commas. Your editor should format on save using this file. | Rarely — only if the team agrees to change formatting rules. |
| `README.md` | Project setup instructions, environment configuration guide, and module overview. | Update whenever the setup process changes or a new environment variable is added. |

---

## 2. src/environments

```
src/environments/
├── environment.ts
└── environment.prod.ts
```

These two files are the **only place** where API keys, base URLs, and third-party service credentials should be stored. Never hard-code a URL, API key, or feature flag anywhere else in the codebase.

### `environment.ts`
Used during local development (`ng serve`). Contains:
- `apiUrl` — the base URL of the Spring Boot backend (e.g. `http://localhost:8080/api`)
- `razorpayKey` — the Razorpay test key for payment integration
- `firebaseConfig` — Firebase project config for push notifications
- `production: false` — Angular uses this to disable optimisations in dev mode
- Feature flags — boolean toggles to enable/disable features during development (e.g. `enableSentimentAnalysis: false`)

### `environment.prod.ts`
Used when building for production (`ng build --configuration production`). Contains the same keys as `environment.ts` but with production values:
- `apiUrl` — the live backend URL (e.g. `https://api.learnsphere.in/api`)
- `razorpayKey` — the live Razorpay key
- `production: true`
- Analytics enabled: any analytics/monitoring service keys go here

> **Rule:** Any service that reads a config value must import it from `environment.ts`. Angular's build system automatically swaps this file with `environment.prod.ts` on a production build.

---

## 3. src/styles

```
src/styles/
├── _variables.scss
├── _typography.scss
├── _mixins.scss
├── _reset.scss
├── _animations.scss
└── styles.scss
```

These are global SCSS files that apply across the entire application. Component-specific styles go in the component's own `.scss` file, not here.

### `_variables.scss`
The single source of truth for all design tokens. Every colour, spacing value, and brand variable lives here. Key variables:
- `$brand-purple: #534AB7` — primary accent colour used across all three dashboards
- `$admin-sidebar-bg: #12102e` — the dark sidebar background unique to the Admin portal
- Status pill colours: `$status-green` (Live/Active), `$status-amber` (Draft/Upcoming), `$status-red` (Urgent/Flagged), `$status-purple` (In Progress), `$status-grey` (Inactive)
- Spacing scale, border radius tokens, box shadow tokens

> **Rule:** If you need a colour or spacing value, check here first. Never write a raw hex code or pixel value in a component's SCSS file — always use a variable from this file.

### `_typography.scss`
Defines the font scale and text styles used across the app:
- Heading sizes (h1 through h4) and their font weights
- Body text size and line height
- Monospace font stack (used for code snippets, timestamps)
- Utility classes: `.text-muted`, `.text-danger`, `.text-success`

### `_mixins.scss`
Reusable SCSS mixins that components can import. Includes:
- `@mixin flex-center` — centres content with flexbox
- `@mixin responsive($breakpoint)` — wraps styles in the correct media query for mobile/tablet/desktop
- `@mixin truncate($lines)` — clamps text to a given number of lines with ellipsis
- `@mixin card` — applies the standard card shadow and border radius

### `_reset.scss`
Box-sizing reset and base normalisation. Sets `box-sizing: border-box` on all elements and removes default browser margins. Do not add component-specific styles here.

### `_animations.scss`
Shared CSS keyframe animations used across multiple components:
- `fade-in` — used on page transitions and modals
- `slide-in-right` — used by the notes drawer in the video player
- `pulse` — used as a fallback while Lottie animations load
- `skeleton-shimmer` — used on loading skeleton cards

### `styles.scss`
The main entry point that imports all the partials above in the correct order. Also contains Angular Material theme overrides to match the brand purple colour. This file is referenced in `angular.json` as the global stylesheet.

---

## 4. src/app/core

The `core/` module contains **singleton services, guards, interceptors, and shared models**. It is imported exactly once in `AppModule`. Never import `CoreModule` inside a feature module.

```
src/app/core/
├── auth/
├── services/
├── interceptors/
├── guards/
├── models/
└── core.module.ts
```

### 4.1 core/auth

```
core/auth/
├── auth.service.ts
├── auth.guard.ts
├── role.guard.ts
├── token.interceptor.ts
└── auth.models.ts
```

#### `auth.service.ts`
The central authentication service. Handles:
- Login — sends credentials to `POST /api/auth/login`, receives a JWT token
- Logout — clears the JWT from storage and resets the NgRx auth state
- Token refresh — silently refreshes the JWT before it expires
- Role resolution — reads the role claim from the JWT payload and returns `STUDENT`, `TEACHER`, or `ADMIN`
- Exposes observables that other services can subscribe to: `isLoggedIn$`, `currentUser$`

#### `auth.guard.ts`
A route guard applied to all protected routes. Checks whether a valid JWT exists in storage. If not, redirects the user to `/login`. Apply this guard to any route that requires authentication.

#### `role.guard.ts`
A route guard that checks the user's role. Applied separately to `/student/**`, `/teacher/**`, and `/admin/**` routes. If a student tries to access `/teacher/dashboard`, this guard redirects them to their own dashboard. Always use this in combination with `auth.guard.ts`.

#### `token.interceptor.ts`
An HTTP interceptor that automatically attaches the Bearer JWT token to the `Authorization` header of every outgoing HTTP request. You do not need to manually set the token in any service — this interceptor handles it globally.

#### `auth.models.ts`
TypeScript interfaces for authentication-related data shapes:
- `AuthResponse` — the shape of the login API response (token, user info)
- `UserPayload` — the decoded JWT payload structure (userId, role, collegeId, email)
- `JwtClaims` — raw claims from the JWT

---

### 4.2 core/services

```
core/services/
├── api.service.ts
├── notification.service.ts
├── storage.service.ts
└── error-handler.service.ts
```

#### `api.service.ts`
The base HTTP service that wraps Angular's `HttpClient`. All other services in the app should use `api.service.ts` instead of injecting `HttpClient` directly. Provides typed helper methods:
- `get<T>(endpoint)` — makes a GET request and returns `Observable<T>`
- `post<T>(endpoint, body)` — makes a POST request
- `put<T>(endpoint, body)` — makes a PUT request
- `delete<T>(endpoint)` — makes a DELETE request

By centralising HTTP calls here, we get consistent error handling, base URL injection from `environment.ts`, and easy mocking in tests.

#### `notification.service.ts`
Manages two types of notifications:
- In-app toast notifications — shows success, error, warning, and info toasts across the app. Call `notificationService.success('Lecture uploaded!')` from any component or service.
- Firebase Cloud Messaging bridge — registers the device for push notifications and handles incoming messages when the app is in the background.

#### `storage.service.ts`
A typed wrapper around the browser's `localStorage` and `sessionStorage`. Never call `localStorage.setItem()` directly in a component. Use this service instead:
- `storage.set(key, value)` — serialises and stores a value
- `storage.get<T>(key)` — retrieves and deserialises a typed value
- `storage.remove(key)` — removes a key
- `storage.clear()` — clears all stored data (called on logout)

#### `error-handler.service.ts`
The global error handler. Catches unhandled JavaScript errors and HTTP errors that slip past the interceptor. Logs them and shows a user-friendly message via `notification.service.ts`. Do not write `console.error()` in components — dispatch errors through this service.

---

### 4.3 core/interceptors

```
core/interceptors/
├── loading.interceptor.ts
└── error.interceptor.ts
```

#### `loading.interceptor.ts`
Tracks all in-flight HTTP requests. Sets a global `isLoading` state to `true` when any request starts, and back to `false` when all requests complete. The topbar spinner reads this state. You do not need to manually manage loading states for API calls in individual components.

#### `error.interceptor.ts`
Catches HTTP error responses globally:
- `401 Unauthorized` — clears the JWT and redirects to `/login`
- `403 Forbidden` — shows a "you don't have permission" toast
- `5xx Server Error` — shows a generic "something went wrong" toast and logs the error
This interceptor runs before the response reaches any service, so services only need to handle the happy path.

---

### 4.4 core/guards

```
core/guards/
├── auth.guard.ts         (see 4.1)
├── role.guard.ts         (see 4.1)
├── payment.guard.ts
├── remedial.guard.ts
└── verified-college.guard.ts
```

#### `payment.guard.ts`
Applied to routes that serve paid course content (external students purchasing courses). Checks whether the current user has a valid payment record for the requested course. If not, redirects to the `/payment/checkout` page for that course. Internal students bypass this guard automatically since their courses are free.

#### `remedial.guard.ts`
Applied to the remedial certification exam routes. Checks whether the student has been flagged for remedial by their college and whether they are accessing a course they are actually assigned to. Prevents remedial students from accessing non-assigned courses via direct URL.

#### `verified-college.guard.ts`
Applied immediately after registration. Checks whether the user has completed the `/verify-college` flow (email verification for students, ID verification for teachers). If not, redirects back to `/verify-college` regardless of which route they try to access. This guard is removed from the route stack once verification is complete.

---

### 4.5 core/models

```
core/models/
├── user.model.ts
├── course.model.ts
├── assessment.model.ts
├── payment.model.ts
└── api-response.model.ts
```

These files contain **only TypeScript interfaces and enums** — no logic, no services. They define the data shapes that flow between the backend API and the frontend.

#### `user.model.ts`
```typescript
// Key interfaces defined here:
interface User { id, email, role, collegeId, name, avatarUrl, createdAt }
interface Student extends User { enrolledCourses, attendanceScore, leaderboardPoints }
interface Teacher extends User { department, bio, bankAccountLinked, royaltyBalance }
interface Admin extends User { twoFactorEnabled, lastAuditAction }
enum UserRole { STUDENT = 'STUDENT', TEACHER = 'TEACHER', ADMIN = 'ADMIN' }
```

#### `course.model.ts`
```typescript
// Key interfaces defined here:
interface Course { id, title, description, teacherId, department, thumbnail, status, price }
interface Lecture { id, courseId, title, number, videoUrl, duration, isDownloadable, status }
interface CourseStatus { DRAFT, PENDING, LIVE, ARCHIVED }
```

#### `assessment.model.ts`
```typescript
// Key interfaces defined here:
interface Quiz { id, courseId, lectureId, title, timeLimit, questionCount, dueDate, type }
interface Question { id, quizId, text, options[], correctIndex, aiGenerated }
interface Submission { id, quizId, studentId, answers[], score, submittedAt }
enum AssessmentType { QUIZ, REMEDIAL, CERTIFICATION }
```

#### `payment.model.ts`
```typescript
// Key interfaces defined here:
interface Order { id, courseId, studentId, amount, currency, razorpayOrderId }
interface Payment { id, orderId, razorpayPaymentId, status, paidAt }
interface RazorpayResponse { razorpay_payment_id, razorpay_order_id, razorpay_signature }
```

#### `api-response.model.ts`
```typescript
// Generic wrapper for all API responses:
interface ApiResponse<T> { data: T, message: string, success: boolean, timestamp: string }
interface PaginatedResponse<T> extends ApiResponse<T[]> { page, size, totalElements, totalPages }
```

> **Rule:** Every API call in a service must type its return value using these interfaces. Never use `any` as a return type.

---

## 5. src/app/shared

The `shared/` module contains reusable components, pipes, and directives that are used across all three role dashboards. Import `SharedModule` in any feature module that needs these building blocks.

```
src/app/shared/
├── components/
├── pipes/
├── directives/
└── shared.module.ts
```

### 5.1 shared/components

#### `video-player/`
The central video player component used on the student lecture page. Do not build a separate player anywhere else. Accepts inputs:
- `[videoUrl]` — the HLS stream URL from the backend
- `[lectureId]` — used to track watch progress and save bookmarks
- `[allowDownload]` — boolean set by the teacher's course settings

Features built into this component: seek bar with timestamp, playback speed (0.5x–2x), subtitle toggle, quality selector (360p/720p/1080p/Auto), bookmark-this-moment button, and a toggle to open the notes drawer. The notes drawer is a child component (`notes-drawer.component.ts`) that slides in from the right without interrupting playback.

#### `stat-card/`
The KPI card that appears at the top of every dashboard (4 cards on student dashboard, 4 on teacher, 4 on admin). Accepts inputs:
- `[icon]` — icon name
- `[value]` — the number or text to display prominently
- `[label]` — the descriptor below the value (e.g. "Enrolled Courses")
- `[trend]` — optional: `up` or `down` with a percentage for trend arrows

All dashboard KPI cards across the entire app use this one component.

#### `sidebar/`
The icon-only collapsed sidebar navigation. Accepts a `[navItems]` input array that is built differently per role (student, teacher, admin). The active route is highlighted automatically using Angular's `routerLinkActive`. On hover, each icon shows a tooltip label. The Admin sidebar uses the `$admin-sidebar-bg` dark background from `_variables.scss`; student and teacher use the light variant.

#### `topbar/`
The top navigation bar present on all authenticated pages. Displays:
- Platform logo (links to the role's dashboard home)
- Global loading spinner (reads from `loading.interceptor.ts`)
- Notifications bell with unread badge (reads from `notification.service.ts`)
- User avatar with dropdown: Profile, Settings, Sign Out
- "Upload Lecture" CTA button — visible only when the logged-in role is `TEACHER`, controlled by `role.directive.ts`

#### `status-pill/`
A colour-coded pill/badge component used everywhere in the app. Accepts a `[status]` input and maps it to the correct colour:
- `LIVE` / `ACTIVE` / `POSITIVE` → green
- `DRAFT` / `UPCOMING` / `WARNING` → amber
- `URGENT` / `FLAGGED` / `HIGH_RISK` → red
- `IN_PROGRESS` / `YOUR_ITEM` → brand purple
- `INACTIVE` / `DEFAULT` → grey

Always use this component for status indicators. Do not write inline status styles in a component's template.

#### `progress-bar/`
A styled progress bar used for course completion and lecture watch progress. Accepts:
- `[value]` — current progress (0–100)
- `[label]` — optional text label shown beside the bar (e.g. "7/12 lectures")
- `[color]` — defaults to brand purple; pass `'green'` for completed state

Used in: student my-courses, student dashboard continue-learning panel, teacher student-standings, certificates in-progress section.

#### `confirm-dialog/`
A reusable confirmation modal for destructive actions. Open it from any component using Angular's `MatDialog`:
```typescript
this.dialog.open(ConfirmDialogComponent, {
  data: { title: 'Blacklist User', message: 'This action cannot be undone.', confirmLabel: 'Blacklist' }
})
```
Used before: blacklisting a user, archiving a course, deleting a note, removing a flagged post.

#### `empty-state/`
Shown when a list or page has no data. Displays a Lottie animation and a CTA button. Accepts:
- `[title]` — e.g. "No courses yet"
- `[subtitle]` — e.g. "Enrol in your first course to get started"
- `[ctaLabel]` — button text
- `[ctaRoute]` — router link for the button

Used in: my-courses (no enrolments), bookmarks (nothing saved), leaderboard (no data), certificates (none earned).

#### `data-table/`
A sortable, filterable, paginated table component. Used by admin user management, admin payout list, and teacher student standings. Accepts:
- `[columns]` — column definitions (key, header, sortable)
- `[data]` — the array of rows
- `[pageSize]` — rows per page (default 20)
- Emits: `(rowAction)` — fires when a per-row action button is clicked, passing the row data and action name

#### `avatar/`
A user avatar component. Shows the user's profile photo if available; falls back to their initials in a coloured circle. Accepts `[user]` input. Optionally shows an online indicator dot.

---

### 5.2 shared/pipes

#### `time-ago.pipe.ts`
Converts an ISO timestamp to a human-readable relative string. Used throughout discussion threads, notification feeds, and message inboxes.
```
"2 minutes ago" / "1 hour ago" / "3 days ago"
```

#### `duration.pipe.ts`
Converts a number of seconds to a display-friendly time string. Used on lecture cards and in the video player.
```
125 → "2:05"     3665 → "1:01:05"
```

#### `currency-inr.pipe.ts`
Formats a number as Indian Rupees with the ₹ symbol and correct Indian comma notation.
```
15000 → "₹15,000"     150000 → "₹1,50,000"
```

#### `truncate.pipe.ts`
Truncates a string to a given character count and appends ellipsis. Used on course description cards.
```
{{ course.description | truncate:120 }}
```

---

### 5.3 shared/directives

#### `role.directive.ts`
A structural directive that shows or hides a DOM element based on the current user's role. Use this instead of writing `*ngIf="userRole === 'TEACHER'"` directly in templates.
```html
<button *appRole="['TEACHER']">Upload Lecture</button>
<div *appRole="['ADMIN', 'TEACHER']">Moderation Panel</div>
```

#### `click-outside.directive.ts`
Emits an event when the user clicks outside a target element. Used to close dropdowns, the notification panel, and the user avatar menu in the topbar.
```html
<div class="dropdown" (appClickOutside)="closeDropdown()">...</div>
```

#### `lazy-image.directive.ts`
Uses the browser's `IntersectionObserver` API to only load an image when it scrolls into view. Apply to any `<img>` that is not in the initial viewport — course thumbnails in long lists, avatar images in the leaderboard, etc.
```html
<img [appLazyImage]="course.thumbnailUrl" alt="Course thumbnail" />
```

---

## 6. src/app/features/auth

Public-facing pages. These routes are accessible without a JWT token. No sidebar or topbar is shown on these pages.

```
features/auth/
├── login/
│   ├── login.component.ts
│   ├── login.component.html
│   └── login.component.scss
├── register/
│   ├── register.component.ts
│   ├── register.component.html
│   └── register.component.scss
├── verify-college/
│   ├── verify-college.component.ts
│   ├── verify-college.component.html
│   └── verify-college.component.scss
├── forgot-password/
│   └── forgot-password.component.ts
├── auth-routing.module.ts
└── auth.module.ts
```

### Routes

| Route | Component | Purpose |
|---|---|---|
| `/login` | `login.component.ts` | Email + password form. On success, dispatches the NgRx `login` action, stores the JWT via `storage.service.ts`, reads the role from the token, and navigates to `/student/dashboard`, `/teacher/dashboard`, or `/admin/dashboard` accordingly. |
| `/register` | `register.component.ts` | Multi-step registration form. Step 1: role selection (Student or Teacher). Step 2: college search (searches the college database by name). Step 3: credentials (name, email, password). Step 4: triggers the verify-college flow. |
| `/verify-college` | `verify-college.component.ts` | For students: sends a verification link to the provided college email. For teachers: prompts upload of a college ID card. Once verified, `verified-college.guard.ts` allows navigation to the dashboard. This page is also reached if an existing user's verification lapses. |
| `/forgot-password` | `forgot-password.component.ts` | Sends a password reset OTP to the user's registered college email. |

### `auth-routing.module.ts`
Defines the four routes above. All routes in this module are publicly accessible — `auth.guard.ts` is deliberately not applied here.

### `auth.module.ts`
The Angular module that declares the auth components and imports `ReactiveFormsModule` for the login and register forms. Does not import `SharedModule` — auth pages are intentionally minimal.

---

## 7. src/app/features/student

All pages for the Student role. This module is **lazy-loaded** — it is only downloaded by the browser when a user with the STUDENT role logs in.

```
features/student/
├── dashboard/
├── courses/
│   ├── course-detail/
│   ├── lecture/
│   │   └── components/
│   └── components/
├── notes/
│   └── components/
├── assessments/
│   └── quiz/
│       └── components/
├── bookmarks/
├── leaderboard/
├── discussion/
├── certificates/
├── settings/
├── services/
├── student-routing.module.ts
└── student.module.ts
```

### Routes

| Route | Component | Description |
|---|---|---|
| `/student/dashboard` | `dashboard/dashboard.component.ts` | Student home page |
| `/student/courses` | `courses/courses.component.ts` | My courses list + explore |
| `/student/courses/:id` | `courses/course-detail/course-detail.component.ts` | Individual course overview |
| `/student/courses/:id/lecture/:lid` | `courses/lecture/lecture.component.ts` | Video player page |
| `/student/notes` | `notes/notes.component.ts` | All notes across courses |
| `/student/assessments` | `assessments/assessments.component.ts` | Assessment list |
| `/student/assessments/:id` | `assessments/quiz/quiz.component.ts` | Live quiz interface |
| `/student/bookmarks` | `bookmarks/bookmarks.component.ts` | Saved lectures and resources |
| `/student/leaderboard` | `leaderboard/leaderboard.component.ts` | College rankings |
| `/student/discussion` | `discussion/discussion.component.ts` | Forum |
| `/student/certificates` | `certificates/certificates.component.ts` | Certificates earned |
| `/student/settings` | `settings/settings.component.ts` | Profile and preferences |

---

### dashboard/

`dashboard.component.ts` — the first page a student sees after login. Renders:
- A personalised greeting with the current date
- 4 `stat-card` components: enrolled courses count, attendance score, leaderboard rank, certificates earned
- "Continue Learning" panel — fetched from `student.service.ts`, shows in-progress courses with `progress-bar` components and lecture counters
- "Pending Assessments" section — due-date sorted list with urgency `status-pill` components
- Recent notifications feed
- "AI Recommended Courses" — populated by the Phase 2 recommendation engine; in Phase 1, shows department-filtered courses instead

---

### courses/

#### `courses.component.ts` — route: `/student/courses`
The my-courses page. Has filter tabs (All / In Progress / Completed / Not Started / Explore). Shows an overall completion `progress-bar` across all enrolled courses. Each course is rendered using `course-card.component.ts`. The "Explore" tab shows cross-department courses (free for same-college students, price displayed for external paid courses) via `explore-section.component.ts`.

#### `course-detail/course-detail.component.ts` — route: `/student/courses/:id`
The course overview page shown when a student clicks into a specific course before selecting a lecture. Displays:
- Course title, teacher name, department
- Overall course `progress-bar`
- Full lecture list with status icons (Done ✓ / Not Started / Locked 🔒)
- Locked lectures use sequential unlock logic — a lecture unlocks only when the previous one is marked complete
- An "Enrol" button if the student has not yet enrolled
- An "Attempt Certification Exam" CTA when all lectures are complete

#### `lecture/lecture.component.ts` — route: `/student/courses/:id/lecture/:lid`
The main video learning page. This is the most complex page in the student module. Contains:
- The `video-player` shared component (passes `videoUrl`, `lectureId`, `allowDownload` inputs)
- A lecture grid panel (from `lecture-grid.component.ts`) showing all lectures in the course with their status
- Clicking a lecture in the grid navigates to `/student/courses/:id/lecture/:newLid`
- A per-lecture discussion thread below the player (from `lecture-discussion.component.ts`)
- The notes drawer (from `notes-drawer.component.ts`) that slides in from the right when the student clicks the notes icon in the player

##### `lecture/components/lecture-grid.component.ts`
Renders the list of all lectures for the current course. Each lecture row shows: lecture number, title, duration, and a status indicator (Done / Playing Now / Locked / Not Started). The currently playing lecture is highlighted.

##### `lecture/components/notes-drawer.component.ts`
A panel that slides in from the right side without pausing the video. Allows the student to write a note that is automatically linked to the current `lectureId` and the current playback timestamp. The timestamp is captured from the `video-player` component via an `@Input`. The note is saved via `student.service.ts`.

##### `lecture/components/lecture-discussion.component.ts`
Renders the Q&A thread for the specific lecture currently being watched. Students can post questions. Teacher replies are indented with a purple left border. Integrates with the backend discussion API via `student.service.ts`.

#### `courses/components/course-card.component.ts`
Used both in the my-courses list and the explore section. Displays: thumbnail, course title, teacher name, `progress-bar`, lecture count, and a `status-pill`. Has an Enrol button for unenrolled courses (triggers payment flow for paid courses, direct enrolment for free).

#### `courses/components/explore-section.component.ts`
The "Explore" tab content. Fetches all courses from other departments in the same college (shown free) and courses from external colleges (shown with price). Clicking an external paid course triggers the `/payment/checkout` flow.

---

### notes/

#### `notes.component.ts` — route: `/student/notes`
Displays all notes the student has created across all courses. Filter tabs let the student filter by course. Each note is shown using `note-card.component.ts`. Export buttons at the top: "Export All as PDF", "Export by Course", "Copy to Clipboard".

##### `components/note-card.component.ts`
Displays a single note. Shows: course name, lecture title, the timestamp at which the note was created (e.g. ⏱ 22:14) as a clickable link that navigates to `/student/courses/:id/lecture/:lid` and resumes at that timestamp, the note text, and any tags (e.g. `#formula`, `#exam`). Has inline Edit and Delete buttons.

##### `components/note-editor.component.ts`
A modal editor opened when the student clicks "Edit" on a note or "New Note" from the notes page. Allows editing the note text, adding/removing tags, and changing the linked course/lecture. Saves via `student.service.ts`.

---

### assessments/

#### `assessments.component.ts` — route: `/student/assessments`
The assessment list page. Filter tabs: All / Pending / Completed / Remedial. Shows 3 `stat-card` components: total done, pending count, highest score. Each pending assessment shows: quiz title, course name, question count, time limit, due date, and a `status-pill` (Urgent / Upcoming / Remedial). A "Start Now" button navigates to `/student/assessments/:id`.

#### `quiz/quiz.component.ts` — route: `/student/assessments/:id`
The live quiz page. Fetches the quiz by ID from `assessment.service.ts` and renders the MCQ interface. Features:
- One question displayed at a time
- Countdown timer (starts from the quiz's time limit)
- Auto-submits when the timer reaches zero
- Option selection highlights the chosen answer
- Next/Previous navigation
- On submission, shows a score summary and navigates to the review state

##### `quiz/components/quiz-interface.component.ts`
Renders a single MCQ question with its answer options. Handles option selection state and emits the selected answer to the parent `quiz.component.ts`.

##### `quiz/components/assessment-review.component.ts`
Post-submission review view. Shows the student's score, and for each question: what they answered, what the correct answer was, and whether they got it right. Accessed after submitting or by clicking "Review" on a completed assessment.

##### `quiz/components/remedial-card.component.ts`
A special card shown at the top of the assessments list for students who have been flagged for remedial. Shows the assigned remedial course, the certification exam they must pass, and the attendance impact (e.g. "Passing this exam will credit 15 attendance hours"). Has an "Attempt Now" CTA.

---

### bookmarks/

#### `bookmarks.component.ts` — route: `/student/bookmarks`
Shows all items the student has saved. Filter tabs: All / Lectures / Notes / Resources. Each saved lecture moment shows: course thumbnail, course name, lecture title, the bookmarked timestamp, and a "Resume" button that navigates to the lecture at that exact timestamp. Downloaded resources show a re-download link. Uses `bookmark.service.ts`.

---

### leaderboard/

#### `leaderboard.component.ts` — route: `/student/leaderboard`
Shows college-wide and batch rankings. Filter tabs: My Batch / By Course / All College / Monthly. Shows 3 `stat-card` components: current rank, total points, points needed for next rank. The ranked list highlights the top 3 with medal icons, shows a gap indicator, then the logged-in student's row (highlighted in brand purple), and one rank below. A "Points Breakdown" panel explains how points are earned:
- Complete a lecture → +10 pts
- Submit an assessment → +25 pts
- Score above 90% → +50 bonus pts
- Helpful discussion post (marked by teacher) → +5 pts

---

### discussion/

#### `discussion.component.ts` — route: `/student/discussion`
The student forum. Filter tabs by course plus a "My Posts" tab. Each thread card shows: author avatar, name, course name, lecture reference, time ago (via `time-ago` pipe), post content, a `status-pill` (Unanswered / Answered / Pinned), teacher reply indented with a left border, like count, reply count, and a Reply button. A compose box at the bottom lets the student post a new question.

---

### certificates/

#### `certificates.component.ts` — route: `/student/certificates`
Three sections:
1. **Earned certificates** — grid of certificate cards, each with course name, issue date, teacher name, a "Download PDF" button, and a "Share" button (LinkedIn link + copy-link)
2. **In-progress certificates** — courses where the student has not yet completed all lectures or the final exam; shows a `progress-bar` and the remaining requirements
3. **Remedial certification** — if applicable, shows the remedial cert that is pending with an "Attempt Now" CTA

---

### settings/

#### `settings.component.ts` — route: `/student/settings`
Organised into sections:
- **Profile** — full name, college email (with verified badge), password change, profile photo upload
- **Notifications** — toggles for: new lecture uploaded, assessment due reminder, teacher reply in discussion, rank change, promotional announcements
- **Preferences** — default video quality, default playback speed, leaderboard visibility (opt in/out of appearing on the public leaderboard), language
- **Account** — linked payment method (for external course purchases), download my data (GDPR-style export), sign out

---

### services/ (student)

```
student/services/
├── student.service.ts
├── assessment.service.ts
├── bookmark.service.ts
└── leaderboard.service.ts
```

#### `student.service.ts`
Handles all API calls for the student module that don't belong to a more specific service:
- `getEnrolledCourses()` — fetches the student's enrolled course list
- `getCourseDetail(courseId)` — fetches a single course with its lecture list
- `getLecture(courseId, lectureId)` — fetches a lecture's details and video URL
- `markLectureComplete(lectureId)` — marks a lecture as watched and awards points
- `getWatchProgress(lectureId)` — gets the last-watched timestamp for resume functionality
- `createNote(note)` / `updateNote(noteId, note)` / `deleteNote(noteId)` — notes CRUD
- `getNotes(courseId?)` — fetches notes, optionally filtered by course

#### `assessment.service.ts`
- `getAssessments()` — lists all assessments for the student
- `getQuiz(quizId)` — fetches questions for a specific quiz
- `startQuiz(quizId)` — notifies the backend that the timer has started
- `submitQuiz(quizId, answers)` — submits answers and returns score
- `getReview(quizId)` — fetches the post-submission review data

#### `bookmark.service.ts`
- `saveBookmark(lectureId, timestamp)` — saves a lecture moment
- `getBookmarks()` — fetches all saved items
- `removeBookmark(bookmarkId)` — removes a saved item

#### `leaderboard.service.ts`
- `getLeaderboard(filter)` — fetches rankings by batch, course, college, or monthly
- `getPointsBreakdown()` — fetches the current student's points history

### `student-routing.module.ts`
Defines all 12 student routes listed in the route table above. All routes in this module are protected by `auth.guard.ts` and `role.guard.ts` (STUDENT only).

### `student.module.ts`
The lazy-loaded Angular module for the student role. Imports `SharedModule` to access shared components, pipes, and directives. Declares all student-specific components.

---

## 8. src/app/features/teacher

All pages for the Teacher role. Lazy-loaded — only downloaded when a TEACHER logs in.

```
features/teacher/
├── dashboard/
├── courses/
│   ├── course-management/
│   │   └── components/
│   └── components/
├── upload/
│   └── components/
├── students/
├── messages/
├── discussion/
├── live/
├── trending/
├── royalties/
├── settings/
├── services/
├── teacher-routing.module.ts
└── teacher.module.ts
```

### Routes

| Route | Component | Description |
|---|---|---|
| `/teacher/dashboard` | `dashboard/dashboard.component.ts` | Teacher home |
| `/teacher/courses` | `courses/courses.component.ts` | My courses list |
| `/teacher/courses/:id` | `courses/course-management/course-management.component.ts` | Manage a specific course |
| `/teacher/upload` | `upload/upload.component.ts` | Upload content |
| `/teacher/students` | `students/students.component.ts` | Student standings |
| `/teacher/messages` | `messages/messages.component.ts` | 1-on-1 inbox |
| `/teacher/discussion` | `discussion/discussion.component.ts` | Forum moderation |
| `/teacher/live` | `live/live.component.ts` | Live sessions |
| `/teacher/trending` | `trending/trending.component.ts` | Platform trends |
| `/teacher/royalties` | `royalties/royalties.component.ts` | Earnings and payouts |
| `/teacher/settings` | `settings/settings.component.ts` | Profile and preferences |

---

### dashboard/

`dashboard.component.ts` — the teacher's home page. Shows:
- 4 `stat-card` components: total students enrolled across all courses, active (live) course count, average rating across all courses, royalty earned this month
- "My Courses" panel — each course shown with student count, star rating, and `status-pill` (Live/Draft/Pending)
- "Recent Student Messages" — inbox preview with unread dot and sender preview, links to `/teacher/messages`
- Quick action buttons: Upload Lecture (→ `/teacher/upload`), Schedule Session (→ `/teacher/live`), Add Quiz (→ `/teacher/upload`), Post Announcement

---

### courses/

#### `courses.component.ts` — route: `/teacher/courses`
My courses list with filter tabs: All / Live / Draft / Pending / Archived. Each course card shows enrolled count, average rating, completion rate, and a `status-pill`. A "New Course" button opens the `course-editor.component.ts` modal to create a new course.

#### `course-management/course-management.component.ts` — route: `/teacher/courses/:id`
The course management detail page for a specific course. Contains:
- Course info header with Edit button (opens `course-editor.component.ts`)
- The full lecture list rendered by `lecture-list.component.ts` — each lecture has a status badge (Published / Draft / Processing) and an Edit button
- Action buttons: Add Lecture (→ `/teacher/upload`), Add Quiz (→ `/teacher/upload`), Upload Notes, Archive Course
- For draft courses: a completion progress bar and a "Submit for Review" button (sends to admin)

##### `course-management/components/lecture-list.component.ts`
Renders all lectures in a course as a draggable list (teachers can reorder lectures). Each row shows: lecture number, title, duration, status badge, and an Edit button. The Edit button opens the video upload component with the existing lecture data pre-filled.

##### `course-management/components/course-editor.component.ts`
A modal form for creating or editing a course. Fields: title, description, department, thumbnail upload, price (0 for free), and publish settings. Used both from "New Course" button and the "Edit" button on an existing course.

---

### upload/

#### `upload.component.ts` — route: `/teacher/upload`
The content upload hub. A tab switcher lets the teacher choose what to upload: Video Lecture / Notes·PDF / Quiz·Assessment / Assignment / Resources.

##### `components/video-upload.component.ts`
Handles video lecture upload. Features:
- Drag-and-drop zone (or file browser) accepting MP4/MOV/AVI files up to 2GB
- Fields: select course (dropdown of teacher's courses), lecture number, lecture title, description
- AI Question Extraction toggle — when enabled, the backend processes the video transcript and returns suggested quiz questions; the teacher reviews and approves before they go live
- Action buttons: "Upload & Publish" (uploads and immediately makes it live), "Save as Draft" (uploads but keeps it hidden from students), "Schedule Publish" (sets a future publish date and time)
- A "Recent Uploads" panel below showing the last 3 uploads with their processing / live status
- Upload progress tracked via `upload.service.ts`

##### `components/quiz-builder.component.ts`
Allows the teacher to create or edit a quiz linked to a course/lecture. The teacher adds MCQ questions manually, or if AI extraction is enabled, a list of AI-suggested questions is shown for review. The teacher can approve, edit, or discard each suggested question. Time limit, passing score, and due date are also set here.

##### `components/resource-upload.component.ts`
For uploading non-video content: PDF notes, slide decks, or reference files. The teacher selects the course and optionally a specific lecture to link the resource to.

---

### students/

#### `students.component.ts` — route: `/teacher/students`
Student standings across the teacher's courses. Filter: by course, or an "At-Risk" filter. Shows:
- 3 `stat-card` components: total enrolled, average completion %, at-risk count
- Top 3 performers shown as highlight cards with avatar, rank, score %, and lecture count
- Full standings list (paginated) using `data-table.component.ts`: rank, avatar, name, lectures watched, score
- At-risk students panel — students below 40% progress or inactive for 14+ days. Each row has a "Send Nudge" button which triggers a notification to that student via `notification.service.ts`

---

### messages/

#### `messages.component.ts` — route: `/teacher/messages`
A two-panel chat interface:
- **Left panel** — conversation list. Each row shows: student avatar, student name, last message preview, unread dot if there are unread messages. Clicking a row opens the conversation in the right panel.
- **Right panel** — full conversation thread. Sent messages appear on the right; received messages on the left. A student info header at the top has two shortcut buttons: "View Profile" and "View Course Progress" (opens the relevant row in `/teacher/students`). A compose box at the bottom with a Send button.
Uses `messaging.service.ts` for real-time updates.

---

### discussion/

#### `discussion.component.ts` — route: `/teacher/discussion`
The teacher's moderation view of the forum. Filter tabs: by course, plus "Flagged" and "Unanswered" tabs. Shows 3 `stat-card` components: total posts, unanswered count, flagged count. Each post has action buttons: Reply / Pin / Mark Resolved / Flag·Remove. AI-flagged posts are shown with a red border, the content hidden by default, and an AI confidence score. The teacher's own replies appear indented below the original question.

---

### live/

#### `live.component.ts` — route: `/teacher/live`
Schedule and manage live sessions. Contains:
- 3 `stat-card` components: sessions this month, next scheduled session date, total registrations
- A "Schedule New Session" form: course selector, session title, date, time, duration, platform choice (Built-in room or Google Meet link). Action buttons: "Schedule & Notify Students" / "Save Draft"
- "Upcoming Sessions" list — each session shows registered student count, an Edit button, and a "Start Now" button
- "Past Sessions & Recordings" — each past session shows attendee count, recording availability, and a "Share Recording" button

---

### trending/

#### `trending.component.ts` — route: `/teacher/trending`
Market intelligence for the teacher. Contains:
- 3 `stat-card` components: teacher's best platform rank, total courses on platform, teacher's average rating vs platform average
- Platform-wide trending courses list (ranked), with the teacher's own courses highlighted in brand purple
- Student sentiment panel for the teacher's courses: bar charts showing positive / neutral / negative % per course. Sourced from the Phase 2 sentiment analysis service.
- Top student comments — best and most critical recent reviews displayed side by side

---

### royalties/

#### `royalties.component.ts` — route: `/teacher/royalties`
The earnings and payout dashboard. Contains:
- 4 `stat-card` components: this month's earnings, total earned since joining, next payout date, external enrollment count
- Earnings by course table — for each course: enrolled count, external paid count, monthly earnings
- Payout history — each past month with amount and transfer date; current month shown as "Pending"
- Royalty breakdown bars — shows what percentage of earnings came from: external course sales / college share / remedial certifications
- A note disclosing the platform fee: "LearnSphere retains 20% of all external sales"

---

### settings/ (teacher)

#### `settings.component.ts` — route: `/teacher/settings`
Organised into sections:
- **Profile** — name, college email (verified badge), department, bio/intro text (this appears on the teacher's public profile page)
- **Linked bank account** — for receiving monthly royalty payouts via bank transfer
- **Notifications** — toggles for: new enrollment, student message, new discussion post, course approved/rejected by admin, payout processed, AI flag on course content
- **Course defaults** — platform-wide defaults for this teacher's courses: AI question extraction on/off, auto-notify students on new lecture upload, allow discussion on each lecture, allow DMs from students
- **Account** — password change, download my data, sign out

---

### services/ (teacher)

```
teacher/services/
├── teacher.service.ts
├── upload.service.ts
├── royalty.service.ts
└── messaging.service.ts
```

#### `teacher.service.ts`
- `getMyCourses()` — fetches the teacher's courses with status
- `getCourseDetail(courseId)` — fetches a specific course with full lecture list
- `createCourse(course)` / `updateCourse(courseId, course)` — course CRUD
- `submitCourseForReview(courseId)` — changes course status to PENDING
- `getStudentStandings(courseId)` — fetches the standings list for a course
- `sendNudge(studentId, courseId)` — sends a nudge notification to an at-risk student

#### `upload.service.ts`
Handles the video upload flow to AWS S3 / Azure Blob:
- `uploadVideo(file, metadata)` — initiates chunked multipart upload, returns an observable with progress percentage
- `getUploadStatus(uploadId)` — polls the backend for processing status (queued / processing / live / failed)
- `uploadResource(file, metadata)` — uploads PDF/notes files

#### `royalty.service.ts`
- `getEarnings()` — fetches this month's and historical earnings
- `getPayoutHistory()` — fetches the list of past payouts with dates and amounts
- `getEarningsByCourse()` — fetches the per-course revenue breakdown

#### `messaging.service.ts`
- `getConversations()` — fetches all student conversation threads with unread counts
- `getMessages(studentId)` — fetches the full message thread with a specific student
- `sendMessage(studentId, text)` — sends a reply
- Sets up WebSocket or polling for real-time message updates

---

## 9. src/app/features/admin

All pages for the Admin role. Lazy-loaded — only downloaded when an ADMIN logs in. Visually distinct from student/teacher: uses a dark sidebar (`$admin-sidebar-bg: #12102e`).

```
features/admin/
├── dashboard/
├── users/
├── courses/
├── colleges/
├── revenue/
├── payouts/
├── reports/
├── flagged/
├── sentiment/
├── settings/
├── services/
├── admin-routing.module.ts
└── admin.module.ts
```

### Routes

| Route | Component | Description |
|---|---|---|
| `/admin/dashboard` | `dashboard/dashboard.component.ts` | Platform overview |
| `/admin/users` | `users/users.component.ts` | User management |
| `/admin/courses` | `courses/courses.component.ts` | Course approvals |
| `/admin/colleges` | `colleges/colleges.component.ts` | College management |
| `/admin/revenue` | `revenue/revenue.component.ts` | Revenue analytics |
| `/admin/payouts` | `payouts/payouts.component.ts` | Payout processing |
| `/admin/reports` | `reports/reports.component.ts` | Platform reports |
| `/admin/flagged` | `flagged/flagged.component.ts` | Flagged content |
| `/admin/sentiment` | `sentiment/sentiment.component.ts` | Sentiment analysis |
| `/admin/settings` | `settings/settings.component.ts` | Platform configuration |

---

### dashboard/

`dashboard.component.ts` — platform-wide overview. Shows:
- 4 `stat-card` components: total revenue, registered users, active courses, open flags
- Top 3 teachers this month: ranked cards with avatar, student count, rating, royalty earned
- Top 3 students this month: ranked by leaderboard points
- Quick action buttons: Review Courses (→ `/admin/courses`), Manage Users (→ `/admin/users`), Process Payouts (→ `/admin/payouts`), Review Flags (→ `/admin/flagged`)
- Platform health snapshot: a 4-box grid showing positive sentiment %, course approval rate, average student completion %, open flag count

---

### users/

#### `users.component.ts` — route: `/admin/users`
Manages all platform users. Contains:
- Filter tabs: All Users / Students / Teachers / Flagged / Blacklisted
- 4 `stat-card` components: total users, student count, teacher count, blacklisted count
- Searchable and filterable table using `data-table.component.ts` with columns: avatar, name, email, role, department, status `status-pill`
- Per-row action buttons: View (opens user profile modal) / Warn (sends a warning notification) / Blacklist (for students — blocks access) / Suspend (for teachers — unpublishes their courses) / Restore (for blacklisted/suspended users)
- Destructive actions open `confirm-dialog.component.ts` before executing
- User activity breakdown bars: daily active %, weekly active %, inactive 30d+ %

---

### courses/

#### `courses.component.ts` — route: `/admin/courses`
Manages course approvals. Filter tabs: Pending / Approved / Rejected / All Courses. Shows:
- 3 `stat-card` components: pending review count, approved this month, total live courses
- Pending review list — each entry shows course name, teacher name, department, lecture count, submission date, and action buttons: Preview (opens video player for the first lecture) / Approve / Reject (requires entering a rejection reason)
- Recently approved courses with enrollment counts
- Rejected courses list with rejection reason

---

### colleges/

#### `colleges.component.ts` — route: `/admin/colleges`
Manages registered institutions. Shows:
- 3 `stat-card` components: registered colleges, total students, total teachers
- Registered colleges list — each entry shows college name, city, student count, teacher count, course count, verification status `status-pill`
- Verified colleges have a "Manage" button; pending colleges have "Verify" and "Reject" buttons
- Teacher affiliation requests panel — teachers who have claimed a college affiliation and uploaded their ID. The admin can Approve or Reject each request.
- "Add College" button to manually register a new institution

---

### revenue/

#### `revenue.component.ts` — route: `/admin/revenue`
Financial analytics. Filter tabs: This Month / Last 3 Months / This Year / By College / By Course. Contains:
- 4 `stat-card` components: total revenue, external paid revenue, college subscription revenue, remedial fees revenue
- Monthly revenue trend bar chart — month-over-month bars
- Revenue by source bar chart: external sales vs college subscriptions vs remedial certification fees
- Top earning courses list with per-course enrollment count and paid student count

---

### payouts/

#### `payouts.component.ts` — route: `/admin/payouts`
Processes teacher royalty payouts. Contains:
- 3 `stat-card` components: total pending payout amount, amount paid this month, platform cut retained
- "Process All Pending" button at the top — triggers batch payout for all teachers above the ₹500 threshold
- Pending payout list — each teacher row shows: name, masked bank account number, course count, amount due, and an individual "Pay Now" button
- Payout history table — monthly entries with total amount paid and teacher count; "Export CSV" button for accounting

---

### reports/

#### `reports.component.ts` — route: `/admin/reports`
Platform-wide analytics. Filter tabs: Engagement / Growth / Course Quality / Revenue. Contains:
- 4 engagement KPI `stat-card` components: average session time, videos watched this month, quizzes attempted, new enrolments
- Engagement by department bar chart (shows which departments have most active students)
- Platform growth grid — new students, new teachers, external paid users, certificates issued — each with % change vs last month
- Scheduled reports panel — manage auto-generated weekly/monthly email reports sent to the admin email

---

### flagged/

#### `flagged.component.ts` — route: `/admin/flagged`
Moderation queue for AI-flagged content. Filter tabs: All / High Risk / Bullying / Spam / Suspicious. Shows:
- 3 `stat-card` components: open flags, resolved this month, AI detection accuracy %
- Each flagged item card shows: user name, content type (comment / DM / review), course name, time ago, risk level `status-pill`, AI confidence score, prior warning count
- High-risk items have a red border; content is hidden by default with a "View Full Post" button
- Action buttons per item: Blacklist User / Issue Warning / View Full Post / Dismiss

---

### sentiment/

#### `sentiment.component.ts` — route: `/admin/sentiment`
AI-powered sentiment analysis dashboard (Phase 2). Filter tabs: Platform-wide / By Course / By Teacher / By College. Contains:
- 3 `stat-card` components: total comments analysed, AI model confidence %, negative trend direction
- Overall sentiment score: 3 boxes showing Positive % / Neutral % / Negative %
- Sentiment by course type bars (Math/Science, Engineering, CS/IT, Remedial)
- Top negative keywords with occurrence counts (e.g. "difficult" → 234, "confusing" → 187, "audio issue" → 91)
- AI action recommendations — auto-generated suggestions (e.g. "Audio quality issues detected in Thermodynamics Lec 4–6 → Notify teacher")
- Teacher sentiment scores table — per-teacher positive % with trend notes (improving / declining)

---

### settings/ (admin)

#### `settings.component.ts` — route: `/admin/settings`
Platform-level configuration. Organised into sections:
- **Admin profile** — name, email (verified), 2FA status (enforced for all admins), password change
- **Platform toggles** — on/off switches for each major system feature:
  - AI abuse detection
  - AI question extraction
  - Sentiment analysis
  - Course approval required before going live
  - Teacher ID verification required
  - External student purchases enabled/disabled
  - Leaderboard public visibility
- **Payout configuration** — platform royalty cut % (default 20%), payout schedule date (default: 5th of each month), minimum payout threshold (default: ₹500)
- **Notification preferences** — high-risk flag alerts, new college application, course submitted for review, weekly digest email
- **Account tools** — audit log (read-only list of all admin actions with timestamps), manage admin accounts (add or remove admin users), sign out

---

### services/ (admin)

```
admin/services/
├── admin.service.ts
├── revenue.service.ts
├── moderation.service.ts
└── sentiment.service.ts
```

#### `admin.service.ts`
- `getUsers(filter)` — paginated user list with filters
- `warnUser(userId)` / `blacklistUser(userId)` / `restoreUser(userId)` — user moderation actions
- `getPendingCourses()` — courses awaiting approval
- `approveCourse(courseId)` / `rejectCourse(courseId, reason)` — course approval actions
- `getColleges()` — registered colleges list
- `verifyCollege(collegeId)` / `rejectCollege(collegeId)` — college verification
- `getAffiliationRequests()` / `approveAffiliation(requestId)` / `rejectAffiliation(requestId)` — teacher college claims

#### `revenue.service.ts`
- `getRevenueSummary(period)` — total and broken-down revenue figures
- `getRevenueChart(period)` — data for the monthly bar chart
- `getTopCourses()` — top earning courses list
- `getPendingPayouts()` — teachers due for payout
- `processPayout(teacherId)` / `processAllPayouts()` — trigger payout

#### `moderation.service.ts`
- `getFlaggedContent(filter)` — paginated list of flagged items
- `dismissFlag(flagId)` — marks a flag as resolved without action
- `issueWarning(userId, flagId)` — sends a warning and logs it
- `blacklistFromFlag(userId, flagId)` — blacklists and resolves the flag

#### `sentiment.service.ts`
- `getSentimentOverview(filter)` — overall positive/neutral/negative percentages
- `getKeywordTrends()` — top negative keywords with counts
- `getTeacherSentimentScores()` — per-teacher sentiment table
- `getAIRecommendations()` — AI-generated action suggestions for the admin

---

## 10. src/app/features/payment

```
features/payment/
├── checkout.component.ts
├── payment-success.component.ts
└── payment.service.ts
```

This is the Razorpay payment flow used when an external student purchases a course.

#### `checkout.component.ts`
The course purchase page. Shows an order summary (course title, teacher, price, platform fee breakdown). On clicking "Pay Now", it calls `payment.service.ts` to create a Razorpay order, then opens the Razorpay SDK payment modal. On successful payment, navigates to `/payment/success`.

#### `payment-success.component.ts`
Post-payment confirmation page. Shows the receipt (order ID, amount, course name) and a "Start Learning" button that navigates to `/student/courses/:id`. Also auto-enrolls the student in the course via `payment.service.ts`.

#### `payment.service.ts`
- `createOrder(courseId)` — calls the backend to create a Razorpay order and returns the `orderId`
- `verifyPayment(razorpayResponse)` — sends the Razorpay payment response to the backend for signature verification
- `enrollAfterPayment(courseId, paymentId)` — enrolls the student in the course once payment is verified

---

## 11. src/app/store

NgRx state management. Each feature domain has its own folder with four files following the standard NgRx pattern.

```
src/app/store/
├── auth/
│   ├── auth.actions.ts
│   ├── auth.effects.ts
│   ├── auth.reducer.ts
│   └── auth.selectors.ts
├── student/
│   ├── student.actions.ts
│   ├── student.effects.ts
│   ├── student.reducer.ts
│   └── student.selectors.ts
├── teacher/
│   ├── teacher.actions.ts
│   ├── teacher.effects.ts
│   ├── teacher.reducer.ts
│   └── teacher.selectors.ts
└── app.state.ts
```

### Pattern for every slice

| File | Purpose |
|---|---|
| `*.actions.ts` | Defines all NgRx actions for this slice. Each action is a strongly-typed event. |
| `*.effects.ts` | Handles side effects — API calls, navigation, and other async operations triggered by actions. |
| `*.reducer.ts` | A pure function that takes the current state and an action and returns the new state. |
| `*.selectors.ts` | Memoised selector functions that components use to read specific pieces of state. |

### `store/auth/`
- **Actions:** `login`, `loginSuccess`, `loginFailure`, `logout`, `refreshToken`, `setCollegeVerified`
- **Effects:** calls `auth.service.ts` login method, handles JWT storage on success, redirects by role
- **State:** `{ user, token, role, isCollegeVerified, loading, error }`
- **Selectors:** `selectCurrentUser`, `selectUserRole`, `selectIsAuthenticated`, `selectIsCollegeVerified`

### `store/student/`
- **Actions:** `loadCourses`, `loadCoursesSuccess`, `loadLecture`, `markLectureComplete`, `updateProgress`, `loadLeaderboard`
- **Effects:** calls `student.service.ts` and dispatches success/failure actions
- **State:** `{ enrolledCourses, currentLecture, watchProgress, leaderboard, loading }`
- **Selectors:** `selectEnrolledCourses`, `selectCurrentLecture`, `selectLeaderboard`

### `store/teacher/`
- **Actions:** `loadMyCourses`, `uploadLecture`, `uploadLectureProgress`, `submitCourseForReview`, `sendNudge`, `loadRoyalties`
- **Effects:** calls `teacher.service.ts` and `upload.service.ts`
- **State:** `{ myCourses, uploadProgress, royalties, loading }`
- **Selectors:** `selectMyCourses`, `selectUploadProgress`, `selectRoyalties`

### `app.state.ts`
The root state interface that combines all feature slices:
```typescript
interface AppState {
  auth: AuthState;
  student: StudentState;
  teacher: TeacherState;
}
```
Register feature reducers using `StoreModule.forFeature()` inside each feature module, and the root reducer with `StoreModule.forRoot()` in `AppModule`.

---

## 12. src/assets

```
src/assets/
├── icons/
├── images/
├── fonts/
└── i18n/
```

| Folder | Contents | Rule |
|---|---|---|
| `icons/` | SVG icon files and Lottie JSON animation files (`.json`) for empty states, loading screens, and the landing page hero | Only use Lottie animations here — do not import heavy animation libraries. SVG icons should be optimised (run through SVGO). |
| `images/` | Platform logo, hero images, default course thumbnail, default avatar image | Keep images compressed. Use WebP format where possible. Never commit uncompressed stock photos. |
| `fonts/` | Custom web font files (`.woff2`) if not loaded via Google Fonts CDN | If using Google Fonts CDN instead, this folder stays empty and fonts are loaded in `index.html`. |
| `i18n/` | Translation JSON files: `en.json` (English), `hi.json` (Hindi) | All user-facing strings should eventually be in these files. Use a translation key in templates (`{{ 'dashboard.greeting' | translate }}`) rather than hard-coding English strings. |

---

## 13. Root App Files

```
src/app/
├── app-routing.module.ts
├── app.component.ts
├── app.component.html
└── app.module.ts
```

#### `app-routing.module.ts`
The root router configuration. Defines the top-level routes:
```
/                   → LandingComponent (eagerly loaded)
/login              → AuthModule (lazy)
/register           → AuthModule (lazy)
/verify-college     → AuthModule (lazy)
/student/**         → StudentModule (lazy, guarded by auth.guard + role.guard STUDENT)
/teacher/**         → TeacherModule (lazy, guarded by auth.guard + role.guard TEACHER)
/admin/**           → AdminModule (lazy, guarded by auth.guard + role.guard ADMIN)
**                  → NotFoundComponent
```

#### `app.component.ts` + `app.component.html`
The root component. Its template is a single `<router-outlet></router-outlet>`. Do not add any other content here — all layout (sidebar, topbar) is handled inside each feature module's layout wrapper.

#### `app.module.ts`
The root Angular module. Imports:
- `BrowserModule`
- `BrowserAnimationsModule` (required by Angular Material)
- `CoreModule` (the one and only import of CoreModule)
- `StoreModule.forRoot(reducers)` (NgRx root store)
- `EffectsModule.forRoot([])` (NgRx effects root)
- `AppRoutingModule`

---

## 14. Rules for Contributors

Follow these rules to keep the codebase consistent and avoid duplicate work.

### Before writing any code

1. **Read this document.** Find the file that is responsible for what you need to build. In most cases, the file already exists — you just need to add to it.
2. **Check the models first.** Before writing any data structure, check `core/models/` to see if the interface already exists.
3. **Check shared components.** Before building a new UI component, check `shared/components/` to see if a reusable version already exists.
4. **Check services.** Before writing an API call, check the relevant `services/` folder — the method may already exist.

### File creation rules

| Situation | Action |
|---|---|
| New API call for an existing feature | Add a method to the existing service in `features/<role>/services/` |
| New UI component used in only one role | Create inside `features/<role>/<page>/components/` |
| New UI component used in 2+ roles | Create inside `shared/components/` |
| New colour, spacing, or design token | Add to `src/styles/_variables.scss` |
| New environment variable or API key | Add to both `environment.ts` and `environment.prod.ts` |
| New route | Add to `<role>-routing.module.ts` and update this document |
| New data shape from the API | Add an interface to the relevant file in `core/models/` |
| New NgRx state | Add action, effect, reducer update, and selector to `store/<slice>/` |

### Naming conventions

| Type | Convention | Example |
|---|---|---|
| Components | `kebab-case` folder, `PascalCase` class | `course-card/`, `CourseCardComponent` |
| Services | `camelCase` with `.service.ts` suffix | `student.service.ts`, `StudentService` |
| Models/Interfaces | `PascalCase` with `.model.ts` suffix | `course.model.ts`, `Course` interface |
| NgRx Actions | `camelCase` string description | `'[Student] Load Courses'` |
| SCSS variables | `$kebab-case` | `$brand-purple`, `$status-red` |
| Route paths | `kebab-case` | `/student/my-courses` |

### Do not

- Do not write raw hex colours in component SCSS files — use `_variables.scss` tokens
- Do not inject `HttpClient` directly in a component or feature service — use `api.service.ts`
- Do not call `localStorage` directly — use `storage.service.ts`
- Do not import `CoreModule` in any feature module
- Do not create a new component if an equivalent exists in `shared/components/`
- Do not store credentials, API keys, or URLs anywhere except `environment.ts` and `environment.prod.ts`
- Do not use `any` as a TypeScript type — use the interfaces from `core/models/`

---

*Last updated: June 2026 — LearnSphere v1.0 Pre-Development*