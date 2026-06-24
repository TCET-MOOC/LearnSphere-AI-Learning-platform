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
import { LiveComponent as TeacherLive } from './features/teacher/live/live.component';
import { PayoutsComponent as AdminPayouts } from './features/admin/payouts/payouts.component';

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'student/dashboard', component: StudentDashboard },
  { path: 'student/courses', component: StudentCourses },
  { path: 'student/lectures', component: StudentLectures },
  { path: 'student/messages', component: StudentMessages },
  { path: 'student/leaderboard', component: StudentLeaderboard },
  { path: 'student/certificates', component: CertificatesComponent },
  { path: 'teacher/royalties', component: TeacherRoyalties },
  { path: 'admin/sentiment', component: AdminSentiment },
  
  { path: 'teacher/dashboard', component: TeacherDashboard },
  { path: 'teacher/courses', component: TeacherCourses },
  { path: 'teacher/messages', component: TeacherMessages },
  { path: 'teacher/live', component: TeacherLive },
  { path: 'admin/dashboard', component: AdminDashboard },
  { path: 'admin/users', component: AdminUsers },
  { path: 'admin/messages', component: AdminMessages },
  { path: 'admin/payouts', component: AdminPayouts }
  
];
