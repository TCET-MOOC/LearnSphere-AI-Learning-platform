#!/usr/bin/env node
/**
 * Design-system style ratchet (DS-01 / W1-12).
 *
 * Reads `stylelint.config.mjs` as the single source of truth for the bans,
 * counts violations per file, and compares them with `stylelint.baseline.json`.
 *
 * Ratchet semantics:
 *   - the global total may not rise, and
 *   - no individual file may rise above its own baseline count.
 * Both must hold, so a new violation in a touched file fails even when another
 * file improved. Reductions are always allowed; `--update` re-baselines them.
 *
 * Enforced metrics are the three DS-01 bans (`color-no-hex`, `radius-literal`,
 * `transition-all`). The rest of the F24 inventory is recorded and reported but
 * not enforced yet — later stories own them (NFR-04(b), DS-13).
 *
 * Usage:
 *   node scripts/stylelint-ratchet.mjs            # gate
 *   node scripts/stylelint-ratchet.mjs --update   # rewrite the baseline
 */
import { readFile, writeFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import stylelint from 'stylelint';

const ROOT = path.resolve(import.meta.dirname, '..');
const SRC = path.join(ROOT, 'src');
const BASELINE_PATH = path.join(ROOT, 'stylelint.baseline.json');
const UPDATE = process.argv.includes('--update');

/** Token layer: exempt from every rule (see `stylelint.config.mjs`). */
const EXEMPT = new Set(['src/styles/_tokens.scss', 'src/styles/_variables.scss']);

const METRICS = [
  {
    id: 'color-no-hex',
    label: 'raw hex colours',
    enforced: true,
    rule: 'NFR-04(a)',
  },
  {
    id: 'radius-literal',
    label: 'literal radius values',
    enforced: true,
    rule: 'NFR-04(c)',
  },
  {
    id: 'transition-all',
    label: '`transition: all` declarations',
    enforced: true,
    rule: 'NFR-04(e)',
  },
  {
    id: 'uppercase',
    label: 'uppercase text-transform rules',
    enforced: false,
    rule: 'NFR-04(b) — owned by DS-13',
  },
  {
    id: 'inline-style',
    label: 'inline template style attributes',
    enforced: false,
    rule: 'owned by the screen-conversion stories',
  },
  {
    id: 'hover-lift',
    label: 'translateY hover lifts',
    enforced: false,
    rule: 'owned by DS-13',
  },
  {
    id: 'backdrop-filter',
    label: 'backdrop-filter glass surfaces',
    enforced: false,
    rule: 'NFR-01 — inventory only, glass is preserved',
  },
  {
    id: 'icon-families',
    label: 'icon families in use',
    enforced: false,
    rule: 'owned by DS-07',
  },
];

const toPosix = (p) => p.split(path.sep).join('/');
const rel = (p) => toPosix(path.relative(ROOT, p));

async function walk(dir) {
  const out = [];
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...(await walk(full)));
    else out.push(full);
  }
  return out;
}

function bump(map, file, by = 1) {
  map.set(file, (map.get(file) ?? 0) + by);
}

/** Metric id for a stylelint warning, or null when it is not one of ours. */
function metricForWarning(warning) {
  if (warning.rule === 'color-no-hex') return 'color-no-hex';
  if (warning.rule !== 'declaration-property-value-disallowed-list') return null;
  const property = /for property "([^"]+)"/.exec(warning.text ?? '')?.[1] ?? '';
  if (property.includes('radius')) return 'radius-literal';
  if (property.startsWith('transition')) return 'transition-all';
  return null;
}

function countMatches(text, pattern) {
  return (text.match(pattern) ?? []).length;
}

