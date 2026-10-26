# LearnSphere AI - Audit Report

**Audit date:** 2026-10-24  
**Scope:** student, teacher and admin profiles; profile/settings actions; invoice/receipt generation; payment history and status handling; My Courses, Video Lectures and the course player; related backend and payments-microservice endpoints.

**Method:** static review of the Angular clients, Spring Boot APIs, payments microservice, route/guard definitions and shared navigation. No production data was changed during the audit. Existing uncommitted work in the repository was treated as the current baseline and was not reverted.

**Severity:**  
- **P0** = security or money-integrity issue that can be abused  
- **P1** = user-visible incorrect behaviour or broken core flow  
- **P2** = incomplete/fragile UI or data presentation  
- **P3** = maintainability or polish

---

## Executive summary

The requested areas contain a mixture of real integrations and placeholder implementations.

1. **Receipt PDFs exist but are not production-safe.** The generator uses Unicode characters that the PDF's standard Helvetica font cannot render (`₹`, `✓`, `•`) and hard-coded coordinates. The transaction/payment ID is never passed from purchase history, the timestamp is missing, and the receipt number is fabricated. The result is the broken/mangled layout described by the user.
2. **Payment history is not authoritative.** The main backend `getHistory` creates fake `SUCCESS` payments for paid enrolments that have no payment row, and the Angular purchases page then overwrites every main-history row with `status: 'PAID'`. Failed attempts are also discarded when a later paid attempt exists. Pending, failed and successful transactions therefore cannot be trusted.
3. **My Courses and Video Lectures are conflated.** My Courses includes an "Explore more courses" catalog, while the sidebar's "Video Lectures" entry reuses the player and silently opens the first enrolled course. There is no enrolled-only continue/resume surface and no all-courses Udemy/Coursera-style catalog.
4. **Teacher and admin profiles are partly or wholly fake.** The teacher profile uses hard-coded department, bio and statistics; its save button only changes local UI. The admin profile saves locally and shows toasts for password/avatar actions without calling an API. Student profile data is mostly real, but its source/template contains mojibake and some placeholders.
5. **Several security problems affect the same data.** These include unrestricted legacy `/api/users/**` endpoints, an unauthenticated/replayable payment simulation endpoint, a verification endpoint that continues after signature failure, a trusted internal-enrolment endpoint that writes payments/royalties on every call, and JWTs persisted in `localStorage`. These are documented below; only the items required for the requested fixes are changed in this pass.

---

## A. Invoice / receipt PDF

### A1. Unsupported Unicode glyphs break the PDF (P1)

`learnSphere/frontend/learnsphere/src/app/core/services/pdf-export.service.ts`

- `generateStudentReceipt` uses `₹`, `✓` and `•` with `helvetica`. jsPDF's built-in Helvetica is a WinAnsi font and does not contain the rupee sign or check mark. In practice these are rendered as missing glyphs or shifted text, producing the reported broken layout.
- Several source strings are already mojibake (`�,1`, `�o"`, `�?�`) before the PDF is even generated.
- The teacher statement generator has the same problem.

### A2. Hard-coded layout and no wrapping (P1)

- The header, boxes, table columns and footer use fixed millimetre coordinates (`tableY = 114`, `margin + 105`, `footerY = 240`).
- Course titles, department names and e-mail addresses are drawn without `splitTextToSize`/`maxWidth`; long values overlap the amount column or leave the page.
- The "amount" column is right-aligned at `pageWidth - margin - 5`, while the department is drawn at `margin + 105`; there is no collision protection.
- The PDF always assumes one line item and a fixed table row height.

### A3. Missing transaction identity and timestamp (P1)

- `ReceiptData` has an optional `paymentId`, but `PurchasesComponent.downloadPdfReceipt()` never supplies it.
- `PaymentResponseDto` already exposes `gatewayPaymentId` and `paidAt`, but purchases map them away.
- The PDF header only receives a date string; it does not print the payment time.
- The receipt number is fabricated as `INV-${id + 1000}` in both the modal and the PDF. It is not related to a real transaction.
- The receipt status is hard-coded to `PAID` even if the row is not paid.

### A4. Misleading invoice wording (P2)

- The document is labelled "TAX INVOICE / RECEIPT" and claims "Taxes (GST Included)" and "₹0.00", although no GST breakdown, GSTIN or invoice series exists.
- For a platform receipt this should be a **payment receipt**, or a proper tax invoice with real tax data. This pass uses a payment receipt and avoids claiming tax details that are not present.

### A5. Receipt modal/print fidelity (P2)

