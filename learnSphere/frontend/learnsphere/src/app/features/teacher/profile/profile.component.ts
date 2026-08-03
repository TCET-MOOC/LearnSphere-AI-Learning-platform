import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({ selector: 'app-teacher-profile', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './profile.component.html', styleUrls: ['./profile.component.scss'] })
export class ProfileComponent {
  name = 'Prof. Ajay Sharma';
  readonly initials = 'AS';
  readonly email = 'a.sharma@mhcollege.edu.in';
  detail = 'Mathematics Department · MH College of Engineering';
  bio = 'Educator and applied mathematics researcher helping students turn difficult ideas into useful intuition.';
  readonly stats = [["6","Active courses"],["1,248","Students"],["4.8","Rating"],["₹18.4k","This month"]];
  editing = false;
  saved = false;
  save(): void { this.editing = false; this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

