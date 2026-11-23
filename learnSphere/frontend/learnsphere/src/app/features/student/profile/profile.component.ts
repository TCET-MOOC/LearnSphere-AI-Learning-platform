import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { User } from '../../../core/models/user.model';
import { NotificationService } from '@core/services/notification.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { STUDENT_PROFILE_COPY } from '@core/content/student-profile.content';

/**
 * Student profile (SCR-26). Reads `GET /users/me`, saves name and bio through `PUT /users/me`.
 * W7-12 (audit.md D1, AUD-10): a cleared bio is saved as empty, a blank name is a validation
 * error, failures are announced like successes with the server's reason, and a failed load is
 * shown instead of hidden. Copy lives in `core/content/student-profile.content.ts`.
 */
@Component({
  selector: 'app-student-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  protected readonly copy = STUDENT_PROFILE_COPY;
  user: User | null = null;
  loading = true;
  saving = false;
  saved = false;
  error: string | null = null;
  editing = false;

  // Editable form fields (bound during edit mode)
  editName = '';
  editBio = '';

  constructor(
    private authService: AuthService,
    private notify: NotificationService,
    private errors: ErrorHandlerService,
  ) {}

  ngOnInit(): void {
    // Seed from the cached snapshot for instant display, then fetch the server copy.
    this.user = this.authService.currentUser;
    this._syncEditFields();

    this.authService.fetchCurrentUser().subscribe({
      next: (u) => {
        this.user = u;
        this._syncEditFields();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = this.copy.loadFailed;
      }
    });
  }

  get name(): string { return this.user?.fullName || this.copy.fallbackName; }
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
    return parts.join(' · ') || this.copy.fallbackDetail;
  }

  get stats(): [string, string][] {
    const s = this.copy.stats;
    return [
      [String(this.user?.leaderboardPoints ?? '—'), s.points],
      [this.user?.attendanceScore != null ? `${this.user.attendanceScore}%` : '—', s.attendance],
      [this.user?.collegeVerificationStatus === 'VERIFIED' ? '✓' : '—', s.verified],
      [this.user?.status ?? '—', s.status],
    ];
  }

  startEdit(): void {
    this._syncEditFields();
    this.error = null;
    this.editing = true;
  }

  cancelEdit(): void {
    this._syncEditFields();
    this.editing = false;
    this.error = null;
  }

  save(): void {
    if (this.saving) return;
    const fullName = this.editName.trim();
    if (!fullName) {
      this.error = this.copy.nameRequired;
      return;
    }
    this.saving = true;
    this.error = null;

    // The bio is always sent: an empty string clears it (the server ignores only `null`).
    this.authService.updateProfile({ fullName, bio: this.editBio.trim() }).subscribe({
      next: (u) => {
        this.user = u;
        this._syncEditFields();
        this.editing = false;
        this.saving = false;
        this.saved = true;
        this.notify.success(this.copy.saved);
        window.setTimeout(() => (this.saved = false), 1600);
      },
      error: (err) => {
        this.saving = false;
        this.error = this.errors.extractMessage(err, this.copy.saveFailed);
        this.notify.error(this.error);
      }
    });
  }

  private _syncEditFields(): void {
    this.editName = this.user?.fullName ?? '';
    this.editBio = this.user?.bio ?? '';
  }
}
