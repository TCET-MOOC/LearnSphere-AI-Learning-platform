# F — Analysis: commerce, royalties & institutions

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** `spec §3.5`, `spec §5` row 9 + preserved row, `spec §6` (Razorpay, internal API), `spec §7` (privacy), `spec §9` Q-06/Q-07 · `AG §4` (Razorpay, college affiliations) · `README.md` revenue model · `RUNNING.md` §3 · backend `Entity/{Payment,Royalty,Payout,College,AffiliationRequest,Course,UserAccount}`, `Service/impl/{Payment,Royalty,Revenue,Admin,Auth}ServiceImpl`, `Controller/{Payment,Internal,Royalty,PayoutAdmin,Revenue,College,Admin,Auth}Controller`, `Config/SecurityConfig` · payments microservice `OrderController`, `WebhookController`, `Order`, `Payment`, `AppConfig`, `application.properties`, `pom.xml` · Angular `features/payment/**`, `features/student/services/payments.service.ts`, `features/student/{courses/course-details,purchases}/**`
**Date of analysis:** 2026-10-22

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n |
| `AG §n` | root `AGENTS.md` section n |
| `README` / `RUNNING` | root `README.md` / `RUNNING.md` |
| `code <path>:<line>` | implementation evidence |
| `derived` | analyst inference, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| `spec §3.5, §5, §6, §7, §9` | Read in full. |
| `AG §4` | Read in full (Razorpay + college affiliations must survive). |
| `REF` | **No payment, royalty, payout, college or affiliation entity exists in the reference.** The PDF ends at Section 10 (Post, p22). For these areas `AG §4` + `spec` + `README` are the only authority. |
| Backend | Five entities; `PaymentServiceImpl`, `RoyaltyServiceImpl`, `RevenueServiceImpl`, `AdminServiceImpl`, `AuthServiceImpl` (payment/college paths); all payment/college controllers. |
| Microservice | All six Java classes, `application.properties`, `pom.xml`. |
| Frontend | Both checkout flows, payment success, purchases, course-detail purchase modal, `PaymentsService`. |
| Does NOT exist | shared order id across rails, gateway event log, refunds, invoices/GST, subscriptions, college revenue share, same-college free-access rule, bank-transfer payout, reconciliation job. |

---

## 1. The contract (and what is actually specified)

1. **Payment flow** — create order → Razorpay checkout → verify signature → `POST /internal/enroll` → enrollment + payment + **70 %** royalty credit. Failure: bad signature → 400, already enrolled → 409, free course → 400. **AC-19** verification fails closed; **AC-20** webhook idempotent per `razorpay_payment_id`; **AC-21** teacher gets 70 %, platform 30 % (`spec §3.5`, `spec §6`).
2. **Enrollment** — unique `(user_id, course_id, role)`; a course cannot be enrolled in unless published (`spec §5` row 9; `AG §5`; AC-10).
3. **Preserved commerce features** — Razorpay purchases link `course_id` + `user_id`; college records remain attached to `user_account` and `department` (`AG §4`). Nothing else about their schema is fixed.
4. **Business model** — external purchase, college subscription, remedial fee; platform 20 % and teacher 80 % (`README.md:45–56`). This conflicts with AC-21's 70/30 (Q-07).
5. **Runbook** — payments microservice on `:8081`, Razorpay + webhooks, "in-memory H2 database for local development" (`RUNNING.md:74–93`).

---

## 2. Two rails, two checkout UIs, three ledgers

6. **Rail A — simulated, main backend.** `POST /api/payments/checkout` creates a `Payment` row with `gatewayOrderId = "order_"+UUID` and `PENDING`; `POST /api/payments/verify` flips it to `SUCCESS`, enrolls the caller and credits 70 % (`Controller/PaymentController.java:41`; `Service/impl/PaymentServiceImpl.java:86–133`). `GET /history` and `/status` complete the surface. There is no gateway call and no signature to verify.
7. **Rail B — Razorpay microservice.** `POST http://localhost:8081/api/payments/orders` calls the Razorpay SDK, stores an `Order`, and returns `razorpay_order_id` + key id; the student course-detail page opens the real Razorpay modal and posts the handler response to `/orders/verify` (`Controller/OrderController.java:47–120`; `features/student/services/payments.service.ts:37–52`; `features/student/courses/course-details/course-detail.component.ts:321–410`).
8. **Rail A UI.** The standalone `/payment/checkout/:courseId` page uses `PaymentService.checkout()` then immediately `verify()` — a one-click simulated purchase with no modal (`features/payment/checkout.component.ts:183–206`). **derived:** the same product has two different checkout experiences depending on which link the student follows.
9. **Three ledgers.** (a) main `payments` table in database `MOOC` (`Payment`); (b) microservice `orders` + `payments` tables in database `learnsphere_payments` (`Order`, `Payment`); (c) `royalties` rows + `UserAccount.royaltyBalance` + `payouts` in the main database. There is no shared primary key between (a) and (b); the only join key is the gateway order id string, and Rail A never has a real one.
10. **Rail A fabricates the missing side of Rail B.** `PaymentServiceImpl.getHistory` scans the caller's paid-course enrollments and, when no local `Payment` exists, inserts a synthetic `SUCCESS` row with `gatewayOrderId = "order_rzp_{courseId}_{userId}"` and a random payment id (`PaymentServiceImpl.java:137–160`). **derived:** every microservice purchase produces a microservice `Payment` row **and** a later fabricated main `Payment` row; the two are not reconciled and may carry different amounts.

