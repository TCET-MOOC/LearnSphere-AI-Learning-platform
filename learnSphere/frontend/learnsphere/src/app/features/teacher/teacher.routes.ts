import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';
import { roleGuard } from '@core/auth/role.guard';
import { UserRole } from '@core/models/user.model';

/**
 * Teacher area, lazy-loaded under `/teacher` (W7-02). Every screen is its own chunk
 * and keeps its own guards; redirects are absolute so they read the same as the old table.
 */
const TEACHER = [UserRole.TEACHER];

export const TEACHER_ROUTES: Routes = [
  { path: '', redirectTo: '/teacher/dashboard', pathMatch: 'full' },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'courses', loadComponent: () => import('./courses/courses.component').then((m) => m.CoursesComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'students', loadComponent: () => import('./students/students.component').then((m) => m.StudentsComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'courses/new', loadComponent: () => import('./courses/course-management/course-management.component').then((m) => m.CourseManagementComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'courses/:id/manage', loadComponent: () => import('./courses/course-management/course-management.component').then((m) => m.CourseManagementComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'upload', loadComponent: () => import('./upload/upload.component').then((m) => m.UploadComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'discussion', loadComponent: () => import('./discussion/discussion.component').then((m) => m.DiscussionComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'messages', loadComponent: () => import('./messages/messages.component').then((m) => m.MessagesComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'live', loadComponent: () => import('./live/live.component').then((m) => m.LiveComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'trending', loadComponent: () => import('./trending/trending.component').then((m) => m.TrendingComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'royalties', loadComponent: () => import('./royalties/royalties.component').then((m) => m.RoyaltiesComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'settings', loadComponent: () => import('./settings/settings.component').then((m) => m.SettingsComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'announcements', loadComponent: () => import('./announcements/announcements.component').then((m) => m.AnnouncementsComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'notifications', loadComponent: () => import('./notifications/notifications.component').then((m) => m.NotificationsComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'profile', loadComponent: () => import('./profile/profile.component').then((m) => m.ProfileComponent), canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
];
