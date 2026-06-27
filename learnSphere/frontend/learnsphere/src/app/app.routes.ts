import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';

// 👇 Note the updated import path here!
import { LandingComponent } from './features/landing/landing.component'; 

import { DashboardComponent as StudentDashboard } from './features/student/dashboard/dashboard.component';
import { DashboardComponent as TeacherDashboard } from './features/teacher/dashboard/dashboard.component';
import { DashboardComponent as AdminDashboard } from './features/admin/dashboard/dashboard.component';
<<<<<<< Updated upstream

=======
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
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream

  // 4. Feature Routes
  { path: 'student/notes', component: NotesComponent },
  { path: 'teacher/students', component: StudentsComponent },
  { path: 'admin/colleges', component: CollegesComponent }
];
=======
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
  { path: 'admin/payouts', component: AdminPayouts },
  
  // Feature Routes
  { path: 'student/notes', component: NotesComponent },
  { path: 'teacher/students', component: StudentsComponent },
  { path: 'admin/colleges', component: CollegesComponent }
];
>>>>>>> Stashed changes
