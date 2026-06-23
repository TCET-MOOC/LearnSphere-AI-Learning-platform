import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentRoutingModule } from './student-routing.module';

// Note: DashboardComponent is standalone — it does NOT go in declarations.
// Standalone components declare their own imports internally.
// Just import StudentRoutingModule here and Angular's router will load them.

@NgModule({
  imports: [
    CommonModule,
    StudentRoutingModule
  ]
})
export class StudentModule {}