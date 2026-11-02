/**
 * Every Lucide icon a template names must be registered in app.config.ts.
 *
 * An unregistered icon throws inside Angular's render, which leaves the rest of that
 * template unrendered until some later change-detection pass repairs it. With the
 * eager route table that later pass usually came; after the W7-02 lazy split, Moderation
 * (SCR-56) and Sentiment (SCR-57) stayed half-drawn and the landing bar lost its links.
 */
import { TestBed } from '@angular/core/testing';
import { LUCIDE_ICONS, LucideIconProviderInterface } from 'lucide-angular';
import { appConfig } from './app.config';

const pascal = (name: string) => name.split('-').filter(Boolean).map((p) => p[0].toUpperCase() + p.slice(1)).join('');

async function iconNamesInSource(): Promise<Map<string, Set<string>>> {
  // @ts-ignore -- spec runs under Node (vitest); the app tsconfig has no Node types.
  const fs = await import('node:fs');
  // @ts-ignore -- as above.
  const path = await import('node:path');
  const walk = (dir: string): string[] =>
    fs.readdirSync(dir, { withFileTypes: true }).flatMap((e: { name: string; isDirectory(): boolean }) =>
      e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]);
  const found = new Map<string, Set<string>>();
  const add = (name: string, file: string) => {
    if (!found.has(name)) found.set(name, new Set());
    found.get(name)!.add(file.split(path.sep).join('/'));
  };
  for (const file of walk('src/app').filter((f) => /\.(html|ts)$/.test(f) && !f.endsWith('.spec.ts'))) {
    const text: string = fs.readFileSync(file, 'utf8');
    for (const tag of text.matchAll(/<lucide-icon\b[^>]*>/g)) {
      const fixed = tag[0].match(/\sname="([a-z0-9-]+)"/);
      if (fixed) add(fixed[1], file);
      const bound = tag[0].match(/\[name\]="([^"]*)"/);
      // A bare literal, or the results of a ternary (not the values it compares against).
      if (bound) for (const lit of bound[1].matchAll(/(?:^\s*|[?:]\s*)'([a-z][a-z0-9-]*)'/g)) add(lit[1], file);
    }
    // Icon names kept as data for templates to bind (core/content/*.ts).
    if (file.split(path.sep).join('/').includes('core/content/')) {
      for (const m of text.matchAll(/\bicon\s*:\s*'([a-z][a-z0-9-]*)'/g)) add(m[1], file);
    }
  }
  return found;
}

describe('Lucide icon registry', () => {
  it('registers every icon name the templates use', async () => {
    TestBed.configureTestingModule({ providers: [...(appConfig.providers ?? [])] });
    const providers = TestBed.inject(LUCIDE_ICONS) as unknown as LucideIconProviderInterface[];
    const list = Array.isArray(providers) ? providers : [providers];
    const registered = (name: string) => list.some((p) => p.hasIcon(pascal(name)));

    const used = await iconNamesInSource();
    expect(used.size).toBeGreaterThan(50);
    const missing = [...used.entries()]
      .filter(([name]) => !registered(name))
      .map(([name, files]) => `${name} (${[...files].join(', ')})`)
      .sort();
    expect(missing).toEqual([]);
  });
});
