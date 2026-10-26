# Docs/source — frozen inputs

Source material that feeds LearnSphere is inventoried here and left untouched. The one authored file
is `spec.md`, the normalised contract derived from those sources. Originals that cannot be moved
(root `AGENTS.md`, `README.md`, `RUNNING.md`) are inventoried with hashes.

> **Note on the folder name:** this repository already used `Docs/` (capital D). On Windows the
> filesystem is case-insensitive, so `Docs/` and the template's `docs/` are the same folder. All
> references in this documentation system use the real path `Docs/…`. The deviation is recorded in
> [`../../TRACKER.md`](../../TRACKER.md) §4.

## Contents

| File | What it is |
|---|---|
| `spec.md` | **The frozen specification** for LearnSphere AI. Consolidated from `AGENTS.md` §1–§10, `LMS_Entity_Reference.pdf` (22 pages) and the non-PDF feature rules. Contract changes append v1.x; editorial corrections are logged in its revision log. |
| `MANIFEST.md` | Inventory of every source, its hash, where it lives and whether an agent can read it. |
| `../LMS_Entity_Reference.txt` | Mechanical text conversion of `Docs/LMS_Entity_Reference.pdf` (pypdf, page markers preserved); kept next to the original, not in this folder. |

There is no `idea.md`: this project started from source material (MODE A), not from an idea-only brief.

## Rules

1. **This folder is frozen.** If a source is wrong or contradicts another, that is recorded as a
   finding in `Docs/analysis/`, never fixed here.
   *Exception:* `spec.md` is versioned — substantive corrections are appended as v1.1, v1.2 …
   sections; editorial corrections (cross-references, coverage notes) are logged in its revision log
   and do not change the contract.
2. Convert formats agents cannot read to `.md`/`.txt` **next to the original**. Conversions are
   mechanical (no paraphrase, no interpretation).
3. Cite sources by the keys defined in `Docs/analysis/_TEMPLATE_analysis.md` §0.1.
4. Originals that must stay at the repo root (`AGENTS.md`, `README.md`, `RUNNING.md`) are not
   duplicated here. `MANIFEST.md` pins their SHA-256 prefix; analysis cites them by key.
