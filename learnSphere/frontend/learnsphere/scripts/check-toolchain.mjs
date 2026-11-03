#!/usr/bin/env node
/**
 * W7-01 gate: the frontend toolchain is Angular 21.2 and stays pinned (ADR-06, [G-F1]).
 *
 * Fails when:
 *  - an `@angular/*` package or TypeScript allows more than patch updates (needs `~`),
 *    or the Angular packages are not all on the 21.2 line;
 *  - the installed (lockfile) Angular version is not 21.2.x;
 *  - `engines.node` is missing, or `.nvmrc`, CI `NODE_VERSION` and the Dockerfile base
 *    image name different Node majors;
 *  - the project docs still claim Angular 19 or do not state Angular 21.2.
 *
 * Usage: npm run check:toolchain
 */
import { readFile } from 'node:fs/promises';
import path from 'node:path';

const APP = path.resolve(import.meta.dirname, '..');
const REPO = path.resolve(APP, '..', '..', '..');
const ANGULAR_LINE = '21.2';
const DOCS = ['Docs/Architecture.md', 'Docs/Frontend.md', 'Docs/Progress.md', 'Docs/learnsphere_system_design.md'];

const read = (p) => readFile(p, 'utf8').catch(() => null);
const problems = [];

const pkg = JSON.parse(await read(path.join(APP, 'package.json')));
const lock = JSON.parse(await read(path.join(APP, 'package-lock.json')));
const deps = { ...pkg.dependencies, ...pkg.devDependencies };

for (const [name, range] of Object.entries(deps)) {
  if (name.startsWith('@angular/')) {
    if (!range.startsWith(`~${ANGULAR_LINE}.`)) problems.push(`${name}: "${range}" must be "~${ANGULAR_LINE}.x"`);
    const installed = lock.packages?.[`node_modules/${name}`]?.version ?? '';
    if (!installed.startsWith(`${ANGULAR_LINE}.`)) problems.push(`${name}: lockfile has ${installed || 'nothing'}, expected ${ANGULAR_LINE}.x`);
  }
}
if (!deps.typescript?.startsWith('~')) problems.push(`typescript: "${deps.typescript}" must be a "~" (patch-only) range`);

const nodeMajor = (s) => s?.match(/(\d+)/)?.[1];
const nvmrc = nodeMajor(await read(path.join(APP, '.nvmrc')));
const ci = nodeMajor((await read(path.join(REPO, '.github/workflows/ci.yml')))?.match(/NODE_VERSION:\s*'?([\d.]+)/)?.[1]);
const docker = nodeMajor((await read(path.join(APP, 'Dockerfile')))?.match(/FROM node:([\d.]+)/)?.[1]);
if (!pkg.engines?.node) problems.push('package.json: engines.node is missing');
if (!nvmrc) problems.push('.nvmrc is missing');
if (!(nvmrc && nvmrc === ci && ci === docker)) {
  problems.push(`Node major differs: .nvmrc=${nvmrc ?? '-'}, CI NODE_VERSION=${ci ?? '-'}, Dockerfile=${docker ?? '-'}`);
}

for (const doc of DOCS) {
  const text = await read(path.join(REPO, doc));
  if (text === null) continue;
  if (/Angular 19/.test(text)) problems.push(`${doc}: still claims Angular 19`);
  if (!text.includes(`Angular ${ANGULAR_LINE}`)) problems.push(`${doc}: does not state Angular ${ANGULAR_LINE}`);
}

if (problems.length) {
  console.error(`Toolchain check failed (${problems.length}):\n  - ${problems.join('\n  - ')}`);
  process.exit(1);
}
console.log(`Toolchain check passed: Angular ${ANGULAR_LINE}, Node ${nvmrc}, docs consistent.`);
