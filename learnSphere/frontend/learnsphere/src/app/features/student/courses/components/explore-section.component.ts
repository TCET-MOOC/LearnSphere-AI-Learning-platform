import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Course } from '@core/models/course.model';

@Component({
  selector: 'app-explore-section',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="explore-grid">
      <article class="explore-card" *ngFor="let course of courses" [routerLink]="['/student/courses', course.id]">
        <div class="explore-card__top">
          <div class="mini-mark" [style.background]="getGradient(course.id)">
            {{ course.title ? course.title.slice(0, 2).toUpperCase() : 'CO' }}
          </div>
          <span class="price-tag" [class.is-free]="!course.price || course.price === 0">
            {{ (!course.price || course.price === 0) ? 'Free' : ('₹' + course.price) }}
          </span>
        </div>

        <div class="explore-card__body">
          <h3 class="course-title">{{ course.title }}</h3>
          <p class="course-meta">{{ course.teacherName || 'Faculty Instructor' }} · {{ course.department || 'General' }}</p>
        </div>

        <div class="explore-card__footer" (click)="$event.stopPropagation()">
          <button
            class="btn btn--explore"
            type="button"
            [disabled]="enrollingId === course.id"
            (click)="onAction(course, $event)">
            {{ enrollingId === course.id ? 'Processing…' : (course.price > 0 ? 'View Course & Enroll' : 'Enroll Free') }}
          </button>
        </div>
      </article>

      <p class="explore-empty" *ngIf="!courses || courses.length === 0">
        No additional courses available to explore right now.
      </p>
    </div>
  `,
  styles: [`
    .explore-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 16px;
      margin-top: 10px;
    }

    .explore-card {
      background: #1a1f2e;
      background: rgba(26, 31, 46, 0.95);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 14px;
      padding: 18px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      cursor: pointer;
      transition: all 0.22s cubic-bezier(0.16, 1, 0.3, 1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      position: relative;
      overflow: hidden;
    }

    .explore-card:hover {
      transform: translateY(-3px);
      border-color: rgba(99, 102, 241, 0.45);
      box-shadow: 0 10px 24px rgba(0, 0, 0, 0.35);
      background: rgba(30, 36, 54, 0.98);
    }

    .explore-card__top {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .mini-mark {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 13px;
      letter-spacing: 0.5px;
      box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
    }

    .price-tag {
      font-size: 13px;
      font-weight: 700;
      color: #34d399;
      background: rgba(52, 211, 153, 0.12);
      border: 1px solid rgba(52, 211, 153, 0.25);
      padding: 4px 10px;
      border-radius: 20px;
      letter-spacing: 0.2px;
    }

    .price-tag.is-free {
      color: #a78bfa;
      background: rgba(167, 139, 250, 0.12);
      border-color: rgba(167, 139, 250, 0.25);
    }

    .explore-card__body {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .course-title {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
      color: #f1f5f9;
      line-height: 1.35;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .course-meta {
      margin: 0;
      font-size: 12.5px;
      color: #94a3b8;
      line-height: 1.4;
    }

    .explore-card__footer {
      margin-top: 4px;
    }

    .btn--explore {
      width: 100%;
      border: 1px solid rgba(99, 102, 241, 0.35);
      background: rgba(99, 102, 241, 0.12);
      color: #a5b4fc;
      border-radius: 9px;
      padding: 9px 12px;
      font-size: 12.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.18s ease;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .btn--explore:hover:not(:disabled) {
      background: #6366f1;
      color: #ffffff;
      border-color: #6366f1;
      box-shadow: 0 4px 14px rgba(99, 102, 241, 0.4);
    }

    .btn--explore:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .explore-empty {
      color: #94a3b8;
      font-size: 13px;
      grid-column: 1 / -1;
      padding: 16px 0;
    }
  `]
})
export class ExploreSectionComponent {
  @Input() courses: Course[] = [];
  @Input() enrollingId: number | null = null;
  @Output() enroll = new EventEmitter<number>();

  private gradients = [
    'linear-gradient(135deg, #6366f1, #8b5cf6)',
    'linear-gradient(135deg, #3b82f6, #06b6d4)',
    'linear-gradient(135deg, #10b981, #059669)',
    'linear-gradient(135deg, #f59e0b, #d97706)',
    'linear-gradient(135deg, #ec4899, #8b5cf6)'
  ];

  constructor(private router: Router) {}

  getGradient(id: number): string {
    return this.gradients[id % this.gradients.length];
  }

  onAction(course: Course, event: Event): void {
    event.stopPropagation();
    if (course.price && course.price > 0) {
      this.router.navigate(['/student/courses', course.id]);
    } else {
      this.enroll.emit(course.id);
    }
  }
}
