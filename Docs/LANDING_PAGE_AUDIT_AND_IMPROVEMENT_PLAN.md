# Landing Page — Audit & Improvement Plan

> Source: `learnSphere/frontend/learnsphere/src/app/features/landing/landing.component.{ts,html,scss}`
> Route: `src/app/app.routes.ts:67` — `{ path: '', component: LandingComponent, pathMatch: 'full' }`
> Status: **Audit only — no code changed.** Future-update reference.
> Last audited: 2026-10-04 (static, read-only + subagent cross-check)

---

## 1. Current-State Summary

| Area | Verdict |
|---|---|
| Component logic (`landing.component.ts:1-15`) | Empty shell — `export class LandingComponent {}` + `// Logic will go here if needed later!`. No `OnInit`, no service, no `HttpClient`, no `Observable`/`async`. Zero `{{ }}` / `*ngFor` in template. |
| Navigation | Majority of links are dead `span`/`div` with `cursor:pointer` only. Only `Log in / Sign up free / Start learning / Start teaching / Browse free previews` have working `routerLink`. |
| Data | 100% hardcoded: stats band, 3 trending cards, department browse bar, preview player timestamps. No call to `GET /api/courses/trending` or `GET /api/courses`. |
| Auth awareness | None. Public route with no guard; authenticated users still see login/signup CTAs; no redirect to role dashboard. |
| SEO / A11y | Single `h1`, `div.h2` sections, no meta/OG tags, `span`-as-link (no keyboard), fake video player, legacy `lucide-icon name` API. |
| Responsive / theme | One `@media` rule (hero only). ~60 inline `style="..."`. `.card/.h2/.sec-label/.sub/.divider/.check` used but not defined locally (global coupling). Dark `#12102e/#1e1a4a/#0d0b1e` ↔ light `#fff/#F8FAF8` zebra with undefined `hr.divider`. Several low-contrast pairs. |

---

## 2. Detailed Findings (file:line evidence)

### 2.1 Dead navigation — conversion leak

> No destination exists for several of these even if wired:
> `app.routes.ts:66-134` = `''`, `login/register/forgot-password`, `verify-certificate*`,
> `payment/*`, `student/*`, `teacher/*`, `admin/*`, `** NotFound`.
> Missing: `/courses`, `/departments`, `/colleges`, `/about`, `/contact`, `/privacy`, public `/leaderboard`.

| Location | Issue |
|---|---|
| `landing.component.html:10` | Nav wrapper has conflicting `style="display:flex;...;display:none;"` — permanently hidden, no mobile alternative. |
| `landing.component.html:11-14` | `Explore courses / Departments / For colleges / Teach with us` are `<span class="nav-link">` — no `routerLink`, no `(click)`, no `<a>`, not focusable. Hover only (`landing.component.scss:52-53`). |
| `landing.component.html:25-29` | `Browse: All departments / Computer Science / Mechanical / Electronics / Civil` — plain `span`s, no link. |
| `landing.component.html:127` | `View all 1,200+ courses →` — `span` with `cursor:pointer` only. |
| `landing.component.html:132,151,170` | 3 course `.card` `div`s with `cursor:pointer` — no `routerLink/(click)/tabindex`. |
| `landing.component.html:248-249` | Video play circle `div cursor:pointer` + CSS triangle — no `button/(click)/routerLink`. |
| `landing.component.html:262` | `Sign up to unlock all 24 lectures →` — plain `span`. |
| `landing.component.html:303` | `Register your college` `<button class="btn-primary">` — **only CTA without `routerLink`** (compare sibling `312: Start teaching` which has `routerLink="/register"`). Dead click. |
| `landing.component.html:243` | `Browse free previews` label says browse, target is `routerLink="/register"` — misleading. |
| `landing.component.html:333-335` | Footer `Explore courses / Departments / Leaderboard` — dead spans. |
| `landing.component.html:342-344` | Footer `About us / Contact / Privacy policy` — dead spans. |
| Working (keep): `18-19` (`a routerLink="/login|/register"`), `47-48` (`button routerLink="/register"` — works but should be `<a>`), `312` (`button routerLink="/register"`). | — |

### 2.2 Hardcoded content vs real backend

