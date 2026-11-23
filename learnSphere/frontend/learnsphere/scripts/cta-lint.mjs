#!/usr/bin/env node
/**
 * DS-13 copy rules (W7-15; NFR-04(f)). Runs as part of `npm run lint:copy`.
 *   1. No `→` suffix on a label (templates, inline templates, `core/content/*.ts`).
 *   2. No unverifiable claim from the DS-13 list (invented status badges, registry/ledger claims,
 *      fallback grades/instructors the API never returned).
 *   3. CTAs are verb + object: a button/link whose whole text is one of the bare words below fails
 *      (the row/screen decides the object, e.g. "Start assessment", "View receipt").
 *   4. One name through the flow: leaving a live room is "Leave session" everywhere.
 */
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';

const ROOT = resolve(import.meta.dirname, '..');
const APP = join(ROOT, 'src/app');

const BARE = new Set([
  'Start', 'Manage', 'Inspect', 'Archive', 'Reactivate', 'Receipt', 'Print', 'Post', 'Refresh',
  'Share', 'Remove', 'Add', 'Leave', 'Verify', 'Flag', 'Download', 'Quiz',
]);
const CLAIMS = [
  /autonomous cluster/i, /autonomous sentiment/i, /cryptographic registry/i,
  /national verification ledger/i, /pass with distinction/i, /learnsphere faculty board/i,
];

function walk(dir) {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name);
    return statSync(p).isDirectory() ? walk(p) : [p];
  });
}

const files = walk(APP).filter((f) => (f.endsWith('.html') || f.endsWith('.ts')) && !f.endsWith('.spec.ts'));
const problems = [];
const line = (text, index) => text.slice(0, index).split('\n').length;

for (const file of files) {
  const rel = relative(ROOT, file).replaceAll('\\', '/');
  const text = readFileSync(file, 'utf8');
  const isContent = rel.startsWith('src/app/core/content/');
  // Template text only: .html files, inline `template:` literals, and content files.
  const regions = [];
  if (file.endsWith('.html')) regions.push([0, text]);
  else {
    for (const m of text.matchAll(/template\s*:\s*`([\s\S]*?)`/g)) regions.push([m.index + m[0].indexOf('`') + 1, m[1]]);
    if (isContent) regions.push([0, text.replace(/^\s*(\/\/|\*|\/\*).*$/gm, '')]);
  }
  for (const [offset, region] of regions) {
    for (const m of region.matchAll(/(→|&rarr;)\s*(<|['"`]|\n|$)/g)) {
      problems.push(`${rel}:${line(text, offset + m.index)} label ends with "→"`);
    }
    for (const claim of CLAIMS) {
      const m = claim.exec(region);
      if (m) problems.push(`${rel}:${line(text, offset + m.index)} unverifiable claim "${m[0]}"`);
    }
    for (const m of region.matchAll(/<(button|a)\b[^>]*>([\s\S]*?)<\/\1>/g)) {
      const label = m[2].replace(/<[^>]+>/g, ' ').replace(/\{\{[^}]*\}\}/g, ' ').replace(/\s+/g, ' ').trim();
      if (BARE.has(label)) problems.push(`${rel}:${line(text, offset + m.index)} bare CTA "${label}" — name the object`);
      if (/^Exit Room$/i.test(label)) problems.push(`${rel}:${line(text, offset + m.index)} "Exit Room" — the flow calls it "Leave session"`);
    }
  }
}

if (problems.length) {
  console.error(`Copy rules failed (${problems.length}):`);
  for (const p of problems) console.error(`  - ${p}`);
  process.exit(1);
}
console.log(`Copy rules passed: ${files.length} files.`);
