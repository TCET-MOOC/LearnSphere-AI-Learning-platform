import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AttemptReview } from '@core/models/assessment.model';

const MCQ_TYPES = ['MCQ', 'MULTIPLE_CHOICE'];

/**
 * AssessmentReviewComponent shows the outcome of a finalized (or past)
 * attempt: overall score plus a per-question breakdown of the correct
 * answer vs. what the student submitted.
 */
@Component({
  selector: 'app-assessment-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="review" *ngIf="review">
      <div class="score-banner" [class.pass]="passed">
        <span class="score-value">{{ review.score ?? 0 }} / {{ review.maxScore }}</span>
        <span class="score-label">{{ passed ? 'Passed' : 'Needs improvement' }} · {{ percent }}%</span>
      </div>

      <div class="answer-list">
        <div class="answer-row" *ngFor="let a of review.answers; let i = index">
          <p class="q-body">{{ i + 1 }}. {{ a.questionBody }}</p>

          <ng-container *ngIf="isMcq(a.questionType)">
            <div class="option-list">
              <div
                class="option"
                *ngFor="let opt of a.options"
                [class.correct]="opt === a.correctAnswer"
                [class.wrong]="opt === a.studentAnswer && opt !== a.correctAnswer">
                <span>{{ opt }}</span>
                <span class="tag" *ngIf="opt === a.correctAnswer">Correct answer</span>
                <span class="tag tag--wrong" *ngIf="opt === a.studentAnswer && opt !== a.correctAnswer">Your answer</span>
              </div>
            </div>
          </ng-container>

          <ng-container *ngIf="!isMcq(a.questionType)">
            <p class="free-answer"><strong>Your answer:</strong> {{ a.studentAnswer || '(no answer)' }}</p>
          </ng-container>

          <p class="marks">{{ a.marksAwarded ?? 0 }} / {{ a.marks }} marks</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .review { display: flex; flex-direction: column; gap: 16px; }
    .score-banner {
      display: flex; flex-direction: column; align-items: center; gap: 4px;
      padding: 20px; border-radius: 12px; background: #fdece9; color: #a32d2d;
    }
    .score-banner.pass { background: #e1f5ee; color: #0f6e56; }
    .score-value { font-size: 26px; font-weight: 700; }
    .score-label { font-size: 12px; font-weight: 600; }
    .answer-list { display: flex; flex-direction: column; gap: 14px; }
    .answer-row { border: 1px solid #e8e7ef; border-radius: 10px; padding: 14px; }
    .q-body { margin: 0 0 10px; font-size: 13px; font-weight: 600; color: #1a1830; }
    .option-list { display: flex; flex-direction: column; gap: 6px; }
    .option {
      display: flex; align-items: center; justify-content: space-between; gap: 10px;
      border: 1px solid #e8e7ef; border-radius: 8px; padding: 8px 10px; font-size: 12.5px;
    }
    .option.correct { border-color: #1d9e75; background: #e1f5ee; }
    .option.wrong { border-color: #c5221f; background: #fdece9; }
    .tag { font-size: 10px; font-weight: 700; color: #0f6e56; }
    .tag--wrong { color: #a32d2d; }
    .free-answer { font-size: 12.5px; color: #46435d; margin: 0; }
    .marks { margin: 10px 0 0; font-size: 11px; font-weight: 700; color: #534ab7; }
  `]
})
export class AssessmentReviewComponent {
  @Input() review: AttemptReview | null = null;

  get passed(): boolean {
    if (!this.review || !this.review.maxScore) return false;
    return ((this.review.score ?? 0) / this.review.maxScore) * 100 >= 40;
  }

  get percent(): number {
    if (!this.review || !this.review.maxScore) return 0;
    return Math.round(((this.review.score ?? 0) / this.review.maxScore) * 100);
  }

  isMcq(type: string): boolean {
    return MCQ_TYPES.includes((type || '').toUpperCase());
  }
}