| Location | Hardcoded | Should be |
|---|---|---|
| `landing.component.html:61-65` | `1,200+ Courses live / 85+ Colleges / 40k+ Students / 4.6 ★ / ₹0` | Live counts: `GET /api/courses`, `GET /api/colleges`, enrollment aggregate, `GET /courses/:id/feedback/avg`. |
| `landing.component.html:127` | `View all 1,200+ courses` (duplicates magic number) | `totalCount` from API or remove number. |
| `landing.component.html:132-149` | `Data Structures & Algorithms / Prof. R. Sharma · VJTI / ★4.8 (312) · 1.2K / ₹799` | `GET /api/courses/trending?limit=3` row. |
| `landing.component.html:151-168` | `Thermodynamics — Complete / Prof. A. Kulkarni · COEP / ★4.6 (178) · 890 / ₹499` | Same. |
| `landing.component.html:170-187` | `Engineering Maths — GATE / Prof. M. Patel · IIT Bombay / ★4.9 (540) · 3.4K / ₹299` | Same. |
| `landing.component.html:26-29` | Static dept list | `DepartmentController.java:46 GET /{id}/courses` + `GET /api/departments`. |
| `landing.component.html:251-258` | Fake player `0:00 / 42:00`, `width:0%` bar, `Lecture 1 — Introduction to DSA` | Real `<video>`/YouTube embed of Lecture 1 or remove progress UI. |

Existing unused backend + service (do not re-invent):

- `learnsphere-backend/.../Controller/TrendingController.java:33` — `@GetMapping("/api/courses/trending")`, public (`SecurityConfig.java:83` `GET /api/courses/** permitAll`), ranked by live enrollment count, `?limit` param, returns `TrendingCourseDto(id,title,thumbnail,teacherName,teacherId,enrolledCount,rank)`.
- `learnsphere-backend/.../Controller/CourseController.java:27,35` — `GET /courses`, `GET /courses/{id}`.
- `features/teacher/services/teacher.service.ts:7-16,114-117` — `interface TrendingCourse` + `getTrendingCourses(limit?) => GET /courses/trending`. Never imported by landing (`landing.component.ts:1-4` imports only `CommonModule, RouterModule, LucideAngularModule` + 8 icons).
- `features/admin/courses/courses.component.ts:58` confirms `GET /api/courses` lists all courses.

### 2.3 No logged-in redirect

- `app.routes.ts:67` — no `canActivate`.
- `core/auth/auth.guard.ts:8-17` — only blocks unauthenticated; no inverse/already-authed guard.
- `landing.component.ts:13-15` — no `ngOnInit` + `AuthService.isAuthenticated/dashboardPathForRole` redirect.
- `core/auth/auth.service.ts:36-38,88-97` — `isAuthenticated` + `dashboardPathForRole() → /student|teacher|admin/dashboard` exists but unused on landing (used by `login:52`, `register:73`, `verify-college:70,81`, `not-found:40`, `role.guard:19`).
- Symptom: authed users still see `Log in / Sign up free / Start learning for free` (`html:18-19,47`).

### 2.4 Accessibility gaps

- Heading outline: single `h1` (`html:38-41`); section titles are `div.h2` (`72,125,198,230 w/ 32px override,275`) — no `h2/h3`.
- Buttons vs anchors: nav actions use `<button routerLink>` (`47-48,243,312`) — should be `<a>`; no `type="button"`/`aria-label`.
- Fake links: `11-14,25-29,127,333-335,342-344` — no `tabindex/role`, no `Enter/Space`, no `:focus-visible` (scss `52-53` styles hover only).
- Video trigger `248` — no `role="button"/tabindex/aria-label="Play preview"`.
- FAQ `278-289` — `div` Q/A, no `<button>/<details>/aria-expanded/heading`.
- Icons: `html:77,91,104,134,153,172,202,207,212,217` (`graduation-cap, globe, file-text, monitor, settings, ruler, search, user, play`) match imports in `ts:4`, but `name="..."` string API is legacy (current is icon-object/`[img]`); sizing inconsistent (`style="width:28px"` vs `[size]="15"` elsewhere e.g. quiz-builder); `monitor/ruler/globe` ambiguous vs renames; `check-circle-2` in `payment-success:15` already renamed `circle-check` upstream — version-bump risk. No `aria-hidden="true"` on decorative icons.

### 2.5 SEO gaps

- `src/index.html:3-8` — only `<title>LearnSphere</title>`, `charset/viewport/favicon`, fonts. No `description`, `og:*`, `twitter:*`, `canonical`, `theme-color`.
- `landing.component.ts` — no `Title/Meta` service use.

### 2.6 Responsive / maintainability gaps

