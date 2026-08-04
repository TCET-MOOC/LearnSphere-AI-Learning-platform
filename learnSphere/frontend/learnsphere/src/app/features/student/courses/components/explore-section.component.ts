import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Course } from '@core/models/course.model';

/**
 * ExploreSectionComponent renders the "browse more courses" grid on the
 * student courses page — public/live courses the student is not yet
 * enrolled in, with a one-click enroll action for free courses.
 */
@Component({
  selector: 'app-explore-section',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="explore-grid">
      <article class="explore-card" *ngFor="let course of courses">
        <div class="explore-card__top">
          <span class="mini-mark">{{ course.title.slice(0, 2) }}</span>
          <span class="rating" *ngIf="course.price === 0">Free</span>
          <span class="rating" *ngIf="course.price > 0">₹{{ course.price }}</span>
        </div>
        <h3 [routerLink]="['/student/courses', course.id]">{{ course.title }}</h3>
        <p>{{ course.teacherName || 'Instructor' }} · {{ course.department }}</p>
        <button
          class="btn btn--soft"
          type="button"
          [disabled]="enrollingId === course.id"
          (click)="enroll.emit(course.id)">
          {{ enrollingId === course.id ? 'Enrolling…' : (course.price > 0 ? 'View course' : 'Enroll free') }}
        </button>
      </article>

      <p class="explore-empty" *ngIf="!courses || courses.length === 0">No courses to explore right now.</p>
    </div>
  `,
  styles: [`
    .explore-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 14px;
    }
    .explore-card {
      background: #fff;
      border: 1px solid #e8e7ef;
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .explore-card__top { display: flex; align-items: center; justify-content: space-between; }
    .mini-mark {
      width: 32px; height: 32px; border-radius: 9px;
      background: #eeedfe; color: #534ab7;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 12px; text-transform: uppercase;
    }
    .rating { font-size: 11px; font-weight: 600; color: #1d9e75; }
    h3 { margin: 4px 0 0; font-size: 13px; cursor: pointer; }
    p { margin: 0 0 6px; font-size: 11.5px; color: #6b6880; }
    .btn {
      border: none; border-radius: 8px; padding: 8px 10px; font: 600 11.5px Inter, Arial; cursor: pointer;
    }
    .btn--soft { background: #eeedfe; color: #534ab7; }
    .btn--soft:disabled { opacity: 0.6; cursor: default; }
    .explore-empty { color: #6b6880; font-size: 12px; grid-column: 1 / -1; }
  `]
})
export class ExploreSectionComponent {
  @Input() courses: Course[] = [];
  @Input() enrollingId: number | null = null;
  @Output() enroll = new EventEmitter<number>();
}
