import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { SESSION_COPY } from '@core/content/session.content';
import { UserRole } from '@core/models/user.model';

describe('LoginComponent return path (W7-04)', () => {
  let navigateByUrl: ReturnType<typeof vi.fn>;

  function setup(query: Record<string, string>) {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(query) } } },
        {
          provide: AuthService,
          useValue: {
            login: () => of({ token: 't', user: { id: 1, fullName: 'Aarav Shah', email: 'a@example.com', role: UserRole.STUDENT, createdAt: '' } }),
            dashboardPathForRole: () => '/student/dashboard',
          },
        },
        { provide: NotificationService, useValue: { success: vi.fn(), info: vi.fn(), error: vi.fn() } },
      ],
    });
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true) as unknown as ReturnType<typeof vi.fn>;
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture;
  }

  function signIn(fixture: ReturnType<typeof setup>) {
    fixture.componentInstance.loginForm.setValue({ email: 'a@example.com', password: 'Str0ng!Passw0rd' });
    fixture.componentInstance.onSubmit();
  }

  it('returns to the page the user was sent away from', () => {
    signIn(setup({ redirectTo: '/student/courses/4?tab=notes' }));
    expect(navigateByUrl).toHaveBeenCalledWith('/student/courses/4?tab=notes');
  });

  it('ignores a return path that would leave the app', () => {
    signIn(setup({ redirectTo: '//evil.example/phish' }));
    expect(navigateByUrl).toHaveBeenCalledWith('/student/dashboard');
  });

  it('falls back to the role dashboard without a return path', () => {
    signIn(setup({}));
    expect(navigateByUrl).toHaveBeenCalledWith('/student/dashboard');
  });

  it('explains why the user is back on the login page after their session expired', () => {
    const fixture = setup({ reason: 'expired', redirectTo: '/student/notes' });
    const notice = (fixture.nativeElement as HTMLElement).querySelector('[role="status"].session-notice');
    expect(notice?.textContent?.trim()).toBe(SESSION_COPY.expiredNotice);
  });

  it('shows no notice on a normal visit', () => {
    const fixture = setup({});
    expect((fixture.nativeElement as HTMLElement).querySelector('.session-notice')).toBeNull();
  });
});
