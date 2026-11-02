import { TestBed } from '@angular/core/testing';
import { Route } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { AuthService } from './auth.service';
import { RolePreloadingStrategy } from './role-preloading.strategy';
import { UserRole } from '@core/models/user.model';

describe('RolePreloadingStrategy (W7-02)', () => {
  let role: UserRole | undefined;

  beforeEach(() => {
    role = undefined;
    TestBed.configureTestingModule({
      providers: [
        RolePreloadingStrategy,
        { provide: AuthService, useValue: { get currentUser() { return role ? { role } : null; } } },
      ],
    });
  });

  const group = (preloadFor: UserRole): Route => ({ path: 'x', data: { preloadFor } });

  async function run(route: Route) {
    const strategy = TestBed.inject(RolePreloadingStrategy);
    let loaded = false;
    const result = await firstValueFrom(strategy.preload(route, () => { loaded = true; return of('chunk'); }));
    return { loaded, result };
  }

  it("preloads the signed-in user's own role group", async () => {
    role = UserRole.STUDENT;
    expect(await run(group(UserRole.STUDENT))).toEqual({ loaded: true, result: 'chunk' });
  });

  it("never preloads another role's group", async () => {
    role = UserRole.STUDENT;
    expect(await run(group(UserRole.ADMIN))).toEqual({ loaded: false, result: null });
    expect(await run(group(UserRole.TEACHER))).toEqual({ loaded: false, result: null });
  });

  it('preloads nothing while signed out', async () => {
    expect(await run(group(UserRole.STUDENT))).toEqual({ loaded: false, result: null });
  });

  it('ignores routes that are not marked for preloading', async () => {
    role = UserRole.ADMIN;
    expect(await run({ path: 'login' })).toEqual({ loaded: false, result: null });
  });
});
