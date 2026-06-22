import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({ selector: 'app-admin-profile', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './profile.component.html', styleUrls: ['./profile.component.scss'] })
export class ProfileComponent {
  name = 'Admin User';
  readonly initials = 'AD';
  readonly email = 'admin@learnsphere.in';
  detail = 'Super Admin · LearnSphere Platform';
  bio = 'Platform administrator responsible for trust, quality, institutional onboarding, and healthy learning operations.';
  readonly stats = [["18.2k","Users"],["842","Courses"],["42","Colleges"],["99.8%","Uptime"]];
  editing = false;
  saved = false;
  save(): void { this.editing = false; this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

