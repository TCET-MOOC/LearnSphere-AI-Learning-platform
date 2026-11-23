#!/usr/bin/env node
/**
 * W7-13 (NFR-02 "hashed static assets served `immutable`"; NFR-03): serves the production build
 * with the real `nginx.conf` in the same nginx image the Dockerfile uses, then checks the
 * Cache-Control each kind of file gets:
 *   - content-hashed files (`main-XXXXXXXX.js`, `chunk-…`, `styles-…`, `media/…-XXXXXXXX.*`):
 *     exactly one `public, max-age=31536000, immutable`;
 *   - unhashed static files (favicon, logos, manifest): cacheable but never `immutable`
 *     (their name does not change when their content does);
 *   - the SPA shell (`/`, deep links, `index.html`): `no-cache`, so a deploy is picked up at once.
 * Needs Docker and a production build (`npm run build`). Exits non-zero on any mismatch.
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import http from 'node:http';

const root = resolve(import.meta.dirname, '..');
const dist = resolve(root, 'dist/learnsphere/browser');
const conf = resolve(root, 'nginx.conf');
const image = 'nginx:1.27-alpine';
const port = Number(process.env.STATIC_CACHE_PORT ?? 8089);
const name = `ls-static-cache-${process.pid}`;

if (!existsSync(resolve(dist, 'index.html'))) {
  console.error('No production build at dist/learnsphere/browser — run `npm run build` first.');
  process.exit(2);
}

const files = readdirSync(dist);
const hashed = files.find((f) => /^main-[A-Z0-9]{8}\.js$/.test(f));
const chunk = files.find((f) => /^chunk-[A-Z0-9]{8}\.js$/.test(f));
const styles = files.find((f) => /^styles-[A-Z0-9]{8}\.css$/.test(f));
if (!hashed || !chunk || !styles) {
  console.error('Could not find hashed main/chunk/styles files; is outputHashing "all"?');
  process.exit(2);
}

const IMMUTABLE = 'public, max-age=31536000, immutable';
const checks = [
  { path: `/${hashed}`, expect: (cc) => cc.length === 1 && cc[0] === IMMUTABLE, want: IMMUTABLE },
  { path: `/${chunk}`, expect: (cc) => cc.length === 1 && cc[0] === IMMUTABLE, want: IMMUTABLE },
  { path: `/${styles}`, expect: (cc) => cc.length === 1 && cc[0] === IMMUTABLE, want: IMMUTABLE },
  ...['favicon.ico', 'logo.svg', 'site.webmanifest'].filter((f) => files.includes(f)).map((f) => ({
    path: `/${f}`,
    expect: (cc) => cc.length === 1 && !cc[0].includes('immutable') && /max-age=\d+/.test(cc[0]),
    want: 'one Cache-Control with max-age and without immutable',
  })),
  ...['/', '/index.html', '/student/dashboard'].map((path) => ({
    path,
    expect: (cc) => cc.length === 1 && cc[0] === 'no-cache',
    want: 'no-cache',
  })),
  { path: '/healthz', expect: () => true, want: 'any', status: 200 },
];

const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8' }).trim();

/** GET with Node's http client, keeping every raw Cache-Control header line. */
function rawGet(url) {
  return new Promise((resolveGet, reject) => {
    http.get(url, (res) => {
      res.resume();
      const cacheControl = [];
      for (let i = 0; i < res.rawHeaders.length; i += 2) {
        if (res.rawHeaders[i].toLowerCase() === 'cache-control') cacheControl.push(res.rawHeaders[i + 1].trim());
      }
      res.on('end', () => resolveGet({ status: res.statusCode, cacheControl }));
    }).on('error', reject);
  });
}

let failures = 0;
try {
  docker(
    'run', '-d', '--rm', '--name', name, '-p', `127.0.0.1:${port}:80`,
    // nginx resolves proxy_pass hosts at start-up; the API is not exercised here.
    '--add-host', 'backend:127.0.0.1', '--add-host', 'payments:127.0.0.1',
    '-v', `${conf}:/etc/nginx/conf.d/default.conf:ro`,
    '-v', `${dist}:/usr/share/nginx/html:ro`,
    image,
  );

  const base = `http://127.0.0.1:${port}`;
  for (let i = 0; i < 50; i++) {
    try { if ((await fetch(`${base}/healthz`)).ok) break; } catch { /* starting */ }
    await new Promise((r) => setTimeout(r, 200));
  }

  for (const c of checks) {
    const res = await rawGet(base + c.path);
    // One entry per Cache-Control header line, so a duplicated header (e.g. `expires` plus
    // `add_header`) is caught instead of being merged.
    const cc = res.cacheControl;
    const shown = cc.length ? cc.map((v) => `"${v}"`).join(' + ') : '(none)';
    const ok = res.status === (c.status ?? 200) && c.expect(cc);
    if (!ok) {
      failures++;
      console.error(`✗ ${c.path} → ${res.status} Cache-Control: ${shown} — want ${c.want}`);
    } else {
      console.log(`✓ ${c.path} → ${shown}`);
    }
  }
} finally {
  spawnSync('docker', ['rm', '-f', name], { stdio: 'ignore' });
}

if (failures) {
  console.error(`Static cache check failed (${failures}).`);
  process.exit(1);
}
console.log('Static cache check passed.');
