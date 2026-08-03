import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { MessagesComponent } from './messages/messages.component';


const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent },
  { path: 'messages', component: MessagesComponent },
  // Add more student routes here as you build them:
  // { path: 'courses',                    component: CoursesComponent },
  // { path: 'courses/:id',               component: CourseDetailComponent },
  // { path: 'courses/:id/lecture/:lid',  component: LectureComponent },
  // { path: 'notes',                     component: NotesComponent },
  // { path: 'assessments',               component: AssessmentsComponent },
  // { path: 'assessments/:id',           component: QuizComponent },
  // { path: 'bookmarks',                 component: BookmarksComponent },
  // { path: 'leaderboard',               component: LeaderboardComponent },
  // { path: 'discussion',                component: DiscussionComponent },
  // { path: 'certificates',              component: CertificatesComponent },
  // { path: 'settings',                  component: SettingsComponent },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class StudentRoutingModule {}