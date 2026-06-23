import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';

// 👇 Note the updated import path here!
import { LandingComponent } from './features/landing/landing.component'; 

import { DashboardComponent as StudentDashboard } from './features/student/dashboard/dashboard.component';
import { BookmarksComponent as StudentBookmarks } from './features/student/bookmarks/bookmarks.component';
import { DashboardComponent as TeacherDashboard } from './features/teacher/dashboard/dashboard.component';
import { DiscussionComponent as TeacherDiscussion } from './features/teacher/discussion/discussion.component';
import { DashboardComponent as AdminDashboard } from './features/admin/dashboard/dashboard.component';
import { ReportsComponent as AdminReports } from './features/admin/reports/reports.component';

export const routes: Routes = [
  // 1. Root path points to your new LandingComponent
  { path: '', component: LandingComponent, pathMatch: 'full' }, 
  
  // 2. Auth Routes
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  
  // 3. App Dashboards
  { path: 'student/dashboard', component: StudentDashboard },
  { path: 'student/bookmarks', component: StudentBookmarks },
  { path: 'teacher/dashboard', component: TeacherDashboard },
  { path: 'teacher/discuss', component: TeacherDiscussion },
  { path: 'teacher/discussion', redirectTo: 'teacher/discuss', pathMatch: 'full' },
  { path: 'admin/dashboard', component: AdminDashboard },
  { path: 'admin/reports', component: AdminReports }
];
