import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Question } from '@core/models/assessment.model';

export interface AnswerChange {
  questionId: number;
  answerText: string;
}

const MCQ_TYPES = ['MCQ', 'MULTIPLE_CHOICE'];

/**
 * QuizInterfaceComponent renders a test's questions one at a time and
 * collects the student's answers, emitting an event as each one is
 * answered (so the parent can persist it immediately) and a final "submit"
 * event once the student reaches the last question and confirms.
 */
@Component({
  selector: 'app-quiz-interface',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="quiz-interface" *ngIf="questions.length > 0">
      <div class="progress-bar">
        <span [style.width.%]="((currentIndex + 1) / questions.length) * 100"></span>
      </div>
      <p class="q-counter">Question {{ currentIndex + 1 }} of {{ questions.length }} · {{ current.marks }} mark{{ current.marks === 1 ? '' : 's' }}</p>

      <div class="q-body">{{ current.body }}</div>

      <div class="q-answer" *ngIf="isMcq(current); else freeText">
        <label class="option" *ngFor="let opt of current.options" [class.selected]="answers[current.questionId] === opt">
          <input
            type="radio"
            [name]="'q-' + current.questionId"
            [value]="opt"
            [checked]="answers[current.questionId] === opt"
            (change)="onSelect(current.questionId, opt)" />
          <span>{{ opt }}</span>
        </label>
      </div>

      <ng-template #freeText>
        <textarea
          class="answer-input"
          rows="4"
          placeholder="Type your answer…"
          [ngModel]="answers[current.questionId]"
          (ngModelChange)="onDraft(current.questionId, $event)"
          (blur)="onCommit(current.questionId)"
        ></textarea>
      </ng-template>

      <div class="q-nav">
        <button type="button" class="btn btn--ghost" [disabled]="currentIndex === 0" (click)="prev()">Previous</button>
        <button type="button" class="btn btn--primary" *ngIf="currentIndex < questions.length - 1" (click)="next()">Next</button>
        <button type="button" class="btn btn--success" *ngIf="currentIndex === questions.length - 1" (click)="finish.emit()">
          Submit test
        </button>
      </div>
    </div>
  `,
  styles: [`
    .quiz-interface { display: flex; flex-direction: column; gap: 14px; }
    .progress-bar { height: 6px; border-radius: 4px; background: #eeedf3; overflow: hidden; }
    .progress-bar span { display: block; height: 100%; background: #534ab7; transition: width .2s ease; }
    .q-counter { margin: 0; font-size: 11px; color: #6b6880; font-weight: 600; }
    .q-body { font-size: 15px; font-weight: 600; color: #1a1830; line-height: 1.5; }
    .q-answer { display: flex; flex-direction: column; gap: 8px; }
    .option {
      display: flex; align-items: center; gap: 10px;
      border: 1px solid #e8e7ef; border-radius: 8px; padding: 10px 12px;
      cursor: pointer; font-size: 13px; color: #1a1830;
    }
    .option.selected { border-color: #534ab7; background: #eeedfe; }
    .option input { accent-color: #534ab7; }
    .answer-input {
      width: 100%; border: 1px solid #dedce7; border-radius: 8px; padding: 10px 12px;
      font: 13px 'Inter', Arial; resize: vertical; color: #1a1830;
    }
    .q-nav { display: flex; justify-content: space-between; gap: 10px; margin-top: 8px; }
    .btn { border: 0; border-radius: 8px; padding: 9px 16px; font: 600 12px 'Inter', Arial; cursor: pointer; }
    .btn--ghost { background: #fff; border: 1px solid #dddbe8; color: #46435d; }
    .btn--ghost:disabled { opacity: .5; cursor: not-allowed; }
    .btn--primary { background: #534ab7; color: #fff; margin-left: auto; }
    .btn--success { background: #1d9e75; color: #fff; margin-left: auto; }
  `]
})
export class QuizInterfaceComponent implements OnChanges {
  @Input() questions: Question[] = [];
  /** Map of questionId -> already-saved answer text, keyed as string for JSON-friendliness. */
  @Input() initialAnswers: Record<number, string> = {};
  @Output() answerChanged = new EventEmitter<AnswerChange>();
  @Output() finish = new EventEmitter<void>();

  currentIndex = 0;
  answers: Record<number, string> = {};

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialAnswers']) {
      this.answers = { ...this.initialAnswers };
    }
  }

  get current(): Question {
    return this.questions[this.currentIndex];
  }

  isMcq(question: Question): boolean {
    return MCQ_TYPES.includes((question.questionType || '').toUpperCase());
  }

  onSelect(questionId: number, option: string): void {
    this.answers[questionId] = option;
    this.answerChanged.emit({ questionId, answerText: option });
  }

  onDraft(questionId: number, value: string): void {
    this.answers[questionId] = value;
  }

  onCommit(questionId: number): void {
    this.answerChanged.emit({ questionId, answerText: this.answers[questionId] || '' });
  }

  next(): void {
    if (this.currentIndex < this.questions.length - 1) this.currentIndex++;
  }

  prev(): void {
    if (this.currentIndex > 0) this.currentIndex--;
  }
}