---

## 3. The microservice has no authentication and a fail-open verify

11. The microservice has **no Spring Security dependency** (`pom.xml` contains only data-jpa, web, validation, Razorpay, dotenv, H2, PostgreSQL, Lombok). There is no filter, no JWT, no key check on `/api/payments/**`.
12. `createOrder` takes `userId` and `courseId` from the request body (`OrderController.java:47–49`) and is annotated `@CrossOrigin("*")` (`:18`). `GET /orders/{userId}` returns any user's orders (`:251–252`). It also falls back to a flat **₹1,000** amount when the main-backend price fetch fails (`:64`), so an outage creates an order at the wrong price. **derived:** anyone who can reach `:8081` can create an order for another user, read their order history, or buy at a fallback price.
13. `POST /orders/simulate-success` is unauthenticated (`OrderController.java:123`). It marks any order `PAID`, writes a `pay_sim_*` payment, and calls `/internal/enroll` (`:124–176`). It does **not** check whether the order is already `PAID`, expired, or belongs to the caller.
14. `POST /orders/verify` catches the signature exception and continues (`OrderController.java:201–204`). A bad or missing signature still marks the order `PAID`, writes a `CAPTURED` payment and calls `/internal/enroll` (`:206–245`). **AC-19 fails: verification is fail-open.**
15. `/orders/verify` has no idempotency key. It accepts any `razorpay_payment_id`; a replay with a fresh id bypasses the unique constraint and triggers another `/internal/enroll` call. **derived:** one order can mint multiple enrollments/royalty credits.
16. The webhook is the only correct rail: it verifies the signature and returns 401 on failure (`Controller/WebhookController.java:47–50`), and checks `existsByRazorpayPaymentId` before processing `payment.captured` (`:61`). This is the AC-20 implementation. **derived:** the correct path is optional; the two bypass paths are easier to call.

---

## 4. `/internal/enroll` trusts its caller for money

17. `/internal/**` is `permitAll` in Spring Security (`Config/SecurityConfig.java:81`); the controller then checks `X-Internal-Key` itself (`Controller/InternalController.java:64`). The default key is committed in both properties files (`internal.api.key=${INTERNAL_API_KEY:my_secret_internal_key}`), so the check is not a secret in any default deployment.
18. The controller accepts `amount` from the JSON body and uses it as the paid amount (`InternalController.java:96–99`). It does **not** recompute the price from the course. A caller with the known default key can enroll any user in any course and credit 70 % of any amount they choose.
19. It records a `Payment` and a `Royalty`, and increments `royaltyBalance`, **on every call**, even when the enrollment already exists (`InternalController.java:106–129`). **derived (critical):** replaying `/internal/enroll`, `/orders/verify` or `/orders/simulate-success` multiplies payments and royalties without bound. `enrollUser` only guards the enrollment row.
20. The internal path does not check `course.status == LIVE`, the course price, the user's role, or that `student` exists (`student` may be null; the `Payment.user` column is nullable). It also enrolls with role `STUDENT` regardless of the buyer's role.

---

## 5. Rail A: single-shot verify, fabricated history

21. `checkout` rejects free courses and already-enrolled users, but does **not** reject non-`LIVE` courses (`PaymentServiceImpl.checkout`). **derived:** a paid `DRAFT`/`PENDING` course can be purchased by id if the caller knows it.
22. `verify` requires the payment to belong to the caller and to be `PENDING`, so a single Rail A order cannot be verified twice (`PaymentServiceImpl.java:86–96`). That is the only idempotency in Rail A.
23. `getHistory` writes rows during a `@Transactional` read (`PaymentServiceImpl.java:137`). It marks fabricated rows `SUCCESS` with `paidAt = enrollment.enrolledAt`; it does not create a royalty. **derived:** simply opening the purchases page changes financial history and can make a free enrollment in a paid course look like revenue.
24. `hasPaid` checks only for a `SUCCESS` payment, not for enrollment (`PaymentServiceImpl.hasPaid`). **derived:** a user enrolled through `POST /api/enrollments` (which skips payment entirely, see C analysis) is enrolled but not "paid"; the two states are independent.

