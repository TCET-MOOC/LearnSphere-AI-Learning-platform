import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Course } from '@core/models/course.model';

/**
 * CourseCardComponent (teacher variant) renders a single course the teacher
 * owns, with quick links into course management/editing and a delete action.
 */
@Component({
  selector: 'app-teacher-course-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <article class="course-card" *ngIf="course">
      <div class="course-card__head">
        <div class="course-id">{{ initials }}</div>
        <div class="course-title">
          <h2>{{ course.title }}</h2>
          <p>{{ course.lectureCount || 0 }} lectures · {{ course.department }}</p>
        </div>
        <span class="pill" [ngClass]="'pill--' + (course.status || '').toLowerCase()">{{ course.status }}</span>
      </div>

      <div class="course-actions">
        <a class="btn btn--soft" [routerLink]="['/teacher/courses', course.id, 'manage']">Manage</a>
        <button class="btn btn--danger" type="button" (click)="remove.emit(course.id)">Delete</button>
      </div>
    </article>
  `,
  styles: [`
    .course-card {
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      box-shadow: var(--card-shadow);
      transition: all 0.2s ease;
    }
    .course-card:hover {
      border-color: var(--brand-primary);
      transform: translateY(-1px);
    }
    .course-card__head { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
    .course-id {
      width: 40px; height: 40px; border-radius: 10px;
      background: var(--brand-surface); color: var(--brand-primary);
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 12px; flex: 0 0 auto;
    }
    .course-title { flex: 1; min-width: 0; }
    .course-title h2 { margin: 0; font-size: 14px; font-weight: 600; color: var(--text-primary); }
    .course-title p { margin: 4px 0 0; font-size: 11.5px; color: var(--text-muted); }
    .course-actions { display: flex; gap: 8px; }
    .btn { border: 1px solid transparent; border-radius: 8px; padding: 8px 14px; font: 600 12px 'Inter', sans-serif; cursor: pointer; text-decoration: none; transition: all 0.15s ease; }
    .btn--soft { background: var(--brand-surface); color: var(--brand-primary); border-color: var(--border-color); }
    .btn--soft:hover { background: var(--brand-primary); color: #fff; }
    .btn--danger { background: var(--status-red-bg); color: var(--status-red-text); }
    .btn--danger:hover { background: var(--status-red-text); color: #fff; }
    .pill { font-size: 11px; padding: 3px 10px; border-radius: 20px; font-weight: 600; text-transform: capitalize; }
    .pill--live { background: var(--status-green-bg); color: var(--status-green-text); }
    .pill--draft { background: var(--status-amber-bg); color: var(--status-amber-text); }
    .pill--pending { background: var(--status-red-bg); color: var(--status-red-text); }
    .pill--archived { background: var(--bg-hover); color: var(--text-muted); }
  `]
})
export class CourseCardComponent {
  @Input() course: Course | null = null;
  @Output() remove = new EventEmitter<number>();

  get initials(): string {
    if (!this.course?.title) return '?';
    return this.course.title
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase())
      .join('');
  }
}
