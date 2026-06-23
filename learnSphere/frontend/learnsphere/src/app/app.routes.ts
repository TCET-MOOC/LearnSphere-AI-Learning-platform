import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { LandingComponent } from './features/landing/landing.component';
import { DashboardComponent as StudentDashboard } from './features/student/dashboard/dashboard.component';
import { DashboardComponent as TeacherDashboard } from './features/teacher/dashboard/dashboard.component';
import { DashboardComponent as AdminDashboard } from './features/admin/dashboard/dashboard.component';
import { BookmarksComponent as StudentBookmarks } from './features/student/bookmarks/bookmarks.component';
import { DiscussionComponent as TeacherDiscussion } from './features/teacher/discussion/discussion.component';
import { ReportsComponent as AdminReports } from './features/admin/reports/reports.component';
import { MessagesComponent as TeacherMessages } from './features/teacher/messages/messages.component';
import { MessagesComponent as StudentMessages } from './features/student/messages/messages.component';
import { MessagesComponent as AdminMessages } from './features/admin/messages/messages.component';
import { CoursesComponent as StudentCourses } from './features/student/courses/courses.component';
import { LectureComponent as StudentLectures } from './features/student/courses/lecture/lecture.component';
import { CoursesComponent as TeacherCourses } from './features/teacher/courses/courses.component';
import { UsersComponent as AdminUsers } from './features/admin/users/users.component';

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'student/dashboard', component: StudentDashboard },
  { path: 'student/courses', component: StudentCourses },
  { path: 'student/lectures', component: StudentLectures },
  { path: 'student/bookmarks', component: StudentBookmarks },
  { path: 'student/messages', component: StudentMessages },
  { path: 'teacher/dashboard', component: TeacherDashboard },
  { path: 'teacher/courses', component: TeacherCourses },
  { path: 'teacher/messages', component: TeacherMessages },
  { path: 'teacher/discuss', component: TeacherDiscussion },
  { path: 'teacher/discussion', redirectTo: 'teacher/discuss', pathMatch: 'full' },
  { path: 'admin/dashboard', component: AdminDashboard },
  { path: 'admin/users', component: AdminUsers },
  { path: 'admin/messages', component: AdminMessages },
  { path: 'admin/reports', component: AdminReports }
];