- ~60+ inline `style="..."` (`html:3-358`, e.g. `3,4,18-19,60-65,89-90`); `landing.component.scss:1-78` defines only `.LS/.pill*/.hero-*/.nav-link/.btn-*`.
- `.card/.h2/.sec-label/.sub/.divider/.check` used (`76,72,118,52`) but undefined locally — global coupling; `hr.divider:118,193,224,268` has no style.
- Only responsive rule: `scss:25-27` (`@media max-width:768px` hero padding). Light grids have none. `296: minmax(400px,1fr)` CTA grid overflows viewports <400px. Fixed `padding:60px 28px/40px (69,120,195,226,270,298,307)` + `max-width:800/1000/1100 (34,60,75...)` without fluid tokens. `23: overflow-x:auto` scroller has no snap/labels.

### 2.7 Theme / contrast gaps

- Zebra without transition: dark `3 (#12102e header)`, `33 .hero-section (#12102e, scss:22)`, `59 (#1e1a4a stats)`, `298 (#12102e)`, `307 (#1e1a4a)`, `318 (#0d0b1e footer)` ↔ light `69 (#fff)`, `120 (#F8FAF8)`, `195 (#fff)`, `226 (#F8FAF8)`, `270 (#fff)`.
- Token bypass: `.LS:8-11` sets `color:var(--text-primary)` but hero/header hardcode `#fff/rgba(255,255,255,*)`; `btn-primary:56-57 var(--brand-primary)` vs inline `303: background:#fff;color:#12102e` (no hover/focus); `btn-outline-dark:63-68` is white-on-dark only (misnamed on light).
- Low-contrast small text on dark: `24: rgba(.5) 12px Browse:`, `26-29: rgba .5/.6 12px`, `52-54: rgba(.7) 13px checks`, `61-65 labels rgba(.5) 12px` — below 4.5:1.
- Light section risks: `233-241: #6B6880 14px on #F8FAF8` borderline; badges `135: #fff on rgba(255,255,255,0.15)`, `154: #9FE1CB on rgba(15,110,86,0.35)`, `173: #ED93B1 on rgba(153,53,86,0.35)` — translucent BGs unpredictable.

---

## 3. Improvement Plan (future work — prioritized)

### P0 — Make it functional (do first)

**P0-1. Wire real data (replace hardcodes).**
- [ ] Inject existing `TeacherService.getTrendingCourses(3)` (or new public `CourseService`) into `LandingComponent`; `OnInit` fetch with loading skeleton + error fallback to current static cards.
- [ ] Render trending with `*ngFor` + `async` pipe: thumbnail, title, teacher, rating, enrolled count, price/free badge. Link each card to `/courses/:id` preview or `/register?next=/student/courses/:id`.
- [ ] Replace stats band `61-65` with live values (`GET /api/courses`, college count, enrollment count) or remove numbers until API ready. Single source of truth — no duplicated `1,200+` in `127`.
- [ ] Replace department browse `26-29` with `GET /api/departments` list; each dept links to filtered catalog or register.
- Acceptance: no hardcoded course/stat in HTML; empty-state + error-state covered; works logged-out (permitAll).

**P0-2. Fix all CTAs (no dead clicks).**
- [ ] Top nav `11-14`: convert to `<a>` with real targets — `Explore courses → /register` (or new `/courses` public catalog), `Departments → anchor/#departments or /register`, `For colleges → /verify-college info or college section anchor`, `Teach with us → /register?role=TEACHER`. Unhide `10` (remove `display:none`) + add mobile menu.
- [ ] `127 View all courses`, `132/151/170` cards, `248` play, `262` unlock line: make real links/buttons with `tabindex` + keyboard.
- [ ] `303 Register your college`: add `routerLink` (e.g. `/register` with college intent or dedicated `/colleges` page when built). Fix `243` label↔target mismatch (`Browse free previews` should go to preview anchor, not `/register`, or relabel to `Sign up to browse`).
- [ ] Footer `333-335,342-344`: point to real routes or remove until pages exist (`About/Contact/Privacy/Leaderboard`). Never ship `cursor:pointer` spans.
- [ ] Decide catalog strategy (needed before wiring): (a) new public `/courses` + `/courses/:id/preview` routes, or (b) point everything at `/register`. Record decision here.
- Acceptance: every clickable element navigates or is removed; keyboard `Tab+Enter` reaches all CTAs.

