import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { User } from '../../../core/models/user.model';
import { ReportsService } from '../services/reports.service';
import { AdminService } from '../services/admin.service';
import { NotificationService } from '@core/services/notification.service';
import { getInitials } from '@core/utils/avatar.util';
import { ADMIN_PROFILE_COPY as PROFILE_COPY } from '@core/content/admin-profile.content';

/**
 * Admin profile (SCR-61; W7-11, AUD-12 / audit.md D3). Reads the profile fresh from
 * `GET /users/me`, shows real platform figures only, and saves name/bio through `PUT /users/me`.
 */
@Component({
  selector: 'app-admin-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  protected readonly copy = PROFILE_COPY;

  displayName = '';
  email = '';
  readonly detail = PROFILE_COPY.role;
  bio = '';

  stats: [string, string][] = [
    ['—', PROFILE_COPY.stats.users],
    ['—', PROFILE_COPY.stats.courses],
    ['—', PROFILE_COPY.stats.colleges],
    ['—', PROFILE_COPY.stats.openFlags],
  ];

  editing = false;
  saving = false;
  saved = false;

  constructor(
    private authService: AuthService,
    private reportsService: ReportsService,
    private adminService: AdminService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const cached = this.authService.currentUser;
    if (cached) this.email = cached.email || '';
    this.authService.fetchCurrentUser().subscribe({
      next: (user) => this.apply(user),
      error: () => this.cdr.markForCheck(),
    });
    this.loadStats();
  }

  get initials(): string {
    return getInitials(this.displayName);
  }

  loadStats(): void {
    forkJoin({
      summary: this.reportsService.getSummary().pipe(catchError(() => of(null))),
      colleges: this.adminService.getColleges().pipe(catchError(() => of(null))),
    }).subscribe(({ summary, colleges }) => {
      const n = (v: number | null | undefined) => (v == null ? '—' : v.toLocaleString('en-IN'));
      this.stats = [
        [n(summary?.totalUsers), PROFILE_COPY.stats.users],
        [n(summary?.totalCourses), PROFILE_COPY.stats.courses],
        [n(colleges?.length), PROFILE_COPY.stats.colleges],
        [n(summary?.flaggedContentPending), PROFILE_COPY.stats.openFlags],
      ];
      this.cdr.markForCheck();
    });
  }

  toggleEdit(): void {
    if (!this.editing) {
      this.editing = true;
      return;
    }
    this.save();
  }

  save(): void {
    if (this.saving) return;
    this.saving = true;
    this.authService.updateProfile({ fullName: this.displayName.trim(), bio: this.bio.trim() }).subscribe({
      next: (user) => {
        this.apply(user);
        this.saving = false;
        this.editing = false;
        this.saved = true;
        this.notificationService.success(PROFILE_COPY.saved);
        window.setTimeout(() => { this.saved = false; this.cdr.markForCheck(); }, 2000);
        this.cdr.markForCheck();
      },
      error: () => {
        this.saving = false;
        this.notificationService.error(PROFILE_COPY.saveFailed);
        this.cdr.markForCheck();
      },
    });
  }

  /** There is no signed-in password change endpoint; send the real reset link instead. */
  changePassword(): void {
    if (!this.email) return;
    this.authService.forgotPassword({ email: this.email }).subscribe({
      next: (res) => this.notificationService.success(res?.message || PROFILE_COPY.resetSent(this.email)),
      error: () => this.notificationService.error(PROFILE_COPY.resetFailed),
    });
  }

  private apply(user: User): void {
    this.displayName = user.fullName || '';
    this.bio = user.bio || '';
    this.email = user.email || this.email;
    this.cdr.markForCheck();
  }
}
