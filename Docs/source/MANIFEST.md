# Source manifest

Frozen 2026-10-22. SHA-256 prefixes identify the exact revision analysed. Originals marked
`root` stay where they are (they are referenced by tooling); they are not copied.

| # | File | What it is | Received / authored | SHA-256 (8) | Agent-readable | Location |
|---|---|---|---|---|---|---|
| 1 | `AGENTS.md` | Authoritative entity/API specification, 10 domain sections + security rules + preservation rules | project team, 2026-10-04 | `3052183F` | yes | root |
| 2 | `Docs/LMS_Entity_Reference.pdf` | 22-page entity reference: fields, DB columns, business rules, endpoints per domain | project team, 2026-04-29 | `DE032A77` | **no → converted** | `Docs/` |
| 3 | `Docs/LMS_Entity_Reference.txt` | Mechanical text conversion of #2 (page markers) | generated 2026-10-22 | — | yes | `Docs/` |
| 4 | `Docs/Architecture.md` | High-level architecture, services, security model | project team, 2026-10-04 | `6443EA2F` | yes | `Docs/` |
| 5 | `Docs/Backend.md` | Backend services, stack, responsibilities | project team, 2026-10-04 | `0F1A150F` | yes | `Docs/` |
| 6 | `Docs/Frontend.md` | Frontend structure and capabilities | project team, 2026-08-19 | `7B1783C3` | yes | `Docs/` |
| 7 | `Docs/learnsphere_system_design.md` | Deep-dive design: flows, ERD, AI, payments, trade-offs | project team, 2026-10-07 | `A9BA230A` | yes | `Docs/` |
| 8 | `Docs/Future_Plans.md` | Completed / deferred / future roadmap | project team, 2026-08-19 | `09333E24` | yes | `Docs/` |
| 9 | `Docs/Progress.md` | Status report ("production candidate") | project team, 2026-08-19 | `9EAB307B` | yes | `Docs/` |
| 10 | `README.md` | Product vision, business model, role features, routes | project team, 2026-06 | `E316B960` | yes | root |
| 11 | `RUNNING.md` | Local run instructions | project team, 2026-10-04 | `791D7BCE` | yes | root |
| 12 | `Docs/VIDEO_PLAYER_LEARNING_FLOW_AUDIT_V2.md` | Audit of the lecture player learning flow | project team, 2026-08-23 | `9EE67730` | yes | `Docs/` |
| 13 | `Docs/LANDING_PAGE_AUDIT_AND_IMPROVEMENT_PLAN.md` | Landing page audit and plan | project team, 2026-10-04 | `6527FD52` | yes | `Docs/` |
| 14 | Source code `learnsphere-backend/src/main/java/…` | 36 entities, 45 controllers, services, security, seeder | project team | — | yes | repo |
| 15 | Source code `learnSphere/frontend/learnsphere/src/app` | Angular SPA, routes, services, components | project team | — | yes | repo |
| 16 | Source code `learnsphere-payments-service/src/main/java/…` | Razorpay microservice (orders, webhook) | project team | — | yes | repo |

## Known absent sources

- No CI configuration (`.github/` does not exist).
- No database migrations (schema is created by `spring.jpa.hibernate.ddl-auto=update`).
- No screenshots of the running UI.
- No requirement document for the "ExamGuard" VS Code extension, only `security_policy` stored as a string.
- No SMTP/provider contract: password reset logs the token instead of emailing it.
