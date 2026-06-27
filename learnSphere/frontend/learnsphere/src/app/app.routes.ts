import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { CertificatesComponent } from './features/student/certificates/certificates.component';
import { RoyaltiesComponent as TeacherRoyalties } from './features/teacher/royalties/royalties.component';
import { SentimentComponent as AdminSentiment } from './features/admin/sentiment/sentiment.component';
import { RegisterComponent } from './features/auth/register/register.component';
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

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'student/dashboard', component: StudentDashboard },
  { path: 'student/courses', component: StudentCourses },
  { path: 'student/lectures', component: StudentLectures },
  { path: 'student/messages', component: StudentMessages },
  { path: 'student/leaderboard', component: StudentLeaderboard },
  { path: 'student/discussion', component: StudentDiscussion },
  { path: 'student/certificates', component: CertificatesComponent },
  { path: 'teacher/royalties', component: TeacherRoyalties },
  { path: 'admin/sentiment', component: AdminSentiment },
  
  { path: 'teacher/dashboard', component: TeacherDashboard },
  { path: 'teacher/courses', component: TeacherCourses },
  { path: 'teacher/messages', component: TeacherMessages },
  { path: 'teacher/live', component: TeacherLive },
  { path: 'teacher/trending', component: TeacherTrending },
  { path: 'admin/dashboard', component: AdminDashboard },
  { path: 'admin/users', component: AdminUsers },
  { path: 'admin/messages', component: AdminMessages },
  { path: 'admin/payouts', component: AdminPayouts },
  { path: 'admin/flagged', component: AdminFlagged },
  { path: 'student/settings', component: StudentSettings },
  { path: 'student/announcements', component: StudentAnnouncements },
  { path: 'student/notifications', component: StudentNotifications },
  { path: 'student/profile', component: StudentProfile },
  { path: 'teacher/dashboard', component: TeacherDashboard },
  { path: 'teacher/courses', component: TeacherCourses },
  { path: 'teacher/messages', component: TeacherMessages },
  { path: 'teacher/settings', component: TeacherSettings },
  { path: 'teacher/announcements', component: TeacherAnnouncements },
  { path: 'teacher/notifications', component: TeacherNotifications },
  { path: 'teacher/profile', component: TeacherProfile },
  { path: 'admin/dashboard', component: AdminDashboard },
  { path: 'admin/users', component: AdminUsers },
  { path: 'admin/messages', component: AdminMessages },
  { path: 'admin/settings', component: AdminSettings },
  { path: 'admin/announcements', component: AdminAnnouncements },
  { path: 'admin/notifications', component: AdminNotifications },
  { path: 'admin/profile', component: AdminProfile },
  { path: 'admin/payouts', component: AdminPayouts }
  
];
