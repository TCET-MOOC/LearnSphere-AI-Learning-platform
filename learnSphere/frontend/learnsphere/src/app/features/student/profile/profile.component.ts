import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({ selector: 'app-student-profile', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './profile.component.html', styleUrls: ['./profile.component.scss'] })
export class ProfileComponent {
  constructor(private authService: AuthService) {}
  
  get name(): string { return this.authService.currentUser?.fullName || 'Student'; }
  get initials(): string { return this.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() || 'S'; }
  get email(): string { return this.authService.currentUser?.email || ''; }
  
  detail = 'IT Department · Semester 5';
  bio = 'Curious builder, problem solver, and lifelong learner focused on software engineering.';
  readonly stats = [["7","Enrolled courses"],["84%","Attendance"],["#12","Leaderboard"],["3","Certificates"]];
  editing = false;
  saved = false;
  save(): void { this.editing = false; this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

