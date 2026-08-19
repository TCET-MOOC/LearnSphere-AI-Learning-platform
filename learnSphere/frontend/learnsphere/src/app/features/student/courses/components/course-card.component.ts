import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Course } from '@core/models/course.model';

/**
 * CourseCardComponent renders a single enrolled course row for the student
 * "My Courses" list, including progress and a link into the course detail page.
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
            <span class="pill" [ngClass]="statusClass" *ngIf="!isCompleted">{{ course.status }}</span>
          </div>
        </div>
        <p>{{ course.teacherName || 'Instructor' }} · {{ course.department }} · {{ course.lectureCount || 0 }} lectures</p>
        <div class="progress-track" aria-hidden="true">
          <span [style.width.%]="displayProgress" [class.is-completed-fill]="isCompleted"></span>
        </div>
      </div>
      <div class="course-progress" [class.is-completed-text]="isCompleted">
        <span *ngIf="isCompleted">🏆 </span>{{ displayProgress }}%
      </div>
    </article>
  `,
  styles: [`
    .course-row {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px 8px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      cursor: pointer;
      text-decoration: none;
      color: inherit;
      transition: background 0.15s ease;
      border-radius: 8px;
    }
    .course-row:hover {
      background: rgba(255, 255, 255, 0.03);
    }
    .course-row:last-child { border-bottom: none; }
    .course-thumb {
      width: 44px;
      height: 44px;
      border-radius: 10px;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 13px;
      flex: 0 0 auto;
    }
    .course-main { flex: 1; min-width: 0; }
    .course-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
    .course-title-row h3 { margin: 0; font-size: 14px; font-weight: 600; color: #f1f5f9; }
    .badge-group { display: flex; align-items: center; gap: 6px; }
    .course-main p { margin: 4px 0 8px; font-size: 12px; color: #94a3b8; }
    .progress-track { height: 6px; border-radius: 4px; background: rgba(255, 255, 255, 0.08); overflow: hidden; }
    .progress-track span { display: block; height: 100%; background: #6366f1; border-radius: 4px; }
    .progress-track span.is-completed-fill { background: #10b981; }
    .course-progress { font-size: 13px; font-weight: 700; color: #818cf8; flex: 0 0 auto; }
    .course-progress.is-completed-text { color: #34d399; }
    .pill { font-size: 10.5px; padding: 2px 8px; border-radius: 20px; font-weight: 600; text-transform: capitalize; }
    .pill--live { background: rgba(52, 211, 153, 0.15); color: #34d399; }
    .pill--draft { background: rgba(245, 158, 11, 0.15); color: #fbbf24; }
    .pill--pending { background: rgba(239, 68, 68, 0.15); color: #f87171; }
    .pill--archived { background: rgba(148, 163, 184, 0.15); color: #94a3b8; }
    .pill--completed { background: rgba(52, 211, 153, 0.2); color: #34d399; font-weight: 700; }
  `]
})
export class CourseCardComponent {
  @Input() course: Course | null = null;
  @Input() progressPercent = 0;

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
}