---

## 6. Royalty and payout ledger

25. The 70 % constant is duplicated in three places: `PaymentServiceImpl.java:37`, `InternalController.java:38`, `RevenueServiceImpl.java:23`. There is no shared policy object.
26. `RoyaltyServiceImpl.getRoyalties` **auto-reconciles** on every teacher read: it counts **all** enrollments in each paid course (`countByCourse_Id`, no role or payment filter), subtracts existing royalty rows, and creates one 70 %-of-price royalty per missing enrollment, then increments the balance (`Service/impl/RoyaltyServiceImpl.java:74–96`). **derived (critical):** a free/direct/college enrollment in a paid course mints a royalty the first time the teacher opens the royalties page. The ledger is self-filling and not tied to money received.
27. Pending payouts are computed live from `UserAccount.royaltyBalance`; `process-all` and `pay/{teacherId}` create a `Payout` with status `PAID`, set `transferredAt`, and zero the balance (`Controller/PayoutAdminController.java`). There is no bank-transfer integration, no minimum threshold (README says ₹500), no schedule, and no lock — **derived:** two concurrent `process-all` calls can both read the same balance and create two payouts.
28. `Payout` defines `PENDING | PAID`, but rows are only ever created `PAID`; the class comment admits there is no pending queue. The teacher's payout history therefore shows only completed transfers.
29. `RevenueServiceImpl` reports teacher royalties as **70 % of all `SUCCESS` payments**, not the sum of `Royalty` rows (`RevenueServiceImpl.getRevenueSummary`). **derived:** the admin revenue report and the teacher royalty ledger are two independent computations that diverge whenever a backfill, auto-reconcile, internal `amount`, or fabricated payment occurs. There is no single source of truth for "what the platform earned".
30. The split is 70/30 in code and AC-21, but **80/20 in `README.md:53–54`** (Q-07). The README also promises a monthly payout above ₹500 and a college "branding" benefit; neither is implemented.

---

## 7. Colleges and affiliations

31. `College` is `id, name, city, verificationStatus ∈ {PENDING, VERIFIED}, appliedAt` (`Entity/College.java`). There is no unique name, code, domain, address or contact; two identical colleges can be created. `AffiliationRequest` is `teacher, claimedDepartment (string), claimedCollege, idDocumentUrl, status ∈ {PENDING, APPROVED, REJECTED}, createdAt` (`Entity/AffiliationRequest.java`).
32. `GET /api/colleges` is public and returns **all** colleges, including `PENDING` ones (`Controller/CollegeController.java`; `SecurityConfig.java:82`). The registration dropdown therefore offers unverified institutions.
33. Admin endpoints create (always `PENDING`), verify, "reject" (= hard delete), list, and approve/reject affiliation requests (`Controller/AdminController.java:23–59`; `AdminServiceImpl.java:43–92`). Rejecting a college deletes the row (`AdminServiceImpl.java:58–64`); **derived:** if any user or course references it, the delete fails on the FK, and there is no archive state.
34. **Registration bypasses affiliation review.** `RegisterRequest` accepts `collegeId` for any role, and `register` sets `user.college` immediately without checking `verificationStatus` (`Dto/AuthDtos.java:23–25`; `AuthServiceImpl.java:85–94`). A self-registered `TEACHER` can attach to any college — including a `PENDING` one — and never submit an affiliation request.
35. `verify-college` creates an `AffiliationRequest` for teachers and directly sets the college for everyone else (`AuthServiceImpl.java:180–195`). It does not prevent a second request or check the college status.
36. **The frontend guard is dead code.** `verifiedCollegeGuard` exists (`core/guards/verified-college.guard.ts:11`) but is not referenced by `app.routes.ts` or any component; backend course authoring does not check affiliation either (B analysis). **derived:** the "verified faculty" gate advertised in `README.md:81` is unenforced end to end.
37. Approving an affiliation sets `teacher.college = claimedCollege` and saves the request; rejecting only flips the status (`AdminServiceImpl.approveAffiliation/rejectAffiliation`). Neither sends a notification. There is no audit trail beyond the request row, and `claimedDepartment` is free text, not a `Department` FK.
38. **Same-college free access is not implemented.** `README.md:62` says students of the same college get free access; `checkout` and `enroll` never compare `user.college` with `course.teacher.college`, and `Course` has no college/department FK (it stores a `department` string). **derived:** the core institutional value proposition is currently a pricing page claim only.
39. `CollegeResponseDto` counts students/teachers/courses via `UserRoleRepository.countByRole_NameAndUser_College_Id` and `courseRepository.countByTeacher_College_Id` (`AdminServiceImpl.toDto`). Because teachers can self-attach at registration, these counts include unapproved affiliations.

---

## 8. Frontend purchase experience

