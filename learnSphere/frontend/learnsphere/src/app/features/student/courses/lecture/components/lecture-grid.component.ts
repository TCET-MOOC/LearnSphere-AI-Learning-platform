import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Lecture } from '@core/models/course.model';
import { DurationPipe } from '@shared/pipes/duration.pipe';

/**
 * LectureGridComponent renders the playlist/curriculum of a course as a grid
 * of lecture cards, highlighting the one currently playing and showing a
 * completion tick for lectures the student has finished.
 */
@Component({
  selector: 'app-lecture-grid',
  standalone: true,
  imports: [CommonModule, DurationPipe],
  template: `
    <div class="lecture-grid">
      <button
        type="button"
        class="lecture-card"
        *ngFor="let lecture of lectures"
        [class.lecture-card--active]="lecture.id === currentLectureId"
        [class.lecture-card--done]="completedIds.has(lecture.id)"
        (click)="select.emit(lecture)">
        <span class="lecture-card__num">{{ lecture.number }}</span>
        <span class="lecture-card__body">
          <strong>{{ lecture.title }}</strong>
          <small>{{ lecture.duration | duration }}</small>
        </span>
        <span class="lecture-card__status" *ngIf="completedIds.has(lecture.id)">✓</span>
      </button>
      <p class="empty" *ngIf="lectures.length === 0">No lectures in this course yet.</p>
    </div>
  `,
  styles: [`
    .lecture-grid { display: flex; flex-direction: column; gap: 6px; }
    .lecture-card {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px;
      border: 1px solid #e8e7ef;
      border-radius: 9px;
      background: #fff;
      cursor: pointer;
      text-align: left;
      font: inherit;
    }
    .lecture-card:hover { border-color: #534ab7; }
    .lecture-card--active { border-color: #534ab7; background: #f6f5ff; }
    .lecture-card--done .lecture-card__num { background: #eaf3de; color: #1d9e75; }
    .lecture-card__num {
      width: 22px; height: 22px; border-radius: 50%;
      background: #eeedfe; color: #534ab7;
      display: flex; align-items: center; justify-content: center;
      font-size: 11px; font-weight: 700; flex: 0 0 auto;
    }
    .lecture-card__body { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .lecture-card__body strong { font-size: 12px; }
    .lecture-card__body small { font-size: 10.5px; color: #6b6880; }
    .lecture-card__status { color: #1d9e75; font-weight: 700; }
    .empty { font-size: 12px; color: #6b6880; }
  `]
})
export class LectureGridComponent {
  @Input() lectures: Lecture[] = [];
  @Input() currentLectureId: number | null = null;
  @Input() completedIds: Set<number> = new Set();
  @Output() select = new EventEmitter<Lecture>();
}
