import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({ selector: 'app-teacher-profile', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './profile.component.html', styleUrls: ['./profile.component.scss'] })
export class ProfileComponent {
  constructor(private authService: AuthService) {}
  
  get name(): string { return this.authService.currentUser?.fullName || 'Teacher'; }
  get initials(): string { return this.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() || 'T'; }
  get email(): string { return this.authService.currentUser?.email || ''; }
  
  detail = 'Mathematics Department · MH College of Engineering';
  bio = 'Educator and applied mathematics researcher helping students turn difficult ideas into useful intuition.';
  readonly stats = [["6","Active courses"],["1,248","Students"],["4.8","Rating"],["₹18.4k","This month"]];
  editing = false;
  saved = false;
  save(): void { this.editing = false; this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

