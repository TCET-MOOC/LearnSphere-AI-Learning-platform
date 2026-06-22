import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({ selector: 'app-student-profile', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './profile.component.html', styleUrls: ['./profile.component.scss'] })
export class ProfileComponent {
  name = 'Raj Shah';
  readonly initials = 'RS';
  readonly email = 'raj.shah@mhcollege.edu.in';
  detail = 'IT Department · Semester 5';
  bio = 'Curious builder, problem solver, and lifelong learner focused on software engineering.';
  readonly stats = [["7","Enrolled courses"],["84%","Attendance"],["#12","Leaderboard"],["3","Certificates"]];
  editing = false;
  saved = false;
  save(): void { this.editing = false; this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