- The modal shows `createdAt`, not `paidAt`, and does not show the gateway payment ID.
- `printReceipt()` calls `window.print()` on the whole page; the print CSS hides everything except `#printable-receipt`, but the modal itself is not designed as a print document. Long content can be clipped.
- The receipt modal uses green/purple status colours while the PDF requirement is black and white. The print output and the downloaded PDF can look different.

### A6. Receipt availability after real checkout (P2)

- The real Razorpay flow lives in `course-detail.component.ts`.
- After verification it shows a success toast but does not navigate to a confirmation/receipt surface or offer a receipt link.
- A receipt can only be found later under `/student/purchases`.
- A second, legacy simulated checkout exists at `/payment/checkout/:courseId`; it is not the Razorpay flow.

---

## B. Payment history and status correctness

### B1. Backend fabricates successful payments (P1)

`learnsphere-backend/.../Service/impl/PaymentServiceImpl.java`, `getHistory`

- For every paid enrolment with no payment row, the method creates a new `Payment` with:
  - `status = SUCCESS`
  - a synthetic `gatewayOrderId`
  - a synthetic `gatewayPaymentId`
  - `paidAt = enrolledAt`
- These rows are persisted and later returned as if a gateway had confirmed them. This is the main reason the history can claim payments that never happened.
- The reconciliation is applied on every history read, so the fake rows become permanent database records.

### B2. Frontend overwrites every main-history status with PAID (P1)

`learnSphere/frontend/learnsphere/src/app/features/student/purchases/purchases.component.ts`

```ts
status: 'PAID',
```

The mapper ignores `p.status`. A main-backend `PENDING` or `FAILED` payment is displayed as paid, and the pending/failed filters can never see it.

### B3. Course-level consolidation hides failed attempts (P1)

- `consolidateAndEnrich` stores one row per `courseId`.
- Paid rows are inserted first, then all other rows for the same course are skipped.
- A user who had a failed attempt and then a successful attempt loses the failed attempt entirely.
- Two genuinely separate transactions for the same course are collapsed into one.

### B4. Different status vocabularies (P1)

- Main `Payment`: `PENDING | SUCCESS | FAILED`
- Microservice `Order`: `CREATED | PAID | FAILED`
- Angular `Order`: `CREATED | PAID | FAILED`

The ad-hoc normalisation only runs on microservice rows. Main rows are not normalised in the same way. The UI needs one display vocabulary.

### B5. Amounts use different units (P2)

- Main `Payment.amount` is rupees (`BigDecimal`).
- Payments microservice `Order.amount` is paise (`Integer`).
- `PurchasesComponent` converts main amounts to paise, but totals, receipt generation and PDF formatting each divide again. Any new source that returns the wrong unit will produce a 100x error.

### B6. Timestamp and transaction ID are lost (P2)

- Purchases uses `p.paidAt || p.createdAt` as `createdAt`, but does not keep either field separately.
- `gatewayPaymentId` is not mapped into `OrderWithCourse`.
- The result is an order history that cannot show the actual capture time or the payment reference.

### B7. Microservice verification accepts invalid signatures (P0)

`learnsphere-payments-service/.../controller/OrderController.java`, `verifyPayment`

- `Utils.verifyPaymentSignature(...)` is wrapped in `try/catch`; on failure the code only logs and continues.
- The order is then marked `PAID`, a payment is recorded, and `/internal/enroll` is called.
- A caller who can reach the endpoint can present an arbitrary order/payment pair and still be enrolled.

### B8. Payment simulation is unauthenticated and replayable (P0)

- `POST /api/payments/orders/simulate-success` has no auth, ownership or signature check.
- It marks any known order `PAID`, writes a payment, and enrolls the user.
- Replaying it can create repeated payment rows and repeated internal-enrolment calls.
- `GET /api/payments/orders/{userId}` is also unauthenticated and returns another user's orders.

### B9. Internal enrolment trusts the caller and is not idempotent (P0)

`learnsphere-backend/.../Controller/InternalController.java`

- `/internal/enroll` requires `X-Internal-Key`, but the security config separately permits `/internal/**`; the only protection is the shared key.
- The amount can be supplied by the request body and is used for the payment record and teacher royalty.
- There is no check for an existing payment/order ID. Every call creates a new `Payment` and credits another royalty, even if the student is already enrolled.
- The webhook calls it without passing the Razorpay payment ID, so the main ledger cannot store the real transaction reference.

### B10. Main simulated payment rail is separate from real checkout (P2)

