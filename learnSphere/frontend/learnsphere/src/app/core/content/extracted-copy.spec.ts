/**
 * W7-07: screens whose copy moved to core/content still render it — including when every lazy
 * route has been loaded first (as `app.routes.spec.ts` does). In the unit-test bundle a content
 * module shared by several lazy screens came back `undefined` inside the components, so each
 * screen has its own content file. This spec is the regression guard for that.
 */
import { Type, importProvidersFrom } from '@angular/core';
import { LucideAngularModule, ShieldAlert, Award, Search, CheckCircle2, Download } from 'lucide-angular';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Routes } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { routes } from '../../app.routes';
import { AuthService } from '@core/auth/auth.service';
import { CollegeService } from '@core/services/college.service';
import { LoginComponent } from '../../features/auth/login/login.component';
import { RegisterComponent } from '../../features/auth/register/register.component';
import { ForgotPasswordComponent } from '../../features/auth/forgot-password/forgot-password.component';
import { VerifyCollegeComponent } from '../../features/auth/verify-college/verify-college.component';
import { VerifyCertificateComponent } from '../../features/public/verify-certificate/verify-certificate.component';
import { CheckoutComponent } from '../../features/payment/checkout.component';
import { PaymentSuccessComponent } from '../../features/payment/payment-success.component';

async function loadEveryRoute(list: Routes): Promise<void> {
  for (const r of list) {
    if (r.loadComponent) await r.loadComponent();
    if (r.loadChildren) await loadEveryRoute((await r.loadChildren()) as Routes);
  }
}

const SCREENS: [string, Type<unknown>, string][] = [
  ['login', LoginComponent, 'Welcome back'],
  ['register', RegisterComponent, 'Create an Account'],
  ['forgot-password', ForgotPasswordComponent, 'Reset your password'],
  ['verify-college', VerifyCollegeComponent, 'Link your college'],
  ['verify-certificate', VerifyCertificateComponent, 'Verify Official Academic Certificate'],
  ['checkout', CheckoutComponent, 'Redirecting to Course Checkout'],
  ['payment-success', PaymentSuccessComponent, 'Payment successful'],
];

describe('Extracted copy renders (W7-07)', () => {
  beforeAll(async () => {
    await loadEveryRoute(routes);
  }, 30_000);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        importProvidersFrom(LucideAngularModule.pick({ ShieldAlert, Award, Search, CheckCircle2, Download })),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser$: of(null), currentUser: null, isAuthenticated: false } },
        { provide: CollegeService, useValue: { getColleges: vi.fn().mockReturnValue(of([])) } },
      ],
    });
  });

  for (const [name, component, text] of SCREENS) {
    it(`${name} renders its copy`, () => {
      const fixture = TestBed.createComponent(component);
      fixture.detectChanges();
      expect((fixture.nativeElement as HTMLElement).textContent).toContain(text);
    });
  }
});
