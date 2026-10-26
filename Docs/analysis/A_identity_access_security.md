# A — Analysis: identity, access & security

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** `spec §2`, `spec §3.1`, `spec §5` rows 1–4, `spec §9` Q-01/Q-02/Q-09/Q-11 · `AG §1–2`, `AG §3` · `REF p3–6` · backend `Entity/{Department,Role,UserAccount,UserRole}`, `Security/*`, `Config/SecurityConfig`, `Service/impl/AuthServiceImpl`, `Controller/{Auth,UserAccount,UserRole,Role,Department}Controller`, `Config/DataSeeder` · Angular `core/auth/*`, `core/services/storage.service.ts`, `core/interceptors/*`
**Date of analysis:** 2026-10-22

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n |
| `AG §n` | root `AGENTS.md` section n |
| `ARCH` / `SYS §n` / `BE` / `FE` | `Docs/Architecture.md` · `Docs/learnsphere_system_design.md` · `Docs/Backend.md` · `Docs/Frontend.md` |
| `code <path>:<line>` | implementation evidence |
| `derived` | analyst inference, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| `REF p3–6` | Read in full: Department, Role, UserAccount, UserRole (fields, rules, endpoints, notes). |
| `AG §1–3` | Read in full. |
| `spec §2, §3.1, §5, §9` | Read in full. |
| Backend | `SecurityConfig.java`, `JwtService.java`, `JwtAuthFilter.java`, `CustomUserDetails.java`, `CustomUserDetailsService.java`, `AuthServiceImpl.java`, `AuthController.java`, all four identity entities, `DataSeeder.java` (roles + demo users). |
| Frontend | `app.routes.ts`, `auth.service.ts`, `auth.guard.ts`, `role.guard.ts`, `storage.service.ts`, `error.interceptor.ts`, `user.model.ts`. |
| Does NOT exist | refresh-token store/table, login rate limiter, audit log, email verification, `assignedBy` column, dedicated deactivation flag. |

---

## 1. The reference contract

1. **Department** — PK `department_id` is `Integer` (not `Long`); `name` `VARCHAR` unique/not null; `created_at` immutable via `@PrePersist` (`REF p3`; `AG §1`).
2. **Role** — PK `role_id` `Integer`; `name` unique with values `ADMIN`, `INSTRUCTOR`, `STUDENT`; roles are seeded, not user-created (`REF p4`; `AG §1`).
3. **UserAccount** — table `user_account`, PK `user_id` Long, `email` unique, `password` BCrypt and `@JsonIgnore`, `dept_id` FK, `is_active` default true, `created_at`, `last_login`. Deactivated accounts are rejected at login; `lastLogin` is written on every successful login (`REF p5`; `AG §2`).
4. **UserRole** — table `user_role`, unique `(user_id, role_id)`, `assigned_at` immutable, admin-only writes (`REF p6`; `AG §2`).
5. **JWT** — access token 15 min with `userId`, `roles`, `dept_id`; refresh token 7 days in HttpOnly cookie or DB; Angular keeps the access token **in memory only** (`AG §3`; `REF p5` note).
6. **Endpoints required** — `/api/auth/{register,login,refresh,logout}`, `/api/users/me`, `/api/users/{id}`, `PATCH /api/users/{id}/status`, `/api/users/{id}/roles…` (`spec §2`).

---

## 2. What the implementation actually does

### 2.1 Entities diverge from the contract in type and name

7. `Department.departmentId` is `Long` (`code Entity/Department.java:16`), not `Integer`; the table is `departments`, not `department` (`:14`). `AG §1` explicitly warns "`departmentId` is `Integer` (not `Long`)".
8. `Role.roleId` is `Long` (`code Entity/Role.java`), table `roles`; the reference requires `Integer` and table `role` (`REF p4`).
9. `UserAccount` is mapped to `users`, not `user_account` (`code Entity/UserAccount.java:14`), and replaces `is_active` with `Status status ∈ {ACTIVE, FLAGGED, BLACKLISTED}` (`:33`). There is no `lastLogin`; the equivalent is `lastActiveAt` (`:40`). Extra fields not in the reference: `college`, `avatarUrl`, `bio`, `twoFactorEnabled`, `bankAccountLinked`, `royaltyBalance`, `attendanceScore`, `leaderboardPoints` (all in `UserAccount.java`). **derived:** these extras are legitimate product needs (preserved features, `AG §4`), but they must be documented as extensions, not read as reference fields.
10. `UserRole` is mapped to `user_roles` and has **no unique constraint** on `(user_id, role_id)` (`code Entity/UserRole.java:6–20`); the reference calls it mandatory (`REF p6`; `spec §5` row 4).

