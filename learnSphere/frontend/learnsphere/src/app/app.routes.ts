import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';

// 👇 Note the updated import path here!
import { LandingComponent } from './features/landing/landing.component'; 

import { DashboardComponent as StudentDashboard } from './features/student/dashboard/dashboard.component';
import { DashboardComponent as TeacherDashboard } from './features/teacher/dashboard/dashboard.component';
import { DashboardComponent as AdminDashboard } from './features/admin/dashboard/dashboard.component';

import { NotesComponent } from './features/student/notes/notes.component';
import { StudentsComponent } from './features/teacher/students/students.component';
import { CollegesComponent } from './features/admin/colleges/colleges.component';

export const routes: Routes = [
  // 1. Root path points to your new LandingComponent
  { path: '', component: LandingComponent, pathMatch: 'full' }, 
  
  // 2. Auth Routes
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  
  // 3. App Dashboards
  { path: 'student/dashboard', component: StudentDashboard },
  { path: 'teacher/dashboard', component: TeacherDashboard },
  { path: 'admin/dashboard', component: AdminDashboard },

  // 4. Feature Routes
  { path: 'student/notes', component: NotesComponent },
  { path: 'teacher/students', component: StudentsComponent },
  { path: 'admin/colleges', component: CollegesComponent }
];