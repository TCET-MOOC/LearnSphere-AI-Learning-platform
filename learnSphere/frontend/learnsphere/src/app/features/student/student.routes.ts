import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';
import { roleGuard } from '@core/auth/role.guard';
import { UserRole } from '@core/models/user.model';

/**
 * Student area, lazy-loaded under `/student` (W7-02). Every screen is its own chunk
 * and keeps its own guards; redirects are absolute so they read the same as the old table.
 */
const STUDENT = [UserRole.STUDENT];

export const STUDENT_ROUTES: Routes = [
  { path: '', redirectTo: '/student/dashboard', pathMatch: 'full' },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'courses', loadComponent: () => import('./courses/courses.component').then((m) => m.CoursesComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'explore', loadComponent: () => import('./explore/explore.component').then((m) => m.ExploreComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'courses/:id', loadComponent: () => import('./courses/course-details/course-detail.component').then((m) => m.CourseDetailComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'courses/:courseId/player', loadComponent: () => import('./courses/lecture/player-resolver.component').then((m) => m.PlayerResolverComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'courses/:courseId/lectures/:lectureId', loadComponent: () => import('./courses/lecture/lecture.component').then((m) => m.LectureComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  // Legacy lecture link: open that lecture, not the course's resume point (W7-06).
  { path: 'courses/:courseId/lecture/:lectureId', redirectTo: '/student/courses/:courseId/lectures/:lectureId' },
  { path: 'lectures', redirectTo: '/student/explore', pathMatch: 'full' },
  { path: 'notes', loadComponent: () => import('./notes/notes.component').then((m) => m.NotesComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'bookmarks', loadComponent: () => import('./bookmarks/bookmarks.component').then((m) => m.BookmarksComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'messages', loadComponent: () => import('./messages/messages.component').then((m) => m.MessagesComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'leaderboard', loadComponent: () => import('./leaderboard/leaderboard.component').then((m) => m.LeaderboardComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'discussion', loadComponent: () => import('./discussion/discussion.component').then((m) => m.DiscussionComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'certificates', loadComponent: () => import('./certificates/certificates.component').then((m) => m.CertificatesComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'purchases', loadComponent: () => import('./purchases/purchases.component').then((m) => m.PurchasesComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'live', loadComponent: () => import('./live/live.component').then((m) => m.StudentLiveComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'live/:id', loadComponent: () => import('./live/live-room.component').then((m) => m.StudentLiveRoomComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'assessments', loadComponent: () => import('./assessments/assessments.component').then((m) => m.AssessmentsComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'assessments/:id', loadComponent: () => import('./assessments/quiz/quiz.component').then((m) => m.QuizComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'settings', loadComponent: () => import('./settings/settings.component').then((m) => m.SettingsComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'announcements', loadComponent: () => import('./announcements/announcements.component').then((m) => m.AnnouncementsComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'notifications', loadComponent: () => import('./notifications/notifications.component').then((m) => m.NotificationsComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'profile', loadComponent: () => import('./profile/profile.component').then((m) => m.ProfileComponent), canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
];
