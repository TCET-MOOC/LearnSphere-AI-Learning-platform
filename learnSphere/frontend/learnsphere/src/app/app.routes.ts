import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { authGuard } from './core/auth/auth.guard';
import { UserRole } from './core/models/user.model';

/**
 * W7-02: only the landing page is in the initial bundle. Public screens load on demand;
 * the student, teacher and admin areas are lazy route groups (`features/<role>/<role>.routes.ts`)
 * that keep their guards per screen. `data.preloadFor` lets RolePreloadingStrategy fetch the
 * signed-in user's own area after login, and never another role's.
 */
export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent) },
  { path: 'forgot-password', loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent) },
  { path: 'verify-college', loadComponent: () => import('./features/auth/verify-college/verify-college.component').then((m) => m.VerifyCollegeComponent), canActivate: [authGuard] },
  { path: 'verify-certificate', loadComponent: () => import('./features/public/verify-certificate/verify-certificate.component').then((m) => m.VerifyCertificateComponent) },
  { path: 'verify-certificate/:code', loadComponent: () => import('./features/public/verify-certificate/verify-certificate.component').then((m) => m.VerifyCertificateComponent) },
  // Older alias of the certificate link; one canonical screen route (W7-06).
  { path: 'verify/certificate/:code', redirectTo: '/verify-certificate/:code' },
  { path: 'payment/checkout/:courseId', loadComponent: () => import('./features/payment/checkout.component').then((m) => m.CheckoutComponent), canActivate: [authGuard] },
  { path: 'payment/success', loadComponent: () => import('./features/payment/payment-success.component').then((m) => m.PaymentSuccessComponent), canActivate: [authGuard] },
  {
    path: 'student',
    loadChildren: () => import('./features/student/student.routes').then((m) => m.STUDENT_ROUTES),
    data: { preloadFor: UserRole.STUDENT },
  },
  {
    path: 'teacher',
    loadChildren: () => import('./features/teacher/teacher.routes').then((m) => m.TEACHER_ROUTES),
    data: { preloadFor: UserRole.TEACHER },
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
    data: { preloadFor: UserRole.ADMIN },
  },

  { path: '**', loadComponent: () => import('./features/not-found/not-found.component').then((m) => m.NotFoundComponent) },
];
