/**
 * W7-05 ([G-F6]; ADR-06; PLAN §7 "State"): client state lives in Angular signals and
 * feature services. The empty NgRx-style `store/` scaffolding is gone and must not return,
 * no store library is added, and new state holders use signals, not `BehaviorSubject`.
 *
 * The services listed in LEGACY_SUBJECT_STATE predate the rule. They keep working; when one
 * is rewritten to signals, delete it from the list (the list may only shrink).
 */

let fs: any; // node:fs and node:path, loaded in beforeAll
let path: any;

const LEGACY_SUBJECT_STATE = [
  'src/app/core/auth/auth.store.ts',
  'src/app/core/interceptors/error.interceptor.ts',
  'src/app/core/services/lecture-context.service.ts',
  'src/app/core/services/notification-state.service.ts',
  'src/app/core/services/sidebar.service.ts',
  'src/app/core/services/theme.service.ts',
  'src/app/core/services/websocket.service.ts',
];

const STORE_LIBRARIES = /^(@ngrx\/|@ngxs\/|@datorama\/akita|@ngneat\/elf|@ngneat\/elf-|@rx-angular\/state|@ngrx$)/;

const walk = (dir: string): string[] =>
  fs.readdirSync(dir, { withFileTypes: true }).flatMap((e: { name: string; isDirectory(): boolean }) =>
    e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]);
const posix = (p: string) => p.split(path.sep).join('/');

describe('State convention (W7-05)', () => {
  beforeAll(async () => {
    // @ts-ignore -- spec runs under Node (vitest); the app tsconfig has no Node types.
    fs = await import('node:fs');
    // @ts-ignore -- as above.
    path = await import('node:path');
  });

  it('has no store/ scaffolding', () => {
    expect(fs.existsSync('src/app/store')).toBe(false);
  });

  it('depends on no store library', () => {
    const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'));
    const names = Object.keys({ ...pkg.dependencies, ...pkg.devDependencies });
    expect(names.filter((n) => STORE_LIBRARIES.test(n))).toEqual([]);
  });

  it('adds no BehaviorSubject state holder outside the legacy list', () => {
    const holders = walk('src/app')
      .filter((f) => f.endsWith('.ts') && !f.endsWith('.spec.ts') && !f.endsWith('.testing.ts'))
      .filter((f) => /new BehaviorSubject\b/.test(fs.readFileSync(f, 'utf8')))
      .map(posix)
      .sort();
    expect(holders.filter((f) => !LEGACY_SUBJECT_STATE.includes(f))).toEqual([]);
  });

  it('keeps the legacy list honest (remove entries that no longer hold a BehaviorSubject)', () => {
    const stale = LEGACY_SUBJECT_STATE.filter(
      (f) => !fs.existsSync(f) || !/new BehaviorSubject\b/.test(fs.readFileSync(f, 'utf8')));
    expect(stale).toEqual([]);
  });
});
