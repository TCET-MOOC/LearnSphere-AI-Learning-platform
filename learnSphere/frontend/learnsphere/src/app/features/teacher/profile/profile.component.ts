import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { User } from '../../../core/models/user.model';
import { Course } from '../../../core/models/course.model';
import { NotificationService } from '../../../core/services/notification.service';
import { getInitials } from '../../../core/utils/avatar.util';
import { TEACHER_PROFILE_COPY as PROFILE_COPY } from '../../../core/content/teacher-profile.content';
import { TeacherService } from '../services/teacher.service';

/**
 * Teacher profile (SCR-41; W7-11, AUD-11 / audit.md D2). Reads the profile fresh from
 * `GET /users/me`, shows real activity, and saves name/bio through `PUT /users/me` (BR-24).
 */
@Component({ selector: 'app-teacher-profile', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './profile.component.html', styleUrls: ['./profile.component.scss'] })
export class ProfileComponent implements OnInit {
  protected readonly copy = PROFILE_COPY;

  /** The last profile the server confirmed. */
  private profile: User | null = null;
  name = '';
  bio = '';
  email = '';
  detail = '';
  stats: [string, string][] = [
    ['—', PROFILE_COPY.stats.liveCourses],
    ['—', PROFILE_COPY.stats.students],
    ['—', PROFILE_COPY.stats.lectures],
    ['—', PROFILE_COPY.stats.royaltyBalance],
  ];
  editing = false;
  saving = false;
  saved = false;

  constructor(
    private authService: AuthService,
    private teacherService: TeacherService,
    private notify: NotificationService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const cached = this.authService.currentUser;
    if (cached) this.email = cached.email || '';
    this.authService.fetchCurrentUser().subscribe({
      next: (user) => {
        this.apply(user);
        this.loadStats(user);
      },
      error: () => this.cdr.markForCheck(),
    });
  }

  get initials(): string {
    return getInitials(this.name) || 'T';
  }

  /** Edit opens the form; Save writes through the API and only then leaves edit mode. */
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
    this.authService.updateProfile({ fullName: this.name.trim(), bio: this.bio.trim() }).subscribe({
      next: (user) => {
        this.apply(user);
        this.saving = false;
        this.editing = false;
        this.saved = true;
        this.notify.success(PROFILE_COPY.saved);
        window.setTimeout(() => { this.saved = false; this.cdr.markForCheck(); }, 1600);
        this.cdr.markForCheck();
      },
      error: () => {
        this.saving = false;
        this.notify.error(PROFILE_COPY.saveFailed);
        this.cdr.markForCheck();
      },
    });
  }

  /** There is no signed-in password change endpoint; send the real reset link instead. */
  changePassword(): void {
    if (!this.email) return;
    this.authService.forgotPassword({ email: this.email }).subscribe({
      next: (res) => this.notify.success(res?.message || PROFILE_COPY.resetSent(this.email)),
      error: () => this.notify.error(PROFILE_COPY.resetFailed),
    });
  }

  private apply(user: User): void {
    this.profile = user;
    this.name = user.fullName || '';
    this.bio = user.bio || '';
    this.email = user.email || this.email;
    this.detail = [user.departmentName, user.collegeName].filter(Boolean).join(' · ') || PROFILE_COPY.fallbackRole;
    this.stats = [this.stats[0], this.stats[1], this.stats[2], [this.rupees(user.royaltyBalance), PROFILE_COPY.stats.royaltyBalance]];
    this.cdr.markForCheck();
  }

  private loadStats(user: User): void {
    forkJoin({
      courses: this.teacherService.getCourses().pipe(catchError(() => of(null as Course[] | null))),
      standings: this.teacherService.getStudentStandings('all').pipe(catchError(() => of(null))),
    }).subscribe(({ courses, standings }) => {
      const live = courses ? courses.filter((c) => c.status === 'LIVE').length.toLocaleString('en-IN') : '—';
      const lectures = courses ? courses.reduce((n, c) => n + (c.lectureCount || 0), 0).toLocaleString('en-IN') : '—';
      const students = standings ? new Set(standings.map((s) => s.studentId)).size.toLocaleString('en-IN') : '—';
      this.stats = [
        [live, PROFILE_COPY.stats.liveCourses],
        [students, PROFILE_COPY.stats.students],
        [lectures, PROFILE_COPY.stats.lectures],
        [this.rupees(user.royaltyBalance), PROFILE_COPY.stats.royaltyBalance],
      ];
      this.cdr.markForCheck();
    });
  }

  private rupees(value: number | null | undefined): string {
    return value == null ? '—' : `₹${Number(value).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
  }
}