### 2.2 Registration chooses its own role

11. `RegisterRequest` carries a role; `AuthServiceImpl.register` assigns exactly that role without an approval step (`code Service/impl/AuthServiceImpl.java:60–105`). `RUN` §5 tells the user to "register once as STUDENT, once as TEACHER, once as ADMIN", so this is intentional for the demo. **derived:** in a real deployment this is privilege escalation; a self-registered user can become `ADMIN`.
12. Registration creates the `UserRole` row directly (`AuthServiceImpl.java:101–103`), so `POST /api/users/{id}/roles` is not the only write path despite `REF p6`.

### 2.3 Login and deactivation

13. Login authenticates through Spring's `AuthenticationManager` (`AuthServiceImpl.java:111–127`) and then writes `lastActiveAt`, not `lastLogin`.
14. `CustomUserDetails.isEnabled()` always returns `true`; `isAccountNonLocked()` returns false only for `BLACKLISTED` (`code Security/CustomUserDetails.java:44–56`). Therefore `FLAGGED` accounts still authenticate. There is no `is_active` boolean and no general "deactivated" state. **derived:** `PATCH /api/users/{id}/status` can only blacklist, which is a stronger action than the spec's deactivate.
15. Passwords are BCrypt-encoded (`SecurityConfig.passwordEncoder()`), and `AuthServiceImpl.login` calls `validatePassword` before authentication. **derived:** password strength is enforced server-side at registration/reset, not at login.

### 2.4 JWT is a single long-lived access token

16. `app.jwt.expiration-ms=86400000` (`code application.properties:18`) — a **24-hour** token, not 15 minutes (`spec §7`; `AG §3`).
17. Claims are `userId` and `roles` only (`code Security/JwtService.java:32–40`); **no `dept_id`** claim, so department-scoped authorization cannot be done from the token.
18. `/api/auth/refresh` accepts a token and re-issues from its email (`code Service/impl/AuthServiceImpl.java:206–219`). It is not a refresh token: an expired access token cannot be parsed, and the response returns `AuthResponse(newToken, newToken, …)`. There is no rotation, no revocation store, and `/api/auth/logout` returns a message without invalidating anything (`AuthController.logout`).
19. The signing secret has a literal default in `application.properties:17` (`change-this-super-secret-key-…`). **derived:** if deployed unchanged, anyone with the repo can forge tokens.

### 2.5 Frontend stores the token exactly where the hard rule forbids

20. `StorageService` reads/writes `localStorage` under `ls_token` and `ls_user` (`code core/services/storage.service.ts:4–31`). `AuthService.persistSession` calls it on login/register (`auth.service.ts:118–122`). This directly violates `AG §3`: "Never store access tokens in `localStorage`".
21. `authGuard` checks `authService.isAuthenticated`, which is `!!storageService.getToken()`; `roleGuard` compares `currentUser.role` against route data (`core/auth/*`). The interceptor logs the user out on any 401 (`error.interceptor.ts:18–21`). **derived:** token expiry produces a hard logout with no silent refresh.
22. `UserRole` in the client is `STUDENT | TEACHER | ADMIN`; the UI and routes are built around `TEACHER`, while the reference says `INSTRUCTOR` (`spec §9` Q-01).

### 2.6 Authorization is role-based at the URL level only