40. Two services, two base URLs: `features/payment/payment.service.ts` uses `environment.apiUrl` (`/api/payments/*`, Rail A); `features/student/services/payments.service.ts` hard-codes `http://localhost:8081/api/payments` (Rail B). The purchases page calls **both** and merges them by `courseId`, preferring a `PAID` row (`features/student/purchases/purchases.component.ts:100–170, 177–200`). **derived:** dedupe by course hides double records but also collapses multiple transactions into one row.
41. The payment-success page is static: it reads `courseId` from the query string and renders a confirmation without calling `/status` or `/verify` (`features/payment/payment-success.component.ts`). **derived:** it can be opened directly for a course the user never bought.
42. `PaymentsService.simulateSuccess` exists in the frontend but has **no call site**; the test-only endpoint is still reachable directly.
43. `RUNNING.md:80` says the microservice uses H2 by default; `application.properties` defaults to PostgreSQL `learnsphere_payments`. The doc is stale. The microservice also defaults `razorpay.key.secret` to the **key id** and `razorpay.webhook.secret` to `your_webhook_secret` (`application.properties`), so a default run cannot verify real signatures.

---

## 9. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|
| F1 | Two independent payment rails with two checkout UIs | users get different flows; two support paths |
| F2 | Microservice has no authentication; `createOrder` trusts `userId` | anyone can create/read orders for anyone |
| F3 | `/orders/simulate-success` is unauthenticated and replayable | free enrollment and royalty minting |
| F4 | `/orders/verify` is fail-open; bad signature still enrolls | AC-19 fails; payment forgery |
| F5 | `/orders/verify` has no idempotency on order id | replay mints extra payments/royalties |
| F6 | `/internal/enroll` trusts client `amount` and the default key is public | arbitrary royalty inflation |
| F7 | `/internal/enroll` writes a payment + royalty on every call | replay multiplies the ledger |
| F8 | `getHistory` fabricates `SUCCESS` payments for paid enrollments | fake revenue from free/direct enrollment |
| F9 | Royalty page auto-creates royalties from **all** enrollments | free enrollment in a paid course mints money |
| F10 | Revenue report (70 % of payments) and royalty ledger (rows/balance) are separate | numbers disagree; no reconciliation |
| F11 | Split is 70/30 in code/spec but 80/20 in README | unresolved business rule Q-07 |
| F12 | Registration accepts `collegeId` for teachers and skips verification | affiliation review bypass |
| F13 | `verifiedCollegeGuard` is never applied; backend has no affiliation check | unverified teachers can author |
| F14 | Rejecting a college hard-deletes it; no notification on affiliation decisions | FK failures; silent rejections |
| F15 | Same-college free access is not implemented anywhere | core institutional model is missing |
| F16 | Main rail has no `Order` entity; statuses are `Payment.PENDING/SUCCESS/FAILED` vs microservice `Order.CREATED/PAID/FAILED` and `Payment.CAPTURED` string | schema/contract drift across services |
| F17 | Rail A checkout does not require `course.status == LIVE` | unpublished paid courses are purchasable by id |
| F18 | Payout has no threshold/schedule/lock and no bank transfer | double-pay race; "paid" is only a DB row |

## 10. Gaps — what the sources do NOT provide

- **NO SCHEMA** for `Payment`, `Royalty`, `Payout`, `College` or `AffiliationRequest`; `REF` ends at Section 10. Only `AG §4` + `spec §6` constrain them.
- **NO RULE** for which rail is canonical, or how the two databases reconcile (Q-06).
- **NO DECISION** on 70/30 vs 80/20 (Q-07).
- **NO CONTRACT** for refunds, chargebacks, GST/invoices, receipts or currency other than INR (`spec §8` defers refunds).
- **NO SPEC** for college subscription billing, seat counts or same-college entitlement.
- **NO REQUIREMENT** for a minimum payout threshold, schedule or bank-transfer integration (README mentions ₹500/month).
- **NO API** for a teacher or college to see a settlement statement or download an invoice.

## 11. Open questions

| ID | Question | Feeds |
|---|---|---|
| F-Q1 | Which rail is production — simulated main backend or Razorpay microservice? | A-06 / Wave 6 |
| F-Q2 | Is the split 70/30 or 80/20? | A-07 / Wave 6 |
| F-Q3 | Should `/internal/enroll` recompute the amount from the course instead of trusting the caller? | A-19 / Wave 6 |
| F-Q4 | Is same-college free access in scope, and how is college membership verified? | A-20 / Wave 6 |
| F-Q5 | Is there one canonical ledger keyed by gateway payment/order id, with the other rail retired? | A-21 / Wave 6 |
| F-Q6 | Does "payout" mean a DB record only, or a real transfer with threshold/schedule? | A-22 / Wave 6 |
