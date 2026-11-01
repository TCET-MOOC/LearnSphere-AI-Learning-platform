import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BarChart2, LucideAngularModule, icons } from 'lucide-angular';
import { MobileNavComponent } from './mobile-nav.component';
import { AuthService } from '@core/auth/auth.service';
import { ThemeService } from '@core/services/theme.service';
import { MOBILE_NAV, MobileRole } from '@core/content/mobile-nav.content';

/**
 * Approved mobile redesign (360 px): bottom bar with the role's four most-used destinations
 * plus More; the More sheet holds every other destination, profile, theme and sign out.
 */
describe('MobileNavComponent (mobile shell)', () => {
  let fixture: ComponentFixture<MobileNavComponent>;
  let auth: { logout: ReturnType<typeof vi.fn>; currentUser: any };
  let theme: any;

  /** Every destination in each role's desktop sidebar, plus the profile. */
  const SIDEBAR: Record<MobileRole, string[]> = {
    STUDENT: ['/student/dashboard', '/student/courses', '/student/explore', '/student/notes', '/student/assessments',
      '/student/bookmarks', '/student/leaderboard', '/student/discussion', '/student/live', '/student/certificates',
      '/student/purchases', '/student/settings', '/student/profile', '/student/messages', '/student/announcements'],
    TEACHER: ['/teacher/dashboard', '/teacher/courses', '/teacher/upload', '/teacher/students', '/teacher/messages',
      '/teacher/discussion', '/teacher/live', '/teacher/trending', '/teacher/royalties', '/teacher/settings',
      '/teacher/profile', '/teacher/announcements'],
    ADMIN: ['/admin/dashboard', '/admin/users', '/admin/courses', '/admin/colleges', '/admin/flagged', '/admin/sentiment',
      '/admin/revenue', '/admin/payouts', '/admin/reports', '/admin/announcements', '/admin/messages', '/admin/settings',
      '/admin/profile']
  };

  async function render(role: MobileRole) {
    auth = { logout: vi.fn(), currentUser: { fullName: 'Aarav Sharma', email: 'student@learnsphere.test', role } };
    theme = { toggleTheme: vi.fn(), isDarkMode: false } as any;
    await TestBed.configureTestingModule({
      imports: [MobileNavComponent, LucideAngularModule.pick({ ...icons, BarChart2 })],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        { provide: AuthService, useValue: auth },
        { provide: ThemeService, useValue: theme }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(MobileNavComponent);
    fixture.componentRef.setInput('role', role);
    fixture.detectChanges();
  }

  const el = () => fixture.nativeElement as HTMLElement;
  const barLabels = () => Array.from(el().querySelectorAll('.mobile-bar > *')).map((n) => n.textContent!.trim());
  const openMore = () => {
    (el().querySelector('.mobile-bar__more') as HTMLButtonElement).click();
    fixture.detectChanges();
  };

  it('shows the student bar: Home, My Courses, Live, Tests, More', async () => {
    await render('STUDENT');
    expect(barLabels()).toEqual(['Home', 'My Courses', 'Live', 'Tests', 'More']);
  });

  it('shows the teacher bar: Home, Courses, Live, Messages, More', async () => {
    await render('TEACHER');
    expect(barLabels()).toEqual(['Home', 'Courses', 'Live', 'Messages', 'More']);
  });

  it('shows the admin bar: Home, Users, Approvals, Reports, More', async () => {
    await render('ADMIN');
    expect(barLabels()).toEqual(['Home', 'Users', 'Approvals', 'Reports', 'More']);
  });

  for (const role of ['STUDENT', 'TEACHER', 'ADMIN'] as MobileRole[]) {
    it(`keeps every ${role.toLowerCase()} destination reachable from the bar or the More sheet`, async () => {
      await render(role);
      openMore();
      const hrefs = new Set(Array.from(el().querySelectorAll('a[href]')).map((a) => a.getAttribute('href')));
      for (const route of SIDEBAR[role]) expect(hrefs, `${route} unreachable`).toContain(route);
      expect(MOBILE_NAV[role].bar).toHaveLength(4);
    });
  }

  it('opens the More sheet as a modal dialog and closes it with Escape', async () => {
    await render('STUDENT');
    expect(el().querySelector('[role="dialog"]')).toBeNull();
    openMore();
    const dialog = el().querySelector('[role="dialog"]')!;
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    dialog.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect(el().querySelector('[role="dialog"]')).toBeNull();
  });

  it('closes the sheet when a destination is chosen', async () => {
    await render('STUDENT');
    openMore();
    (el().querySelector('[role="dialog"] a[href="/student/notes"]') as HTMLAnchorElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[role="dialog"]')).toBeNull();
  });

  it('signs out from the sheet and goes to the login page', async () => {
    await render('STUDENT');
    const router = TestBed.inject(Router);
    const nav = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    openMore();
    (Array.from(el().querySelectorAll('[role="dialog"] button')).find((b) => b.textContent!.includes('Sign out')) as HTMLButtonElement).click();
    expect(auth.logout).toHaveBeenCalled();
    expect(nav).toHaveBeenCalledWith(['/login']);
  });

  it('toggles the dark theme from the sheet', async () => {
    await render('STUDENT');
    openMore();
    (el().querySelector('[role="dialog"] input[role="switch"]') as HTMLInputElement).click();
    expect(theme.toggleTheme).toHaveBeenCalled();
  });
});