23. `SecurityConfig` permits `GET /api/courses/**`, `GET /api/colleges`, `/api/chatbot/**`, `/api/ai/**`, `/uploads/**`, `/ws/**` and `/internal/**`; requires `ROLE_ADMIN` for `/api/admin/**` and `ROLE_TEACHER` for `/api/teacher/**`; everything else only needs authentication (`code Config/SecurityConfig.java:77–97`).
24. Curriculum writes do not live under `/api/teacher`: `POST /api/courses`, `POST /api/courses/{id}/modules`, `POST /api/modules/{id}/contents`, `POST /api/modules/{id}/assignments` and most test writes are only "authenticated" (`code Controller/{Course,Module,Content,Assignment,Test}Controller`). `ModuleController` has no principal parameter at all. **derived:** any logged-in student can create modules, content, assignments and tests, and can mutate any course's curriculum. This is the highest-severity authorization gap in the system.

### 2.7 Governance features that do exist

25. Admin user governance is implemented: `GET /api/users/{id}`, `PATCH /api/users/{id}/status`, role assignment/removal (`code Controller/UserAccountController.java`; `Controller/UserAdminController.java` for activity/top-teachers).
26. Departments and roles have CRUD controllers (`DepartmentController`, `RoleController`, `UserRoleController`), and `DataSeeder` seeds `STUDENT`, `TEACHER`, `ADMIN` plus demo users with `Password@123` (`code Config/DataSeeder.java:49–51, 128–141`).
27. Teacher college claims create an `AffiliationRequest` instead of directly attaching the college; students attach directly (`AuthServiceImpl.verifyCollege`, `:161–186`).

---

## 3. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|
| A1 | `department_id`/`role_id` are `Long` and singular table names are plural in the running schema | DTO/ID consistency; `spec §5` says Integer. Either migrate or formally record the mapping (A-02/A-03) |
| A2 | No unique constraint on `user_roles (user_id, role_id)` | duplicate roles break `rolesOf()` assumptions and authorization |
| A3 | Registration lets the user pick `ADMIN`/`TEACHER` | privilege escalation; must be gated in non-demo profiles |
| A4 | Deactivation model is `status`, not `is_active`; `FLAGGED` can log in; `isEnabled()` is always true | AC-01 cannot be proven; account governance inconsistent |
| A5 | 24-hour JWT, no `dept_id`, no refresh token, no revocation, hard-coded default secret | violates `AG §3`; account takeover surface |
| A6 | Access token persisted in `localStorage` | violates the explicit hard rule; XSS exfiltrates the session |
| A7 | Curriculum/test writes require only authentication, not ownership/role | students can author or mutate courses |
| A8 | `assertOwner` treats "course has no teacher" as permission to claim it, and a null principal returns early | ownership edge cases are exploitable |
| A9 | Password reset logs the token instead of sending email | acceptable for demo, not for production (ROAD: deferred SMTP) |
| A10 | No login rate limiting / lockout / audit log | brute force and insider actions are invisible |

## 4. Gaps — what the sources do NOT provide

- **NO FIELD** `is_active` in the running schema; only `status` (ACTIVE/FLAGGED/BLACKLISTED).
- **NO TABLE** for refresh tokens or revoked tokens.
- **NO COLUMN** `assigned_by` on `user_role` (`REF p6` note confirms it does not exist).
- **NO CONTRACT** for role approval, teacher verification SLA, or admin self-protection (can an admin blacklist another admin?).
- **NO REQUIREMENT** for password policy beyond "validated" (minimum length is in code, not in the sources).
- **NO DEFINITION** of what `FLAGGED` means for login/authorization.

## 5. Open questions

| ID | Question | Feeds |
|---|---|---|
| A-Q1 | Is `TEACHER` the runtime name for the reference's `INSTRUCTOR`, or should roles be renamed? | `spec §9` Q-01 → A-01 |
| A-Q2 | Are singular table names mandatory, or is the running plural schema accepted? | Q-02 → A-02 |
| A-Q3 | Does "deactivated" mean `BLACKLISTED`, and may a `FLAGGED` user sign in? | A-23 (`Q-09/A-09` covers tokens only) |
| A-Q4 | Is self-service role selection a demo-only feature? | Q-11 → A-11 |
| A-Q5 | Should the access token move to memory now, or only when refresh tokens land? | Q-09 → A-09 |
