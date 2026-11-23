import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { UserRole } from '@core/models/user.model';

/**
 * W7-12 / AUD-10 (audit.md D1; BR-24, spec SCR-26 "validation errors"): the student profile
 * reports failures as prominently as successes, can clear the bio, refuses a blank name before
 * calling the server, and says when the fresh profile could not be loaded.
 */
describe('Student ProfileComponent (W7-12)', () => {
  let fixture: ComponentFixture<ProfileComponent>;
  let el: HTMLElement;
  let auth: {
    currentUser: unknown;
    fetchCurrentUser: ReturnType<typeof vi.fn>;
    updateProfile: ReturnType<typeof vi.fn>;
  };
  let notify: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn>; info: ReturnType<typeof vi.fn> };

  const server = {
    id: 2, fullName: 'Aarav Sharma', email: 'student@learnsphere.test', role: UserRole.STUDENT,
    bio: 'Loves graphs.', departmentName: 'Computer Science', collegeName: 'TCET',
    collegeVerificationStatus: 'VERIFIED' as const, leaderboardPoints: 120, attendanceScore: 92, status: 'ACTIVE',
  };

  async function setUp(opts: { fetch?: unknown; update?: unknown } = {}) {
    auth = {
      currentUser: { ...server, bio: 'stale' },
      fetchCurrentUser: vi.fn(() => opts.fetch ?? of(server)),
      updateProfile: vi.fn((patch: Record<string, unknown>) => opts.update ?? of({ ...server, ...patch })),
    };
    notify = { success: vi.fn(), error: vi.fn(), info: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: NotificationService, useValue: notify },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProfileComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  const settle = async () => { fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges(); };
  const field = (id: string) => el.querySelector(`#${id}`) as HTMLInputElement | HTMLTextAreaElement;
  const type = (id: string, value: string) => {
    const f = field(id);
    f.value = value;
    f.dispatchEvent(new Event('input'));
  };
  const edit = async () => { (el.querySelector('#profile-edit-btn') as HTMLButtonElement).click(); await settle(); };
  const save = async () => { (el.querySelector('#profile-save-btn') as HTMLButtonElement).click(); await settle(); };

  it('clearing the bio saves an empty bio instead of silently keeping the old one', async () => {
    await setUp();
    await edit();
    type('profile-bio-input', '   ');
    await save();
    expect(auth.updateProfile).toHaveBeenCalledWith({ fullName: 'Aarav Sharma', bio: '' });
  });

  it('a blank name is a validation error and is not sent', async () => {
    await setUp();
    await edit();
    type('profile-name-input', '  ');
    await save();
    expect(auth.updateProfile).not.toHaveBeenCalled();
    expect(el.querySelector('.error-alert')?.textContent).toContain('Enter your name');
    expect(el.querySelector('#profile-save-btn')).toBeTruthy();
  });

  it('success is announced and the edit mode closes', async () => {
    await setUp();
    await edit();
    type('profile-name-input', 'Aarav S.');
    await save();
    expect(notify.success).toHaveBeenCalledWith('Profile saved.');
    expect(el.querySelector('#profile-edit-btn')).toBeTruthy();
    expect(el.querySelector('.identity-card h2')?.textContent).toContain('Aarav S.');
  });

  it('a failed save is reported as prominently as a success, with the server reason', async () => {
    await setUp({ update: throwError(() => ({ status: 400, error: { detail: 'Bio is too long.' } })) });
    await edit();
    await save();
    expect(notify.success).not.toHaveBeenCalled();
    expect(notify.error).toHaveBeenCalledWith('Bio is too long.');
    expect(el.querySelector('.error-alert')?.textContent).toContain('Bio is too long.');
    expect(el.querySelector('.saved-toast')).toBeNull();
    expect(el.querySelector('#profile-save-btn')).toBeTruthy();
  });

  it('says so when the fresh profile could not be loaded', async () => {
    await setUp({ fetch: throwError(() => ({ status: 503 })) });
    expect(el.querySelector('.error-alert')?.textContent).toContain('Could not load your latest profile');
  });
});
