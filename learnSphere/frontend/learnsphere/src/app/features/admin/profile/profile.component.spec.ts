import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LucideAngularModule, CheckCircle2, Plus } from 'lucide-angular';
import { ProfileComponent } from './profile.component';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { ReportsService } from '../services/reports.service';
import { AdminService } from '../services/admin.service';
import { UserRole } from '@core/models/user.model';

/**
 * W7-11 / AUD-12 (audit.md D3): the admin profile shows the server's profile and real platform
 * figures, Save writes through `PUT /users/me`, and no action claims success it did not have.
 */
describe('Admin ProfileComponent (W7-11)', () => {
  let fixture: ComponentFixture<ProfileComponent>;
  let el: HTMLElement;
  let auth: {
    currentUser: unknown;
    fetchCurrentUser: ReturnType<typeof vi.fn>;
    updateProfile: ReturnType<typeof vi.fn>;
    forgotPassword: ReturnType<typeof vi.fn>;
  };
  let notify: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn>; info: ReturnType<typeof vi.fn> };

  const server = {
    id: 3, fullName: 'Rohan Gupta', email: 'admin@learnsphere.test', role: UserRole.ADMIN,
    bio: 'Runs platform operations.', createdAt: '2026-01-01T00:00:00',
  };

  async function setUp(updateResult = of({ ...server, fullName: 'Rohan K. Gupta', bio: 'New bio' })) {
    auth = {
      currentUser: { ...server, fullName: 'Stale Admin', bio: undefined },
      fetchCurrentUser: vi.fn(() => of(server)),
      updateProfile: vi.fn(() => updateResult),
      forgotPassword: vi.fn(() => of({ message: 'Reset link sent to admin@learnsphere.test' })),
    };
    notify = { success: vi.fn(), error: vi.fn(), info: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ProfileComponent, LucideAngularModule.pick({ CheckCircle2, Plus })],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: NotificationService, useValue: notify },
        {
          provide: ReportsService,
          useValue: {
            getSummary: () => of({
              totalUsers: 6452, usersByRole: {}, totalCourses: 214, coursesByStatus: {},
              totalRevenue: 0, totalCertificatesIssued: 0, flaggedContentPending: 2,
            }),
          },
        },
        { provide: AdminService, useValue: { getColleges: () => of([{ id: 1 }, { id: 2 }, { id: 3 }]) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProfileComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  const input = (label: string) =>
    Array.from(el.querySelectorAll('label')).find((l) => l.textContent?.includes(label))!.querySelector('input, textarea') as
      HTMLInputElement | HTMLTextAreaElement;
  const button = (text: string) =>
    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim().includes(text)) as HTMLButtonElement | undefined;

  it('loads the profile fresh from GET /users/me, not the cached snapshot', async () => {
    await setUp();
    expect(auth.fetchCurrentUser).toHaveBeenCalledTimes(1);
    expect(input('Display name').value).toBe('Rohan Gupta');
    expect(input('About').value).toBe('Runs platform operations.');
    expect(input('Role and institution').value).toBe('Administrator · LearnSphere platform');
  });

  it('shows only real platform figures — no invented uptime or health', async () => {
    await setUp();
    const stats = Array.from(el.querySelectorAll('.stat')).map(
      (s) => `${s.querySelector('strong')?.textContent?.trim()} ${s.querySelector('span')?.textContent?.trim()}`,
    );
    expect(stats).toEqual(['6,452 Users', '214 Courses', '3 Colleges', '2 Open flags']);
    expect(el.textContent).not.toContain('99.9%');
  });

  it('saves name and bio through PUT /users/me; success only after the server accepts', async () => {
    await setUp();
    button('Edit profile')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    const name = input('Display name');
    name.value = 'Rohan K. Gupta';
    name.dispatchEvent(new Event('input'));
    const bio = input('About');
    bio.value = 'New bio';
    bio.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    button('Save profile')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(auth.updateProfile).toHaveBeenCalledWith({ fullName: 'Rohan K. Gupta', bio: 'New bio' });
    expect(notify.success).toHaveBeenCalled();
    expect(el.querySelector('h2')?.textContent).toContain('Rohan K. Gupta');
  });

  it('a failed save shows an error, keeps edit mode and claims no success', async () => {
    await setUp(throwError(() => ({ status: 500 })));
    button('Edit profile')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    button('Save profile')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(notify.success).not.toHaveBeenCalled();
    expect(notify.error).toHaveBeenCalled();
    expect(button('Save profile')).toBeTruthy();
  });

  it('has no fake avatar upload, and Change password sends a real reset link', async () => {
    await setUp();
    button('Edit profile')!.click();
    fixture.detectChanges();
    expect(el.querySelector('.profile-avatar button')).toBeNull();
    expect(input('Role and institution').disabled).toBe(true);

    button('Change password')!.click();
    expect(auth.forgotPassword).toHaveBeenCalledWith({ email: 'admin@learnsphere.test' });
    expect(notify.success).toHaveBeenCalledWith('Reset link sent to admin@learnsphere.test');
    expect(notify.info).not.toHaveBeenCalled();
  });
});
