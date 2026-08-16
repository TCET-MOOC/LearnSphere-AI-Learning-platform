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
      background: #fff;
      border: 1px solid #e8e7ef;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
    }
    .course-card__head { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
    .course-id {
      width: 40px; height: 40px; border-radius: 10px;
      background: #eeedfe; color: #534ab7;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 12px; flex: 0 0 auto;
    }
    .course-title { flex: 1; min-width: 0; }
    .course-title h2 { margin: 0; font-size: 14px; }
    .course-title p { margin: 4px 0 0; font-size: 11.5px; color: #6b6880; }
    .course-actions { display: flex; gap: 8px; }
    .btn { border: none; border-radius: 8px; padding: 8px 12px; font: 600 11.5px Inter, Arial; cursor: pointer; text-decoration: none; }
    .btn--soft { background: #eeedfe; color: #534ab7; }
    .btn--danger { background: #fff0ee; color: #a32d2d; }
    .pill { font-size: 10px; padding: 3px 9px; border-radius: 20px; font-weight: 600; text-transform: capitalize; }
    .pill--live { background: #eaf3de; color: #1d9e75; }
    .pill--draft { background: #faeeda; color: #ba7517; }
    .pill--pending { background: #fcebeb; color: #a32d2d; }
    .pill--archived { background: #f1efe8; color: #6b6880; }
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
