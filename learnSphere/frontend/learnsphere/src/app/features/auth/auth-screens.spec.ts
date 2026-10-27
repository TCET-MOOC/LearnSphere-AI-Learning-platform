import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { VerifyCollegeComponent } from './verify-college/verify-college.component';
import { AuthService } from '@core/auth/auth.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { NotificationService } from '@core/services/notification.service';
import { CollegeService } from '@core/services/college.service';

describe('Auth Screens on Tokens (DS-05 / W1-16)', () => {
  const authServiceMock = {
    currentUser$: of(null),
    login: vi.fn().mockReturnValue(of({})),
    register: vi.fn().mockReturnValue(of({})),
    requestPasswordReset: vi.fn().mockReturnValue(of({})),
    resetPassword: vi.fn().mockReturnValue(of({})),
    requestAffiliation: vi.fn().mockReturnValue(of({}))
  };

  const errorHandlerMock = {
    handleError: vi.fn(),
    extractMessage: vi.fn().mockReturnValue('Error occurred')
  };

  const notificationServiceMock = {
    success: vi.fn(),
    error: vi.fn()
  };

  const collegeServiceMock = {
    getColleges: vi.fn().mockReturnValue(of([]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, RegisterComponent, ForgotPasswordComponent, VerifyCollegeComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: ErrorHandlerService, useValue: errorHandlerMock },
        { provide: NotificationService, useValue: notificationServiceMock },
        { provide: CollegeService, useValue: collegeServiceMock }
      ]
    }).compileComponents();
  });

  it('LoginComponent should render input surface and disabled submit button properly', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const emailInput = compiled.querySelector('#email') as HTMLInputElement;
    const passwordInput = compiled.querySelector('#password') as HTMLInputElement;
    const submitBtn = compiled.querySelector('button.btn-submit') as HTMLButtonElement;

    expect(emailInput).toBeTruthy();
    expect(passwordInput).toBeTruthy();
    expect(submitBtn).toBeTruthy();
    expect(submitBtn.disabled).toBe(true);
  });

  it('RegisterComponent should render input surface, role selector, and disabled submit button', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const inputs = compiled.querySelectorAll('input');
    expect(inputs.length).toBeGreaterThanOrEqual(3);

    const roleButtons = compiled.querySelectorAll('.role-btn');
    expect(roleButtons.length).toBe(3);

    const submitBtn = compiled.querySelector('button.btn-submit') as HTMLButtonElement;
    expect(submitBtn).toBeTruthy();
    expect(submitBtn.disabled).toBe(true);
  });

  it('ForgotPasswordComponent should render request form inputs and submit button', () => {
    const fixture = TestBed.createComponent(ForgotPasswordComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const emailInput = compiled.querySelector('input[type="email"]');
    expect(emailInput).toBeTruthy();

    const submitBtn = compiled.querySelector('button.btn-submit') as HTMLButtonElement;
    expect(submitBtn).toBeTruthy();
    expect(submitBtn.disabled).toBe(true);
  });

  it('VerifyCollegeComponent should render college selection and input fields', () => {
    const fixture = TestBed.createComponent(VerifyCollegeComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const select = compiled.querySelector('select');
    expect(select).toBeTruthy();

    const submitBtn = compiled.querySelector('button.btn-submit') as HTMLButtonElement;
    expect(submitBtn).toBeTruthy();
    expect(submitBtn.disabled).toBe(true);
  });
});
