import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-student-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  loading = true;
  saving = false;
  saved = false;
  error: string | null = null;
  editing = false;

  // Editable form fields (bound during edit mode)
  editName = '';
  editBio = '';

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // Seed form from the cached snapshot first for instant display,
    // then fetch the authoritative server copy.
    this.user = this.authService.currentUser;
    this._syncEditFields();

    this.authService.fetchCurrentUser().subscribe({
      next: (u) => {
        this.user = u;
        this._syncEditFields();
        this.loading = false;
      },
      error: () => {
        // If the server fetch fails, fall back gracefully to the cached user.
        this.loading = false;
      }
    });
  }

  get name(): string { return this.user?.fullName || 'Student'; }
  get initials(): string {
    return (this.user?.fullName || 'S')
      .split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  }
  get email(): string { return this.user?.email || ''; }
  get bio(): string { return this.user?.bio || ''; }
  get detail(): string {
    const parts: string[] = [];
    if (this.user?.departmentName) parts.push(this.user.departmentName);
    if (this.user?.collegeName) parts.push(this.user.collegeName);
    return parts.join(' · ') || 'LearnSphere Student';
  }

  get stats(): [string, string][] {
    return [
      [String(this.user?.leaderboardPoints ?? '—'), 'Leaderboard points'],
      [this.user?.attendanceScore != null ? `${this.user.attendanceScore}%` : '—', 'Attendance score'],
      [this.user?.collegeVerificationStatus === 'VERIFIED' ? '✓' : '—', 'College verified'],
      [this.user?.status ?? '—', 'Account status'],
    ];
  }

  startEdit(): void {
    this._syncEditFields();
    this.editing = true;
  }

  cancelEdit(): void {
    this._syncEditFields();
    this.editing = false;
    this.error = null;
  }

  save(): void {
    if (this.saving) return;
    this.saving = true;
    this.error = null;

    this.authService.updateProfile({
      fullName: this.editName.trim() || undefined,
      bio: this.editBio.trim() || undefined,
    }).subscribe({
      next: (u) => {
        this.user = u;
        this._syncEditFields();
        this.editing = false;
        this.saving = false;
        this.saved = true;
        window.setTimeout(() => (this.saved = false), 1600);
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message ?? 'Failed to save profile. Please try again.';
      }
    });
  }

  private _syncEditFields(): void {
    this.editName = this.user?.fullName ?? '';
    this.editBio = this.user?.bio ?? '';
  }
}