- `PaymentServiceImpl.checkout/verify` creates locally simulated `order_...` and `pay_...` IDs.
- The real student checkout uses the payments microservice and Razorpay.
- Both write to the same `payments` table with different assumptions. Reports and history can mix simulated and real transactions.

### B11. No server-side user filter on microservice order lookup (P0/P1)

- `GET /api/payments/orders/{userId}` takes a path user ID and returns that user's orders without an authenticated principal or ownership check.
- This leaks other students' course/order data and is unnecessary because the main backend already knows the authenticated user.

### B12. Missing failed-payment write-back to the main ledger (P2)

- A Razorpay failure is recorded only in the microservice `orders` table.
- The main `payments` table usually has no failed row, so the main history API cannot show the failure by itself.
- The UI currently compensates by merging microservice orders, but that merge must preserve failures rather than discard them.

---

## C. Student course surfaces and navigation

### C1. My Courses is not enrolled-only (P1)

`learnSphere/frontend/learnsphere/src/app/features/student/courses/courses.component.html`

- The page is titled "My Courses", but after the enrolled list it renders an "Explore more courses" catalog.
- `CoursesComponent.loadExplore()` also runs on every search.
- This conflicts with the desired model: My Courses = enrolled only; a separate catalog = all available courses.

### C2. No direct continue/resume player path (P1)

- `CourseCardComponent` links only to `/student/courses/:id` (the course landing/curriculum page).
- The course detail page has a "Continue Learning" button, but My Courses itself does not.
- There is no route such as `/student/courses/:courseId/player` that resolves the next/first lecture.
- The player only handles `courseId + lectureId`; if only `courseId` is present it falls back to the first enrolled course.

### C3. "Video Lectures" is not a catalog (P1)

`app.routes.ts`:

```ts
{ path: 'student/lectures', component: StudentLectures }
```

- The sidebar item "Video Lectures" points at the full lecture workspace.
- `LectureComponent.loadDefaultContext()` picks the first enrolled course with lectures.
- There is no all-courses catalog surface, no catalog search/filter, no course cards with price/enrolment state, and no Udemy/Coursera-style browse experience.

### C4. Player access is not clearly enrollment-protected (P1)

- `LectureComponent.loadCourseContext()` calls `getPublicCourse()` and `getCourseLectures()`.
- The backend permits `GET /api/courses/**` without authentication.
- The route guard checks only that the user has the `STUDENT` role; it does not check enrollment.
- A student can potentially construct a player URL for a course they have not purchased/enrolled in and fetch lecture metadata/video URLs.
- The course detail page hides the play buttons until `isEnrolled`, but that is only a client-side check.

### C5. Route/component duplication (P2)

- Both `/student/courses/:courseId/lectures/:lectureId` and `/student/courses/:courseId/lecture/:lectureId` map to the player.
- `/student/lectures` also maps to the player and uses a hidden fallback.
- This makes "where does Continue take me?" ambiguous.

### C6. Catalog presentation issues (P2)

- `ExploreSectionComponent` uses malformed currency text (`'�,1'`) and dark-only inline styles.
- Search is wired to the catalog but not to a standalone catalog page.
- Catalog cards do not distinguish "enrolled" from "available".
- Free enrolment from the old explore section immediately enrolls; paid cards go to the detail page. This is reasonable, but the contexts are conflated.

### C7. Course detail progress is not the same as resume position (P2)

- The detail page uses the first lecture in the curriculum for "Continue Learning" (`lectures[0]`), not the last/incomplete lecture.
- The player's `loadDefaultContext` also does not resume a specific course or last position.

### C8. Minor UI/data defects (P3)

- `CourseCardComponent` and related templates contain mojibake separators and check marks.
- `LectureComponent.totalCourseDuration` estimates `lectures.length * 15` instead of summing real durations.
- Course list filters include `LIVE` and `ARCHIVED` even though enrolled courses are usually `LIVE`.
- The My Courses empty state points to the catalog section that is being removed.

---

## D. Profiles, settings and account screens

### D1. Student profile is mostly real but not clean (P2)

`features/student/profile`

- Loads `GET /users/me`; saves `PUT /users/me` with `fullName` and `bio`. This is the correct pattern.
- Stats are derived from real fields (`leaderboardPoints`, `attendanceScore`, college verification, status).
- Problems:
  - Template/source contains mojibake for separators, check marks, ellipses and placeholders (`A�`, `�?"`, `�o"`).
  - There is no avatar upload/save despite the User model supporting `avatarUrl`.
  - Empty bio is saved as `''`; this is acceptable but the patch semantics are not explicit.
  - No e-mail change or 2FA action is implemented (links to settings).
  - `saved` toast does not report failures with the same prominence as successes.