**P0-3. Auth-aware landing.**
- [ ] `ngOnInit`: `if (authService.isAuthenticated) router.navigate([dashboardPathForRole(currentUser.role)])`.
- [ ] Alternatively add `alreadyAuthedGuard` and apply to `''`, `login`, `register`.
- [ ] When authed and staying (e.g. preview), swap header `18-19,47` to `Go to dashboard`.
- Acceptance: logged-in student/teacher/admin hitting `/` lands on their dashboard; no login/signup flash.

### P1 — Trust + conversion (do next)

**P1-4. SEO.**
- [ ] `Title` + `Meta`: title `LearnSphere — College-Verified Courses …`, `description`, `og:title/description/image`, `twitter:card`, `canonical`, `theme-color` in `index.html` + per-route `Title/Meta` update in landing.
- [ ] Semantic headings: `h1` hero only; sections → `h2`; card titles → `h3`.
- [ ] FAQ `278-289` → `<details>/<summary>` or accessible accordion + FAQ JSON-LD.
- Acceptance: Lighthouse SEO ≥95; headings outline clean; link preview renders.

**P1-5. Accessibility.**
- [ ] Anchors for navigation, `aria-label` on icon-only/play controls, `aria-hidden="true"` on decorative `lucide-icon`.
- [ ] Migrate `lucide-icon name="…"` to current supported API; pin/verify icon names (`monitor/ruler/globe`, `check-circle-2→circle-check` elsewhere); consistent sizing via inputs not inline `width`.
- [ ] `:focus-visible` styles for all interactive elements; FAQ `aria-expanded`; video trigger `role="button" tabindex="0"` + `Enter/Space`.
- Acceptance: full keyboard pass; axe/Lighthouse a11y ≥95.

**P1-6. Honest preview player.**
- [ ] Either embed real Lecture-1 (HTML5 `<video>` or YouTube) with working play + time/progress, or delete fake `0:00/42:00 + 0%` UI (`251-258`) and `Try before you enroll` claims `233-241` until real.
- Acceptance: no fake progress/timestamps shipped.

### P2 — Polish (do last)

**P2-7. Styles + responsive.**
- [ ] Extract inline styles to `landing.component.scss` (or shared tokens); define `.h2/.sec-label/.card/.divider/.check` locally; remove `hr.divider`-without-style abrupt cuts (use gradient/section padding).
- [ ] Fix `296` grid `minmax(400px,1fr) → minmax(min(100%,320px),1fr)`; add breakpoints for `69,120,195,226,270` sections; fluid `clamp()` padding; scroll-snap browse bar `23` with accessible labels; mobile nav for `10`.
- [ ] `prefers-reduced-motion` support; remove `transition:transform 0.2s` hover-only cards without focus equivalent.
- Acceptance: 320px/768px/1440px pass, no horizontal scroll; no `style=""` left in template.

**P2-8. Contrast + theme tokens.**
- [ ] Bump dark-section small text to `rgba(255,255,255,.75)+`; solidify badges `135,154,173`; verify `#6B6880 on #F8FAF8` pairs (darken text or bg).
- [ ] Replace hard hex with `var(--bg-*, --text-*, --brand-*)`; fix `303` white-button override with proper variant + hover/focus; rename or rescope `btn-outline-dark` (white-only) vs light-section outline.
- Acceptance: WCAG AA 4.5:1 body text (3:1 large); dark↔light transitions intentional.

---

## 4. Execution Checklist (for build mode)

1. [ ] P0-1 data wiring + skeleton/error states
2. [ ] P0-2 CTA/link audit fix + catalog-route decision recorded
3. [ ] P0-3 auth redirect / header swap
4. [ ] P1-4 SEO + headings + FAQ JSON-LD
5. [ ] P1-5 a11y pass (keyboard, focus, icons, FAQ)
6. [ ] P1-6 preview player real or removed
7. [ ] P2-7 styles/responsive extraction
8. [ ] P2-8 contrast/token pass
9. [ ] Final: `ng serve` zero Sass warnings, Lighthouse (Perf/A11y/SEO) snapshot attached here

---

## 5. Open Decisions (fill before building)

- [ ] Catalog: new public `/courses` (+ `/:id/preview`) routes vs point all CTAs at `/register`?
- [ ] College CTA: dedicated `/colleges` page vs `register?intent=college`?
- [ ] Stats source: which backend aggregates are canonical (enrollments, colleges, ratings)?
- [ ] Preview: HTML5 video vs YouTube embed for Lecture 1?

*Related: `TrendingController.java`, `CourseController.java`, `SecurityConfig.java:83`, `teacher.service.ts:114-117`, `app.routes.ts`, `auth.service.ts:36-38,88-97`, `index.html:3-8`.*
