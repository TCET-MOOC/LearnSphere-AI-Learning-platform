#!/usr/bin/env node
/**
 * Copy ratchet (W7-07; PLAN §7 "Copy", §11 DoD): user-facing copy lives in `src/app/core/content/*.ts`.
 *
 * Counts hard-coded copy in every component template (`*.component.html` and inline `template:`):
 * literal text nodes with at least two letters, and literal `placeholder`, `title`, `aria-label`
 * and `alt` attributes. Interpolations (`{{ … }}`), bindings and control-flow blocks are ignored.
 *
 * Ratchet semantics (same as the style ratchet):
 *   - no template may rise above its count in `copy.baseline.json`;
 *   - a template that is not in the baseline (new code) must have none;
 *   - the templates in EXTRACTED must have none (their copy was moved in W7-07).
 * Reductions are always allowed; `--update` re-baselines them (never upward).
 *
 * Usage:
 *   node scripts/copy-ratchet.mjs            # gate
 *   node scripts/copy-ratchet.mjs --update   # lower the baseline after extracting copy
 *   node scripts/copy-ratchet.mjs --list <file>   # show the copy found in one template
 */
import { readFile, writeFile, readdir } from 'node:fs/promises';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');
const APP = path.join(ROOT, 'src', 'app');
const BASELINE_PATH = path.join(ROOT, 'copy.baseline.json');
const UPDATE = process.argv.includes('--update');
const LIST = process.argv.includes('--list') ? process.argv[process.argv.indexOf('--list') + 1] : null;

/** Screens whose copy W7-07 moved into core/content: they must stay at zero. */
const EXTRACTED = [
  'src/app/features/auth/login/login.component.html',
  'src/app/features/auth/register/register.component.html',
  'src/app/features/auth/forgot-password/forgot-password.component.html',
  'src/app/features/auth/verify-college/verify-college.component.html',
  'src/app/features/public/verify-certificate/verify-certificate.component.html',
  'src/app/features/payment/checkout.component.ts',
  'src/app/features/payment/payment-success.component.ts',
];

const COPY_ATTRS = /\s(placeholder|title|aria-label|alt)="([^"]*)"/g;

export function findCopy(template) {
  const t = template
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<(style|script)\b[\s\S]*?<\/\1>/g, '')
    .replace(/\{\{[\s\S]*?\}\}/g, ' ')
    // Angular control flow: `@if (…) {`, `} @else {`, `@for (…; track …) {`, `@case (…) {`, closing braces.
    .replace(/@(if|else if|else|for|switch|case|default|empty|defer|placeholder|loading|error)\b(\s*\((?:[^()]|\([^()]*\))*\))?\s*\{/g, ' ')
    .replace(/@let\s+[^;]+;/g, ' ')
    .replace(/[{}]/g, ' ');
  const found = [];
  for (const m of t.matchAll(/>([^<>]*)</g)) {
    const text = m[1].replace(/&[a-z#0-9]+;/gi, ' ').replace(/\s+/g, ' ').trim();
    if (/[A-Za-z]{2,}/.test(text)) found.push(text);
  }
  for (const m of t.matchAll(COPY_ATTRS)) {
    if (/[A-Za-z]{2,}/.test(m[2])) found.push(`${m[1]}="${m[2]}"`);
  }
  return found;
}

async function walk(dir) {
  const out = [];
  for (const e of await readdir(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...(await walk(p)));
    else out.push(p);
  }
  return out;
}

async function templates() {
  const result = new Map();
  for (const file of await walk(APP)) {
    const rel = path.relative(ROOT, file).split(path.sep).join('/');
    if (rel.endsWith('.component.html')) {
      result.set(rel, findCopy(await readFile(file, 'utf8')));
    } else if (rel.endsWith('.component.ts')) {
      const inline = (await readFile(file, 'utf8')).match(/\btemplate:\s*`([\s\S]*?)`/);
      if (inline) result.set(rel, findCopy(inline[1]));
    }
  }
  return result;
}

const found = await templates();

if (LIST) {
  for (const text of found.get(LIST) ?? []) console.log(`  ${text}`);
  process.exit(0);
}

const baseline = JSON.parse(await readFile(BASELINE_PATH, 'utf8').catch(() => '{}'));
const problems = [];
let total = 0;
for (const [file, copy] of [...found].sort()) {
  const n = copy.length;
  total += n;
  if (EXTRACTED.includes(file)) {
    if (n) problems.push(`${file}: ${n} hard-coded (extracted in W7-07, must be 0): ${copy.slice(0, 3).join(' | ')}`);
  } else if (!(file in baseline)) {
    if (n) problems.push(`${file}: new template with ${n} hard-coded (use core/content/*.ts): ${copy.slice(0, 3).join(' | ')}`);
  } else if (n > baseline[file]) {
    problems.push(`${file}: ${n} hard-coded, baseline ${baseline[file]}`);
  }
}
for (const file of EXTRACTED) if (!found.has(file)) problems.push(`${file}: listed in EXTRACTED but not found`);

if (UPDATE) {
  // The first run records today's copy; after that the baseline only goes down and never admits a new template.
  const initial = Object.keys(baseline).length === 0;
  const next = {};
  for (const [file, copy] of [...found].sort()) {
    if (EXTRACTED.includes(file) || !copy.length) continue;
    if (file in baseline) next[file] = Math.min(baseline[file], copy.length);
    else if (initial) next[file] = copy.length;
  }
  await writeFile(BASELINE_PATH, JSON.stringify(next, null, 2) + '\n');
  console.log(`Copy baseline written: ${Object.keys(next).length} templates, ${Object.values(next).reduce((a, b) => a + b, 0)} strings.`);
  process.exit(0);
}

if (problems.length) {
  console.error(`Copy ratchet failed (${problems.length}):\n  - ${problems.join('\n  - ')}`);
  process.exit(1);
}
console.log(`Copy ratchet passed: ${total} hard-coded strings across ${found.size} templates (none new).`);
