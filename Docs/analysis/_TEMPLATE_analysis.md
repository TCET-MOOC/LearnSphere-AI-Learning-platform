# [Letter] — Analysis: [area]

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** [which parts of the sources — spec sections, screens, tables]
**Date of analysis:** yyyy-mm-dd

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n (text: `Docs/LMS_Entity_Reference.txt`) |
| `AG §n` | root `AGENTS.md` section n |
| `ARCH` / `BE` / `FE` / `SYS §n` | `Docs/Architecture.md` · `Docs/Backend.md` · `Docs/Frontend.md` · `Docs/learnsphere_system_design.md` |
| `README §n` / `RUN` / `ROAD` / `PROG` | root `README.md` · `RUNNING.md` · `Docs/Future_Plans.md` · `Docs/Progress.md` |
| `code <path>:<line>` | implementation evidence in this repo |
| `derived` | an inference of the analyst, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| spec | Read in full (v1.0, 10 sections). |
| REF | Read in full via mechanical text conversion (22 pages). |
| Code | Read the entities, controllers and service implementations in scope. |
| Not present | no screenshots, no CI, no migrations, no test suite beyond one context test. |

---

## 1. [Topic]

## n. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|

## n+1. Gaps — what the sources do NOT provide

## n+2. Open questions
