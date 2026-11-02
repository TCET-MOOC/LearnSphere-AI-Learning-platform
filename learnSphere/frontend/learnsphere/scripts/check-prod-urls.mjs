#!/usr/bin/env node
/**
 * W7-03 gate: a production build must not point at a developer machine.
 *
 * Scans the built browser bundle (dist/learnsphere/browser) for `localhost`,
 * `127.0.0.1` and the dev ports of the backend (8080) and payments service (8081).
 * Production reaches the APIs through same-origin paths (`/api`, `/api/payments`,
 * `/ws-native`) that nginx proxies (nginx.conf).
 *
 * Usage: npm run build && npm run check:prod-urls
 */
import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';

const DIST = path.resolve(import.meta.dirname, '..', 'dist', 'learnsphere', 'browser');
const BANNED = [/localhost/g, /127\.0\.0\.1/g, /:8080\b/g, /:8081\b/g];

const files = (await readdir(DIST, { recursive: true }))
  .filter((f) => /\.(js|html|css|json)$/.test(f))
  .map((f) => path.join(DIST, f));

const hits = [];
for (const file of files) {
  const text = await readFile(file, 'utf8');
  for (const re of BANNED) {
    for (const m of text.matchAll(re)) {
      const at = m.index ?? 0;
      hits.push(`${path.relative(DIST, file)}: ${JSON.stringify(text.slice(Math.max(0, at - 40), at + 40))}`);
    }
  }
}

if (hits.length) {
  console.error(`Production bundle points at a dev machine (${hits.length} hit(s)):`);
  for (const h of hits.slice(0, 20)) console.error('  ' + h);
  process.exit(1);
}
console.log(`Production URL check passed (${files.length} files).`);
