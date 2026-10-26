## Story

- **Story ID:** `P0-xx` / `Wx-xx` / `H-xx`
- **Title:**
- **Acceptance rule(s):** `AC-xx` / `BR-xx` / `A-xx` / `AUD-xx` / `[X-Fn]`
- **Change type:** behaviour-changing / docs-config-only (use the verification checklist below)

## Test-first evidence

> Mandatory for every behaviour change. The test must have been observed failing for the intended
> reason before the production change. A compile error or missing fixture is not red evidence.

| Test file | Test name(s) | Red command | Red result (intended failure) |
|---|---|---|---|
| `path/to/spec` | `should_...` | `command` | `1 failed, ...` |

| Green command | Result |
|---|---|
| `command` | `... passed` |

- [ ] I ran the focused test first and saw it fail for the intended reason.
- [ ] The smallest production change made the same test pass.
- [ ] I refactored with the test green and reran the focused test plus the fast suite.
- [ ] No test was weakened, skipped, quarantined or deleted to obtain green.
- [ ] The test is hermetic: no live Razorpay/NVIDIA/YouTube/upload/publish/live-provider calls,
      no shared database, no timing sleeps, deterministic fixtures.
- [ ] A bug fix starts with a regression test; a legacy refactor starts with a characterisation test.

## Suites run

- [ ] Backend: `cd learnsphere-backend && ./mvnw test` (Testcontainers PostgreSQL; Docker required)
- [ ] Frontend unit: `cd learnSphere/frontend/learnsphere && npm test`
- [ ] Frontend build: `cd learnSphere/frontend/learnsphere && npm run build`
- [ ] Payments service: `cd learnsphere-payments-service && ./mvnw test`
- [ ] Contract / integration / E2E suites affected by this change:
- [ ] Migration applied to a fresh database and seed still works (if schema changed)

## Review gates

- [ ] Acceptance test names match the behaviour, not the implementation.
- [ ] Permissions/ownership are asserted; no secret, answer, PII, token, stream key or storage key leaks.
- [ ] Cache behaviour is correct: public read `ETag`/`max-age` or private read `no-store` where required.
- [ ] Hot-path/query-count/performance evidence is attached when the story changes a hot path.
- [ ] Accessibility and visual-regression gates pass for changed screens (`NFR-01`).
- [ ] `TRACKER.md` is ticked with the gate commands and any deviation is recorded.
- [ ] No unrelated work-in-progress files are staged or committed.

## Docs/config-only verification

Use this instead of a fake unit test:

| Check | Command | Result |
|---|---|---|
| Example: files parse / config boots | `command` | `pass` |

## Notes for reviewers

<!-- Risk, rollback, follow-up story, or linked evidence. -->
