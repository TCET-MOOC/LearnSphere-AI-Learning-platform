#!/usr/bin/env node
/**
 * Template accessibility lint (W7-08; `spec §7` "keyboard reachable", `PLAN §7` Accessibility).
 *
 * Fails on, in every `*.component.html` and inline `template:`:
 *   1. a non-interactive element (div, span, li, article, section, tr, td, p, h1–h6, img, lucide-icon)
 *      with a `(click)` handler but no keyboard path — add `appActivate`, or use a <button>/<a>;
 *      exempt: backdrops/overlays (Escape closes the dialog via `appDialog`) and handlers that only
 *      call `$event.stopPropagation()`;
 *   2. an `<a>` with `(click)` but no `href`/`routerLink` (not focusable);
 *   3. an `<img>` without `alt` / `[alt]`.
 *
 * Usage: node scripts/a11y-lint.mjs
 */
import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');
const APP = path.join(ROOT, 'src', 'app');

const NON_INTERACTIVE = /<(div|span|li|article|section|tr|td|p|h[1-6]|img|lucide-icon)\b[^>]*>/gs;
const KEYBOARD_PATH = /\bappActivate\b|\(keydown[.\w]*\)|\(keyup[.\w]*\)/;
const EXEMPT_CLICK = /\(click\)="\$event\.stopPropagation\(\)"/;
const BACKDROP = /class="[^"]*(backdrop|overlay)[^"]*"/;

export function lintTemplate(template) {
  const t = template.replace(/<!--[\s\S]*?-->/g, '');
  const problems = [];
  const line = (i) => t.slice(0, i).split('\n').length;
  for (const m of t.matchAll(NON_INTERACTIVE)) {
    const tag = m[0];
    if (!/\(click\)=/.test(tag) || KEYBOARD_PATH.test(tag) || EXEMPT_CLICK.test(tag) || BACKDROP.test(tag)) continue;
    problems.push(`${line(m.index)}: click-only <${m[1]}> has no keyboard path (add appActivate or use a button)`);
  }
  for (const m of t.matchAll(/<a\b[^>]*>/gs)) {
    const tag = m[0];
    if (/\(click\)=/.test(tag) && !/\shref=|\[href\]|routerLink|\[routerLink\]|appActivate/.test(tag)) {
      problems.push(`${line(m.index)}: <a (click)> without href/routerLink is not focusable`);
    }
  }
  for (const m of t.matchAll(/<img\b[^>]*>/gs)) {
    if (!/\salt=|\[alt\]|\[attr\.alt\]/.test(m[0])) problems.push(`${line(m.index)}: <img> without alt`);
  }
  return problems;
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

const failures = [];
let templates = 0;
for (const file of await walk(APP)) {
  const rel = path.relative(ROOT, file).split(path.sep).join('/');
  let source;
  if (rel.endsWith('.component.html')) source = await readFile(file, 'utf8');
  else if (rel.endsWith('.component.ts')) source = (await readFile(file, 'utf8')).match(/\btemplate:\s*`([\s\S]*?)`/)?.[1];
  if (source === undefined) continue;
  templates++;
  for (const p of lintTemplate(source)) failures.push(`${rel}:${p}`);
}

if (failures.length) {
  console.error(`Accessibility lint failed (${failures.length}):\n  - ${failures.join('\n  - ')}`);
  process.exit(1);
}
console.log(`Accessibility lint passed: ${templates} templates.`);
