import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { LandingComponent } from './features/landing/landing.component';
import { DashboardComponent as StudentDashboard } from './features/student/dashboard/dashboard.component';
import { DashboardComponent as TeacherDashboard } from './features/teacher/dashboard/dashboard.component';
import { DashboardComponent as AdminDashboard } from './features/admin/dashboard/dashboard.component';
import { MessagesComponent as TeacherMessages } from './features/teacher/messages/messages.component';
import { MessagesComponent as StudentMessages } from './features/student/messages/messages.component';
import { MessagesComponent as AdminMessages } from './features/admin/messages/messages.component';

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'student/dashboard', component: StudentDashboard },
  { path: 'student/messages', component: StudentMessages },
  { path: 'teacher/dashboard', component: TeacherDashboard },
  { path: 'teacher/messages', component: TeacherMessages },
  { path: 'admin/dashboard', component: AdminDashboard },
  { path: 'admin/messages', component: AdminMessages }
];