### D2. Teacher profile is a static mock (P1)

`features/teacher/profile`

- `detail` is hard-coded to "Mathematics Department · MH College of Engineering".
- `bio` is hard-coded.
- `stats` are hard-coded: 6 courses, 1,248 students, 4.8 rating, ₹18.4k.
- `name` is a getter and is bound with `[(ngModel)]="name"` while editing; this is not a writable model.
- `save()` only sets `editing = false` and shows a toast. No API call.
- Avatar "+" button has no handler.
- "Change password" button has no handler.
- The teacher dashboard is also populated with static KPI numbers and course rows.

### D3. Admin profile is partially real and partially fake (P1)

`features/admin/profile`

- Display name/e-mail come from the cached auth snapshot, not a fresh profile fetch.
- `detail` and `bio` are hard-coded.
- Stats are partly real (`ReportsService.getSummary`, `AdminService.getColleges`) but "Platform Health 99.9%" is invented.
- `save()` does not call the API; it only closes edit mode, shows a toast, and leaves the local fields dirty.
- `changePassword()` only displays "reset instructions dispatched" although no request is made.
- `onAvatarUpload()` only displays a toast; no file picker or upload.
- The admin user "Inspect" modal shows only summary fields already loaded in the list; it does not fetch the account detail/roles/history.

### D4. Settings screens are placeholders (P2)

- Student settings: toggles/save only update local state and show "Changes saved"; sign-out button has no click handler.
- Teacher settings: same local-only save; payout account is hard-coded (`SBI ...4821 Verified`); sign-out button has no click handler.
- Admin settings: toggles/save are local-only; "Audit log" and "Manage admins" only display informational toasts.
- None of these preferences are persisted or loaded from the backend.

### D5. Admin user management is mostly real, but incomplete (P2)

- List, status changes (ACTIVE/FLAGGED/BLACKLISTED), activity summary and college data are wired to `/api/admin/**` endpoints.
- "Add Account" only displays an informational toast.
- The "Inspect" drawer does not show roles, recent payments, courses or audit history.
- Role assignment UI is absent even though the backend has role endpoints.

### D6. Legacy user API is over-exposed (P0)

`UserAccountController`

- `GET /api/users` returns all `UserAccount` entities to any authenticated user.
- `GET /api/users/{id}`, `POST /api/users`, `PUT /api/users/{id}` and `DELETE /api/users/{id}` are also available to any authenticated user.
- `PATCH /api/users/{id}/status` can change another user's status (including blacklisting) without an admin check.
- `POST /api/users/{id}/roles` and `DELETE /api/users/{id}/roles/{roleId}` allow role changes without an admin check.
- `PUT /api/users/{id}` uses the generic safe-patch service and can change e-mail/department; `POST /api/users` accepts an arbitrary entity, including a raw password.
- The `/api/admin/users` controller is the correctly scoped surface and should be the only management API.

### D7. Profile/session mechanics do not follow the documented contract (P1)

- The AGENTS/security baseline says access tokens must live in memory and refresh tokens should be HttpOnly/rotating.
- `StorageService` stores both the token and user snapshot in `localStorage` under `ls_token` / `ls_user`.
- `AuthService` has no silent refresh loop and no in-memory-only token handling.
- `POST /auth/logout` is a no-op; it does not invalidate a refresh token.
- `POST /auth/refresh` accepts the same access token and returns a new one, rather than implementing the documented refresh-token flow.
- `forgot-password` returns the reset token in the response body (explicitly a development workaround).
- `AuthService.logout()` clears local storage but does not call the backend logout endpoint.

### D8. Account status semantics are inconsistent (P2)

- The profile/user DTO uses `ACTIVE | FLAGGED | BLACKLISTED`.
- `UserAdminService` and the admin UI use the same labels, but `PATCH /api/users/{id}/status` accepts `isActive` and maps `false` to `BLACKLISTED`, not a neutral deactivated state.
- There is no dedicated inactive/deactivated status in the entity, despite the original specification using `isActive`.

---

## E. Cross-cutting security and integrity notes

### E1. Legacy `/api/users/**` authorization (P0)

See D6. The security filter chain only guards `/api/admin/**`, `/api/teacher/**` and a few public paths; `/api/users/**` falls through to `authenticated()`.

### E2. Microservice order API exposure (P0)

