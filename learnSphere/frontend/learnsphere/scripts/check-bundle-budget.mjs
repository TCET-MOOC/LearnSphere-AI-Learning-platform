#!/usr/bin/env node
/**
 * W7-13 (NFR-02 budgets): initial JS ≤ 500 kB gzip; each lazy chunk ≤ 250 kB gzip.
 *
 * Angular's `budgets` measure raw bytes, so this gate measures what the browser downloads:
 * the initial set is every script `index.html` loads plus everything those scripts import
 * statically (followed transitively); every other `chunk-*.js` is lazy. Sizes are gzip level 9.
 * Following every static import can count a shared chunk Angular's own "Initial total" leaves out,
 * so the measured initial size is an upper bound (the gate errs on the strict side).
 * Prints the measured baseline so a story can quote it. Run after `npm run build`.
 *
 * Overrides for a deliberate check of the gate itself: BUDGET_INITIAL_KB, BUDGET_LAZY_KB.
 */
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { gzipSync } from 'node:zlib';

const dist = resolve(import.meta.dirname, '../dist/learnsphere/browser');
const INITIAL_KB = Number(process.env.BUDGET_INITIAL_KB ?? 500);
const LAZY_KB = Number(process.env.BUDGET_LAZY_KB ?? 250);

if (!existsSync(resolve(dist, 'index.html'))) {
  console.error('No production build at dist/learnsphere/browser — run `npm run build` first.');
  process.exit(2);
}

const gz = (file) => gzipSync(readFileSync(resolve(dist, file)), { level: 9 }).length;
const kb = (bytes) => (bytes / 1024).toFixed(1);

const html = readFileSync(resolve(dist, 'index.html'), 'utf8');
const entry = [...html.matchAll(/<script[^>]+src="([^"]+\.js)"/g)].map((m) => m[1]);
const initialCss = [...html.matchAll(/<link[^>]+href="([^"]+\.css)"/g)].map((m) => m[1]);

// Static imports only: `import ... from "./chunk-X.js"` / `import "./chunk-X.js"` /
// `export ... from "./chunk-X.js"`. Dynamic `import("./chunk-X.js")` stays lazy.
const STATIC_IMPORT = /(?:^|[;\s}])(?:import|export)\s*(?:[^'"();]*?from\s*)?["']\.\/([\w.-]+\.js)["']/g;
const initial = new Set();
const queue = [...entry];
while (queue.length) {
  const file = queue.pop();
  if (initial.has(file) || !existsSync(resolve(dist, file))) continue;
  initial.add(file);
  const src = readFileSync(resolve(dist, file), 'utf8');
  for (const m of src.matchAll(STATIC_IMPORT)) queue.push(m[1]);
}

const initialJs = [...initial].reduce((sum, f) => sum + gz(f), 0);
const initialStyles = initialCss.reduce((sum, f) => sum + gz(f), 0);
const lazy = readdirSync(dist)
  .filter((f) => f.endsWith('.js') && !initial.has(f))
  .map((f) => ({ file: f, size: gz(f) }))
  .sort((a, b) => b.size - a.size);

console.log(`Initial JS: ${initial.size} files, ${kb(initialJs)} kB gzip (budget ${INITIAL_KB} kB); initial CSS ${kb(initialStyles)} kB gzip`);
console.log(`Lazy chunks: ${lazy.length}; largest ${lazy.slice(0, 3).map((c) => `${c.file} ${kb(c.size)} kB`).join(', ')} (budget ${LAZY_KB} kB each)`);

const problems = [];
if (initialJs > INITIAL_KB * 1024) problems.push(`initial JS ${kb(initialJs)} kB gzip > ${INITIAL_KB} kB`);
for (const c of lazy) if (c.size > LAZY_KB * 1024) problems.push(`${c.file} ${kb(c.size)} kB gzip > ${LAZY_KB} kB`);

if (problems.length) {
  console.error(`Bundle budget failed (${problems.length}):`);
  for (const p of problems) console.error(`  - ${p}`);
  process.exit(1);
}
console.log('Bundle budget passed.');
