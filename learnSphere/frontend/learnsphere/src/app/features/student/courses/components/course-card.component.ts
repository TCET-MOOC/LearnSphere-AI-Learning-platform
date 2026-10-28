import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Course, ResumeTarget } from '@core/models/course.model';
import { StudentService } from '../../services/student.service';

/**
 * CourseCardComponent renders a single enrolled course row for the student
 * "My Courses" list, including progress, Continue/resume target navigation,
 * and a link into the course detail page.
 */
@Component({
  selector: 'app-course-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <article class="course-row" *ngIf="course" [routerLink]="['/student/courses', course.id]">
      <div class="course-thumb">{{ initials }}</div>
      <div class="course-main">
        <div class="course-title-row">
          <h3>{{ course.title }}</h3>
          <div class="badge-group">
            <span class="pill pill--completed" *ngIf="isCompleted">Completed</span>
            <span class="pill pill--progress" *ngIf="!isCompleted">In Progress</span>
          </div>
        </div>
        <p>{{ course.teacherName || 'Instructor' }} · {{ course.department }} · {{ course.lectureCount || 0 }} lectures</p>
        <div class="progress-track" aria-hidden="true">
          <span [style.width.%]="displayProgress" [class.is-completed-fill]="isCompleted"></span>
        </div>
      </div>
      <div class="course-meta-end">
        <div class="course-progress" [class.is-completed-text]="isCompleted">
          <span *ngIf="isCompleted">🏆 </span>{{ displayProgress }}%
        </div>
        <button
          class="btn-continue"
          type="button"
          [disabled]="resuming"
          (click)="onContinue($event)">
          {{ resuming ? '…' : (isCompleted ? 'Review' : 'Continue') }}
        </button>
      </div>
    </article>
  `,
  styles: [`
    .course-row {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px 12px;
      border-bottom: 1px solid var(--border-color);
      cursor: pointer;
      text-decoration: none;
      color: inherit;
      transition: background 0.15s ease, border-color 0.15s ease;
      border-radius: var(--border-radius-md);
      background: var(--bg-surface);
    }
    .course-row:hover {
      background: var(--bg-hover);
    }
    .course-row:last-child { border-bottom: none; }
    .course-thumb {
      width: 44px;
      height: 44px;
      border-radius: var(--border-radius-md);
      background: var(--brand-surface);
      color: var(--brand-primary);
      border: 1px solid var(--brand-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 13px;
      flex: 0 0 auto;
    }
    .course-main { flex: 1; min-width: 0; }
    .course-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
    .course-title-row h3 { margin: 0; font-size: 14px; font-weight: 600; color: var(--text-primary); }
    .badge-group { display: flex; align-items: center; gap: 6px; }
    .course-main p { margin: 4px 0 8px; font-size: 12px; color: var(--text-secondary); }
    .progress-track { height: 6px; border-radius: var(--border-radius-full); background: var(--bg-hover); overflow: hidden; }
    .progress-track span { display: block; height: 100%; background: var(--brand-primary); border-radius: var(--border-radius-full); }
    .progress-track span.is-completed-fill { background: var(--status-green-text); }
    .course-meta-end {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 0 0 auto;
    }
    .course-progress { font-size: 13px; font-weight: 700; color: var(--brand-primary); min-width: 48px; text-align: right; font-variant-numeric: tabular-nums; }
    .course-progress.is-completed-text { color: var(--status-green-text); }
    .btn-continue {
      padding: 6px 14px;
      border-radius: var(--border-radius-sm);
      font-size: 12px;
      font-weight: 600;
      background: var(--brand-surface);
      color: var(--brand-primary);
      border: 1px solid var(--brand-primary);
      cursor: pointer;
      transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
      white-space: nowrap;

      &:hover:not(:disabled) {
        background: var(--brand-primary);
        color: var(--text-on-brand, var(--bg-primary));
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
    .pill { font-size: 10.5px; padding: 2px 8px; border-radius: var(--border-radius-full); font-weight: 600; text-transform: capitalize; }
    .pill--live { background: var(--status-green-bg); color: var(--status-green-text); border: 1px solid var(--status-green-bg); }
    .pill--draft { background: var(--status-amber-bg); color: var(--status-amber-text); border: 1px solid var(--status-amber-bg); }
    .pill--pending { background: var(--status-red-bg); color: var(--status-red-text); border: 1px solid var(--status-red-bg); }
    .pill--archived { background: var(--status-grey-bg); color: var(--status-grey-text); border: 1px solid var(--status-grey-bg); }
    .pill--progress { background: var(--brand-surface); color: var(--brand-primary); border: 1px solid var(--brand-surface); font-weight: 600; }
    .pill--completed { background: var(--status-green-bg); color: var(--status-green-text); border: 1px solid var(--status-green-bg); font-weight: 700; }
  `]
})
export class CourseCardComponent {
  @Input() course: Course | null = null;
  @Input() progressPercent = 0;

  resuming = false;

  constructor(
    private studentService: StudentService,
    private router: Router
  ) {}

  get initials(): string {
    if (!this.course?.title) return '?';
    return this.course.title
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase())
      .join('');
  }

  get statusClass(): string {
    return `pill--${(this.course?.status || '').toLowerCase()}`;
  }

  get displayProgress(): number {
    return this.course?.progressPercent != null ? Math.round(this.course.progressPercent) : this.progressPercent;
  }

  get isCompleted(): boolean {
    return !!(this.course?.completed || this.displayProgress >= 100);
  }

  onContinue(event: Event): void {
    event.stopPropagation();
    if (!this.course) return;
    this.resuming = true;
    this.studentService.getResumeTarget(this.course.id).subscribe({
      next: (target: ResumeTarget) => {
        this.resuming = false;
        if (target.lectureId) {
          this.router.navigate(['/student/courses', this.course!.id, 'lectures', target.lectureId]);
        } else {
          this.router.navigate(['/student/courses', this.course!.id]);
        }
      },
      error: () => {
        this.resuming = false;
        this.router.navigate(['/student/courses', this.course!.id]);
      }
    });
  }
}
