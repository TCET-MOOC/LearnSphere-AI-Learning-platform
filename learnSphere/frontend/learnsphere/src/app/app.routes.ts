import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { CertificatesComponent } from './features/student/certificates/certificates.component';
import { RoyaltiesComponent as TeacherRoyalties } from './features/teacher/royalties/royalties.component';
import { SentimentComponent as AdminSentiment } from './features/admin/sentiment/sentiment.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { VerifyCollegeComponent } from './features/auth/verify-college/verify-college.component';
import { LandingComponent } from './features/landing/landing.component';
import { DashboardComponent as StudentDashboard } from './features/student/dashboard/dashboard.component';
import { DashboardComponent as TeacherDashboard } from './features/teacher/dashboard/dashboard.component';
import { DashboardComponent as AdminDashboard } from './features/admin/dashboard/dashboard.component';
import { MessagesComponent as TeacherMessages } from './features/teacher/messages/messages.component';
import { MessagesComponent as StudentMessages } from './features/student/messages/messages.component';
import { MessagesComponent as AdminMessages } from './features/admin/messages/messages.component';
import { CoursesComponent as StudentCourses } from './features/student/courses/courses.component';
import { LectureComponent as StudentLectures } from './features/student/courses/lecture/lecture.component';
import { CoursesComponent as TeacherCourses } from './features/teacher/courses/courses.component';
import { UsersComponent as AdminUsers } from './features/admin/users/users.component';
import { LeaderboardComponent as StudentLeaderboard } from './features/student/leaderboard/leaderboard.component';
import { DiscussionComponent as StudentDiscussion } from './features/student/discussion/discussion.component';
import { LiveComponent as TeacherLive } from './features/teacher/live/live.component';
import { TrendingComponent as TeacherTrending } from './features/teacher/trending/trending.component';
import { PayoutsComponent as AdminPayouts } from './features/admin/payouts/payouts.component';
import { FlaggedComponent as AdminFlagged } from './features/admin/flagged/flagged.component';
import { SettingsComponent as StudentSettings } from './features/student/settings/settings.component';
import { AnnouncementsComponent as StudentAnnouncements } from './features/student/announcements/announcements.component';
import { NotificationsComponent as StudentNotifications } from './features/student/notifications/notifications.component';
import { ProfileComponent as StudentProfile } from './features/student/profile/profile.component';
import { SettingsComponent as TeacherSettings } from './features/teacher/settings/settings.component';
import { AnnouncementsComponent as TeacherAnnouncements } from './features/teacher/announcements/announcements.component';
import { NotificationsComponent as TeacherNotifications } from './features/teacher/notifications/notifications.component';
import { ProfileComponent as TeacherProfile } from './features/teacher/profile/profile.component';
import { SettingsComponent as AdminSettings } from './features/admin/settings/settings.component';
import { AnnouncementsComponent as AdminAnnouncements } from './features/admin/announcements/announcements.component';
import { NotificationsComponent as AdminNotifications } from './features/admin/notifications/notifications.component';
import { ProfileComponent as AdminProfile } from './features/admin/profile/profile.component';
import { CollegesComponent as AdminColleges } from './features/admin/colleges/colleges.component';
import { authGuard } from './core/auth/auth.guard';
// Note: admin/courses, admin/revenue, admin/reports components are still empty scaffolding
// (no exported component class yet) — their routes will be added once those are implemented.
import { roleGuard } from './core/auth/role.guard';
import { UserRole } from './core/models/user.model';

const STUDENT = [UserRole.STUDENT];
const TEACHER = [UserRole.TEACHER];
const ADMIN = [UserRole.ADMIN];

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'verify-college', component: VerifyCollegeComponent, canActivate: [authGuard] },

  // Student portal
  { path: 'student/dashboard', component: StudentDashboard, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/courses', component: StudentCourses, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/lectures', component: StudentLectures, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/messages', component: StudentMessages, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/leaderboard', component: StudentLeaderboard, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/discussion', component: StudentDiscussion, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/certificates', component: CertificatesComponent, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/settings', component: StudentSettings, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/announcements', component: StudentAnnouncements, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/notifications', component: StudentNotifications, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },
  { path: 'student/profile', component: StudentProfile, canActivate: [authGuard, roleGuard], data: { roles: STUDENT } },

  // Teacher portal
  { path: 'teacher/dashboard', component: TeacherDashboard, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/courses', component: TeacherCourses, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/messages', component: TeacherMessages, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/live', component: TeacherLive, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/trending', component: TeacherTrending, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/royalties', component: TeacherRoyalties, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/settings', component: TeacherSettings, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/announcements', component: TeacherAnnouncements, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/notifications', component: TeacherNotifications, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },
  { path: 'teacher/profile', component: TeacherProfile, canActivate: [authGuard, roleGuard], data: { roles: TEACHER } },

  // Admin portal
  { path: 'admin/dashboard', component: AdminDashboard, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/users', component: AdminUsers, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/messages', component: AdminMessages, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/payouts', component: AdminPayouts, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/flagged', component: AdminFlagged, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/sentiment', component: AdminSentiment, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/settings', component: AdminSettings, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/announcements', component: AdminAnnouncements, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/notifications', component: AdminNotifications, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/profile', component: AdminProfile, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
  { path: 'admin/colleges', component: AdminColleges, canActivate: [authGuard, roleGuard], data: { roles: ADMIN } },
];
