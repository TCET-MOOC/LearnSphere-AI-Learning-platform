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
          <span class="pill" [ngClass]="statusClass">{{ course.status }}</span>
        </div>
        <p>{{ course.teacherName || 'Instructor' }} · {{ course.department }} · {{ course.lectureCount || 0 }} lectures</p>
        <div class="progress-track" aria-hidden="true">
          <span [style.width.%]="progressPercent"></span>
        </div>
      </div>
      <div class="course-progress">{{ progressPercent }}%</div>
    </article>
  `,
  styles: [`
    .course-row {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 14px 4px;
      border-bottom: 1px solid #eeedf3;
      cursor: pointer;
      text-decoration: none;
      color: inherit;
    }
    .course-row:last-child { border-bottom: none; }
    .course-thumb {
      width: 44px;
      height: 44px;
      border-radius: 10px;
      background: #eeedfe;
      color: #534ab7;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 13px;
      flex: 0 0 auto;
    }
    .course-main { flex: 1; min-width: 0; }
    .course-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
    .course-title-row h3 { margin: 0; font-size: 13px; font-weight: 600; }
    .course-main p { margin: 4px 0 8px; font-size: 11.5px; color: #6b6880; }
    .progress-track { height: 5px; border-radius: 4px; background: #eeedf3; overflow: hidden; }
    .progress-track span { display: block; height: 100%; background: #534ab7; }
    .course-progress { font-size: 13px; font-weight: 700; color: #534ab7; flex: 0 0 auto; }
    .pill { font-size: 10px; padding: 2px 8px; border-radius: 20px; font-weight: 600; text-transform: capitalize; }
    .pill--live { background: #eaf3de; color: #1d9e75; }
    .pill--draft { background: #faeeda; color: #ba7517; }
    .pill--pending { background: #fcebeb; color: #a32d2d; }
    .pill--archived { background: #f1efe8; color: #6b6880; }
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
}
