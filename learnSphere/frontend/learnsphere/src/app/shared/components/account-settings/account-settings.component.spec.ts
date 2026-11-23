import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { ThemeService } from '@core/services/theme.service';
import { UserRole } from '@core/models/user.model';
import { SettingsComponent as StudentSettings } from '../../../features/student/settings/settings.component';
import { SettingsComponent as TeacherSettings } from '../../../features/teacher/settings/settings.component';
import { SettingsComponent as AdminSettings } from '../../../features/admin/settings/settings.component';

/**
 * W7-12 / AUD-13 (audit.md D4; BR-24, spec SCR-26/41/61): settings show only what is real.
 * Theme, Change password and Sign out work; account facts come from `GET /users/me`; every
 * preference the server cannot store stays visible but disabled and says so. Nothing claims
 * "Changes saved".
 */
type Case = { role: 'student' | 'teacher' | 'admin'; component: Type<unknown>; userRole: UserRole };
const CASES: Case[] = [
  { role: 'student', component: StudentSettings, userRole: UserRole.STUDENT },
  { role: 'teacher', component: TeacherSettings, userRole: UserRole.TEACHER },
  { role: 'admin', component: AdminSettings, userRole: UserRole.ADMIN },
];

describe.each(CASES)('$role settings (W7-12)', ({ role, component, userRole }) => {
  let fixture: ComponentFixture<unknown>;
  let el: HTMLElement;
  let auth: {
    currentUser: unknown;
    fetchCurrentUser: ReturnType<typeof vi.fn>;
    forgotPassword: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };
  let theme: { isDarkMode: boolean; toggleTheme: ReturnType<typeof vi.fn> };
  let notify: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn>; info: ReturnType<typeof vi.fn> };
  let navigate: ReturnType<typeof vi.spyOn>;

  const server = {
    id: 7, fullName: 'Asha Rao', email: `${role}@learnsphere.test`, role: userRole,
    collegeName: 'TCET', collegeVerificationStatus: 'VERIFIED' as const,
  };

  async function setUp(reset = of({ message: `Reset link sent to ${server.email}` })) {
    auth = {
      currentUser: { ...server, fullName: 'Stale Name', collegeVerificationStatus: undefined },
      fetchCurrentUser: vi.fn(() => of(server)),
      forgotPassword: vi.fn(() => reset),
      logout: vi.fn(),
    };
    theme = {
      isDarkMode: false,
      toggleTheme: vi.fn(() => { theme.isDarkMode = !theme.isDarkMode; }),
    };
    notify = { success: vi.fn(), error: vi.fn(), info: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [component],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: ThemeService, useValue: theme },
        { provide: NotificationService, useValue: notify },
      ],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(component);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    el = fixture.nativeElement as HTMLElement;
  }

  const tab = (name: string) => {
    const b = Array.from(el.querySelectorAll('.settings-nav button')).find((x) => x.textContent?.trim() === name) as HTMLButtonElement;
    b.click();
    fixture.detectChanges();
  };
  const row = (label: string) =>
    Array.from(el.querySelectorAll('.setting-row')).find((r) => r.querySelector('h3')?.textContent?.trim() === label) as HTMLElement | undefined;
  const button = (text: string) =>
    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === text) as HTMLButtonElement | undefined;
  const allTabs = () => Array.from(el.querySelectorAll('.settings-nav button')).map((b) => b.textContent!.trim());

  it('has no fake Save button and never claims "Changes saved"', async () => {
    await setUp();
    expect(button('Save changes')).toBeUndefined();
    for (const name of allTabs()) {
      tab(name);
      el.querySelectorAll<HTMLButtonElement>('.setting-row button').forEach((b) => b.click());
      fixture.detectChanges();
      expect(el.textContent).not.toContain('Changes saved');
    }
    expect(notify.info).not.toHaveBeenCalled();
  });

  it('reads the account fresh from GET /users/me', async () => {
    await setUp();
    expect(auth.fetchCurrentUser).toHaveBeenCalledTimes(1);
    expect(el.querySelector('.settings-user b')?.textContent).toBe('Asha Rao');
  });

  it('Sign out calls the logout API and goes to /login', async () => {
    await setUp();
    tab('Account');
    button('Sign out')!.click();
    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('Change password sends a real reset link to the account e-mail', async () => {
    await setUp();
    tab('Account');
    row('Password')!.querySelector('button')!.click();
    expect(auth.forgotPassword).toHaveBeenCalledWith({ email: server.email });
    expect(notify.success).toHaveBeenCalledWith(`Reset link sent to ${server.email}`);
    expect(row('Password')!.textContent).not.toMatch(/Last changed \d+ days ago/);
  });

  it('a failed reset request reports the failure, not success', async () => {
    await setUp(throwError(() => ({ status: 500 })));
    tab('Account');
    row('Password')!.querySelector('button')!.click();
    expect(notify.success).not.toHaveBeenCalled();
    expect(notify.error).toHaveBeenCalled();
  });

  it('the theme switch is real: it reflects and changes the saved theme', async () => {
    await setUp();
    tab('General');
    const sw = row('Dark mode')!.querySelector('button.toggle') as HTMLButtonElement;
    expect(sw.getAttribute('role')).toBe('switch');
    expect(sw.getAttribute('aria-checked')).toBe('false');
    expect(sw.disabled).toBe(false);
    sw.click();
    fixture.detectChanges();
    expect(theme.toggleTheme).toHaveBeenCalledTimes(1);
    expect(sw.getAttribute('aria-checked')).toBe('true');
  });

  it('every other preference is disabled and says it is not available yet', async () => {
    await setUp();
    let unavailable = 0;
    for (const name of allTabs()) {
      tab(name);
      for (const r of Array.from(el.querySelectorAll<HTMLElement>('.setting-row'))) {
        const label = r.querySelector('h3')!.textContent!.trim();
        if (['Dark mode', 'Password'].includes(label) || r.querySelector('a')) continue;
        const controls = Array.from(r.querySelectorAll<HTMLButtonElement>('button'));
        if (controls.length === 0) continue; // a plain fact row
        unavailable++;
        controls.forEach((c) => expect(c.disabled, label).toBe(true));
        controls.filter((c) => c.classList.contains('toggle')).forEach((c) => expect(c.classList.contains('on'), label).toBe(false));
        expect(r.textContent, label).toContain('Not available yet');
      }
    }
    expect(unavailable).toBeGreaterThan(5);
  });

  if (role === 'student') {
    it('college e-mail status comes from the server', async () => {
      await setUp();
      tab('Account');
      expect(row('College email')!.querySelector('.setting-value')!.textContent!.trim()).toBe('Verified');
    });
  }

  if (role === 'teacher') {
    it('shows no invented payout account', async () => {
      await setUp();
      tab('Account');
      expect(el.textContent).not.toContain('SBI');
      expect(el.textContent).not.toContain('4821');
    });
  }

  if (role === 'admin') {
    it('shows the real role and links admin management to User management', async () => {
      await setUp();
      tab('Account');
      expect(row('Admin role')!.querySelector('.setting-value')!.textContent!.trim()).toBe('Administrator');
      expect(el.textContent).not.toContain('Super Admin');
      const link = row('Manage admins')!.querySelector('a') as HTMLAnchorElement;
      expect(link.getAttribute('href')).toBe('/admin/users');
    });
  }
});
