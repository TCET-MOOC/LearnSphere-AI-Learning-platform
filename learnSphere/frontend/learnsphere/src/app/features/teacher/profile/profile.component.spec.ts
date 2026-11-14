import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { TeacherService } from '../services/teacher.service';
import { UserRole } from '@core/models/user.model';

/**
 * W7-11 / AUD-11 (audit.md D2): the teacher profile shows the server's profile and real
 * activity, and Save writes through `PUT /users/me` (BR-24, AC-02).
 */
describe('Teacher ProfileComponent (W7-11)', () => {
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
    id: 2, fullName: 'Meera Iyer', email: 'teacher@learnsphere.test', role: UserRole.TEACHER,
    departmentName: 'Computer Science', collegeName: 'TCET Mumbai',
    bio: 'Teaches applied machine learning.', royaltyBalance: 18450, createdAt: '2026-01-01T00:00:00',
  };

  async function setUp(updateResult = of({ ...server, fullName: 'Meera R. Iyer', bio: 'Updated bio' })) {
    auth = {
      // Stale snapshot: the page must not trust it.
      currentUser: { ...server, fullName: 'Stale Name', bio: undefined },
      fetchCurrentUser: vi.fn(() => of(server)),
      updateProfile: vi.fn(() => updateResult),
      forgotPassword: vi.fn(() => of({ message: 'Reset link sent to teacher@learnsphere.test' })),
    };
    notify = { success: vi.fn(), error: vi.fn(), info: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: NotificationService, useValue: notify },
        {
          provide: TeacherService,
          useValue: {
            getCourses: () => of([
              { id: 1, status: 'LIVE', lectureCount: 12 },
              { id: 2, status: 'LIVE', lectureCount: 8 },
              { id: 3, status: 'DRAFT', lectureCount: 4 },
            ]),
            getStudentStandings: () => of([{ studentId: 10 }, { studentId: 11 }, { studentId: 10 }]),
          },
        },
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
    expect(el.querySelector('h2')?.textContent).toContain('Meera Iyer');
    expect(input('Display name').value).toBe('Meera Iyer');
    expect(input('About').value).toBe('Teaches applied machine learning.');
    expect(input('Role and institution').value).toBe('Computer Science · TCET Mumbai');
    expect(el.textContent).not.toContain('Mathematics Department');
  });

  it('shows real activity: live courses, distinct students, lectures and royalty balance', async () => {
    await setUp();
    const stats = Array.from(el.querySelectorAll('.stat')).map(
      (s) => `${s.querySelector('strong')?.textContent?.trim()} ${s.querySelector('span')?.textContent?.trim()}`,
    );
    expect(stats).toEqual(['2 Live courses', '2 Students', '24 Lectures', '₹18,450 Royalty balance']);
    expect(el.textContent).not.toContain('1,248');
    expect(el.textContent).not.toContain('4.8');
    expect(el.textContent).not.toContain('30 days ago'); // no password date exists on the server
  });

  it('saves name and bio through PUT /users/me and shows what the server returned', async () => {
    await setUp();
    button('Edit profile')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    const name = input('Display name');
    name.value = 'Meera R. Iyer';
    name.dispatchEvent(new Event('input'));
    const bio = input('About');
    bio.value = 'Updated bio';
    bio.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    button('Save profile')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(auth.updateProfile).toHaveBeenCalledWith({ fullName: 'Meera R. Iyer', bio: 'Updated bio' });
    expect(notify.success).toHaveBeenCalled();
    expect(el.querySelector('h2')?.textContent).toContain('Meera R. Iyer');
    expect(button('Edit profile')).toBeTruthy();
  });

  it('keeps the edits and stays in edit mode when the save fails; no success message', async () => {
    await setUp(throwError(() => ({ status: 400, error: { detail: 'Name is required' } })));
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

  it('does not let the teacher edit department or college (not part of PUT /users/me)', async () => {
    await setUp();
    button('Edit profile')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(input('Role and institution').disabled).toBe(true);
  });

  it('has no avatar upload control and sends a real password reset link', async () => {
    await setUp();
    button('Edit profile')!.click();
    fixture.detectChanges();
    expect(el.querySelector('.profile-avatar button')).toBeNull();

    button('Change password')!.click();
    expect(auth.forgotPassword).toHaveBeenCalledWith({ email: 'teacher@learnsphere.test' });
    expect(notify.success).toHaveBeenCalledWith('Reset link sent to teacher@learnsphere.test');
  });
});