async function collect() {
  const counts = new Map(METRICS.map((m) => [m.id, new Map()]));

  const scssFiles = [];
  const htmlFiles = [];
  for (const file of await walk(SRC)) {
    const relative = rel(file);
    if (EXEMPT.has(relative)) continue;
    if (file.endsWith('.scss')) scssFiles.push(file);
    else if (file.endsWith('.html')) htmlFiles.push(file);
  }

  // The three DS-01 bans come from stylelint itself (SCSS-aware parsing).
  const lint = await stylelint.lint({
    files: scssFiles,
    configFile: path.join(ROOT, 'stylelint.config.mjs'),
    formatter: 'json',
  });
  const results = JSON.parse(lint.report);
  let parseErrors = 0;
  for (const result of results) {
    const file = rel(result.source);
    for (const warning of result.warnings) {
      if (warning.rule === 'CssSyntaxError') {
        parseErrors += 1;
        continue;
      }
      const metric = metricForWarning(warning);
      if (metric) bump(counts.get(metric), file);
    }
  }
  if (parseErrors > 0) {
    throw new Error(
      `stylelint could not parse ${parseErrors} SCSS file(s); fix the syntax before measuring the ratchet.`,
    );
  }

  // The remaining F24 inventory is textual: stylelint cannot parse templates.
  const texts = new Map();
  for (const file of [...scssFiles, ...htmlFiles]) {
    texts.set(rel(file), await readFile(file, 'utf8'));
  }

  for (const [file, text] of texts) {
    const isScss = file.endsWith('.scss');
    if (isScss) {
      const uppercase = countMatches(text, /text-transform\s*:\s*uppercase/g);
      if (uppercase) bump(counts.get('uppercase'), file, uppercase);
      const lifts = countMatches(text, /translateY\(/g);
      if (lifts) bump(counts.get('hover-lift'), file, lifts);
      const glass = countMatches(text, /backdrop-filter/g);
      if (glass) bump(counts.get('backdrop-filter'), file, glass);
    } else {
      const inline = countMatches(text, /style\s*=\s*["']/g);
      if (inline) bump(counts.get('inline-style'), file, inline);
      const uppercase = countMatches(text, /text-transform\s*:\s*uppercase/g);
      if (uppercase) bump(counts.get('uppercase'), file, uppercase);
    }
  }

  // Two icon families ship today: Lucide components and the Material Icons font.
  const iconFamilies = new Map();
  for (const [file, text] of texts) {
    if (!file.endsWith('.html')) continue;
    const material = countMatches(text, /<mat-icon\b/g);
    const lucide = countMatches(text, /<lucide-icon\b|<lucide-[a-z-]+\b/g);
    if (material) iconFamilies.set(`material:${file}`, material);
    if (lucide) iconFamilies.set(`lucide:${file}`, lucide);
  }
  const families = new Set([...iconFamilies.keys()].map((k) => k.split(':')[0]));
  counts.get('icon-families').set('app-wide', families.size);

  const metrics = {};
  for (const metric of METRICS) {
    const perFile = counts.get(metric.id);
    const files = {};
    let total = 0;
    for (const [file, count] of [...perFile].sort(([a], [b]) => a.localeCompare(b))) {
      if (count <= 0) continue;
      files[file] = count;
      total += count;
    }
    metrics[metric.id] = { enforced: metric.enforced, rule: metric.rule, total, files };
  }
  return metrics;
}

function flatten(baseline) {
  const out = new Map();
  for (const metric of METRICS) {
    const entry = baseline.metrics?.[metric.id];
    const files = new Map(Object.entries(entry?.files ?? {}));
    out.set(metric.id, { total: entry?.total ?? 0, files });
  }
  return out;
}

function describeRegressions(metrics, base) {
  const failures = [];
  for (const metric of METRICS.filter((m) => m.enforced)) {
    const current = metrics[metric.id];
    const previous = base.get(metric.id);
    if (current.total > previous.total) {
      failures.push(
        `${metric.id}: total ${current.total} > baseline ${previous.total} (+${current.total - previous.total})`,
      );
    }
    for (const [file, count] of Object.entries(current.files)) {
      const allowed = previous.files.get(file) ?? 0;
      if (count > allowed) {
        failures.push(`${metric.id}: ${file} has ${count}, baseline allows ${allowed}`);
      }
    }
  }
  return failures;
}

const metrics = await collect();
const payload = {
  $comment:
    'DS-01 style ratchet baseline. Counts may only fall; regenerate with `npm run lint:styles:update`.',
  scope: 'src/**/*.scss + src/**/*.html, excluding src/styles/_tokens.scss and src/styles/_variables.scss',
  metrics,
};

if (UPDATE) {
  payload.updatedAt = new Date().toISOString().slice(0, 10);
  await writeFile(BASELINE_PATH, JSON.stringify(payload, null, 2) + '\n', 'utf8');
  console.log(`stylelint ratchet baseline written to ${rel(BASELINE_PATH)}`);
  for (const metric of METRICS) {
    console.log(`  ${metric.id.padEnd(16)} ${String(metrics[metric.id].total).padStart(5)}`);
  }
  process.exit(0);
}

let baseline;
try {
  const raw = await readFile(BASELINE_PATH, 'utf8');
  baseline = JSON.parse(raw.replace(/^\uFEFF/, ''));
} catch (error) {
  console.error(`Cannot read ${rel(BASELINE_PATH)}: ${error.message}`);
  console.error('Run `npm run lint:styles:update` once to create it.');
  process.exit(2);
}

const base = flatten(baseline);
const failures = describeRegressions(metrics, base);

console.log('DS-01 style ratchet');
for (const metric of METRICS) {
  const current = metrics[metric.id];
  const previous = base.get(metric.id);
  const delta = current.total - previous.total;
  const status = !metric.enforced ? 'info' : delta > 0 ? 'FAIL' : delta < 0 ? 'down' : 'ok';
  const deltaText = delta === 0 ? '=' : `${delta > 0 ? '+' : ''}${delta}`;
  console.log(
    `  ${status.padEnd(4)} ${metric.id.padEnd(16)} ${String(current.total).padStart(5)} ` +
      `baseline ${String(previous.total).padStart(5)} (${deltaText}) ` +
      `${Object.keys(current.files).length} file(s)${metric.enforced ? '' : ' [advisory]'}`,
  );
}

if (failures.length > 0) {
  console.error('\nStyle ratchet failed — violations may only fall:');
  for (const failure of failures) console.error(`  - ${failure}`);
  console.error('\nUse a token, or re-baseline deliberately with `npm run lint:styles:update`.');
  process.exit(1);
}

console.log('\nStyle ratchet passed.');
