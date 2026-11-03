import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';
import { roleGuard } from '@core/auth/role.guard';
import { UserRole } from '@core/models/user.model';

/**
 * Admin area, lazy-loaded under `/admin` (W7-02). Every screen is its own chunk
 * and keeps its own guards; redirects are absolute so they read the same as the old table.
 */
const ADMIN = [UserRole.ADMIN];

export const ADMIN_ROUTES: Routes = [
  { path: '', redirectTo: '/admin/dashboard', pathMatch: 'full' },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'users', loadComponent: () => import('./users/users.component').then((m) => m.UsersComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'messages', loadComponent: () => import('./messages/messages.component').then((m) => m.MessagesComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'payouts', loadComponent: () => import('./payouts/payouts.component').then((m) => m.PayoutsComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'flagged', loadComponent: () => import('./flagged/flagged.component').then((m) => m.FlaggedComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'sentiment', loadComponent: () => import('./sentiment/sentiment.component').then((m) => m.SentimentComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'settings', loadComponent: () => import('./settings/settings.component').then((m) => m.SettingsComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'announcements', loadComponent: () => import('./announcements/announcements.component').then((m) => m.AnnouncementsComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'notifications', loadComponent: () => import('./notifications/notifications.component').then((m) => m.NotificationsComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'profile', loadComponent: () => import('./profile/profile.component').then((m) => m.ProfileComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'colleges', loadComponent: () => import('./colleges/colleges.component').then((m) => m.CollegesComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'reports', loadComponent: () => import('./reports/reports.component').then((m) => m.ReportsComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'revenue', loadComponent: () => import('./revenue/revenue.component').then((m) => m.RevenueComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'courses', loadComponent: () => import('./courses/courses.component').then((m) => m.CoursesComponent), canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
];