See B7-B11. The payments service has no Spring Security dependency in its current POM and uses `@CrossOrigin("*")`. Order creation accepts client-supplied `userId` and `courseId`; order lookup is by path user ID; simulation is open.

### E3. Main payment simulation endpoints (P1)

- `/api/payments/checkout` and `/api/payments/verify` can create a paid `Payment` and enroll the user with no gateway interaction.
- They are authenticated, but they bypass the Razorpay flow.
- They should be clearly development-only or removed from non-dev profiles.

### E4. Public course/lecture payloads (P1)

- `GET /api/courses/**` is public, including lecture endpoints.
- If lecture DTOs contain `videoUrl`, unauthenticated callers can retrieve course video URLs.
- Player access must be checked server-side by enrollment/purchase, not only in Angular.

### E5. PDF data leakage / privacy (P2)

- Receipts include the student's name/e-mail and payment references.
- They are generated client-side, which is acceptable for a user-downloaded receipt, but any future server-side email/archival flow must scope access to the owner.
- `paymentId` and order IDs should not be logged in client error telemetry.

---

## F. Fix plan for this implementation pass

The following items are directly requested and will be fixed after this audit:

1. **Receipt PDF/UI**
   - Replace all non-WinAnsi glyphs in the generated PDF with ASCII (`INR`, `OK`, `-`).
   - Rebuild the receipt as a monochrome, dynamic-layout A4 document.
   - Add receipt number, order ID, transaction/payment ID, payment date **and time**, status and amount.
   - Wrap long text and compute row heights dynamically.
   - Pass `gatewayPaymentId` and `paidAt` from the main payment history into the receipt.
   - Make the receipt modal match the black-and-white document and show the transaction ID/timestamp.
   - Remove the fabricated `INV-{id + 1000}` number and use the real payment/order reference.
   - Stop calling the simulated payment flow for the primary receipt path; keep the existing real checkout path intact.

2. **Payment history**
   - Remove fake payment creation from `PaymentServiceImpl.getHistory`.
   - Map the main status (`SUCCESS/PENDING/FAILED`) correctly in Angular.
   - Keep failed and pending attempts visible; deduplicate by real gateway order ID rather than collapsing by course.
   - Normalise amounts to paise and timestamps to one representation.
   - Show correct counts and totals.
   - Preserve transaction IDs and capture times.
   - Make the receipt action available only for successful payments.
   - Do not show a successful status for a row that has no real successful payment.
   - Keep microservice order data as a supplementary source, with clear precedence rules.

3. **Student course surfaces**
   - Make **My Courses** enrolled-only, with a direct player/resume entry point.
   - Add a course-scoped player route that resolves the first incomplete lecture.
   - Add a separate **Explore Courses** catalog that shows all published/available courses in a Udemy/Coursera-style grid.
   - Point Video Lectures/sidebar to the catalog, not to the player fallback.
   - Keep lecture playback at the existing course/lecture route.
   - Preserve course detail/enrolment behaviour and the real Razorpay purchase path.

4. **Profile corrections**
   - Convert the teacher profile to real `AuthService` data and a real `PUT /users/me` save.
   - Convert the admin profile to a fresh `GET /users/me` read and a real save; replace fake health/uptime with real data.
   - Remove dead/fake avatar, password and save handlers or route them to real settings/API actions.
   - Leave the student profile's working profile API in place and avoid unrelated churn to its WIP styles.

5. **Safe integrity hardening**
   - Stop writing fabricated payment rows.
   - Fix the main history query to return actual rows only.
   - Add the real payment ID/timestamp to the internal payment records when the microservice can supply them.
   - Keep broader auth/microservice hardening (B7-B11, D6-D8) as documented follow-up work unless required by the fixes above.

---

## G. Verification checklist

- [ ] Angular production build passes.
- [ ] Spring Boot backend compiles/tests pass.
- [ ] Payments microservice compiles/tests pass.
- [ ] PDF opens with no missing glyphs, overlaps, or clipped text.
- [ ] PDF contains the correct header, receipt number, order ID, transaction ID, payment date/time, course, amount and status.
- [ ] Purchases page shows real PAID / PENDING / FAILED states and filters.
- [ ] Failed/pending attempts are not erased by later success.
- [ ] My Courses contains only enrolled courses.
- [ ] Continue Learning opens the course player at an incomplete/first lecture.
- [ ] Explore Courses lists all published courses and supports search.
- [ ] Enrolled catalog cards link to Continue, not duplicate enrolment.
- [ ] Teacher/admin profile edits persist after refresh.
- [ ] No unrelated WIP files are overwritten or staged.
