import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TeacherService } from '../../services/teacher.service';
import { TeacherAssessmentService } from '../../services/assessment.service';
import { Course } from '@core/models/course.model';
import { QuestionDraft } from '@core/models/assessment.model';
import { NotificationService } from '@core/services/notification.service';

const QUESTION_TYPES = ['MCQ', 'SHORT_ANSWER', 'ESSAY'];

/**
 * QuizBuilderComponent lets a teacher build a Test (with Questions) against
 * one of their own courses: pick the course, set test-level details, then
 * add questions one at a time (MCQ questions collect options + the correct
 * answer so the backend can auto-grade; other types just collect marks).
 *
 * Self-contained (not yet wired into a parent template) — see the courses
 * dropdown for ownership scoping and NotificationService for feedback.
 */
@Component({
  selector: 'app-quiz-builder',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="quiz-builder">
      <h3>Build a test</h3>
      <p class="hint">Create a test for one of your courses, then add its questions below.</p>

      <div class="form-grid" *ngIf="!createdTestId">
        <label>
          Course
          <select [(ngModel)]="courseId" [disabled]="courses.length === 0">
            <option [ngValue]="null" disabled>Select a course…</option>
            <option *ngFor="let c of courses" [ngValue]="c.id">{{ c.title }}</option>
          </select>
        </label>
        <label>
          Test title
          <input type="text" [(ngModel)]="title" placeholder="e.g. Unit 3 quiz" />
        </label>
        <label>
          Duration (minutes)
          <input type="number" min="1" [(ngModel)]="durationMinutes" />
        </label>
        <label class="checkbox-row">
          <input type="checkbox" [(ngModel)]="isRemedial" />
          <span>This is a remedial / makeup test</span>
        </label>
      </div>

      <button
        type="button"
        class="btn btn--primary"
        *ngIf="!createdTestId"
        [disabled]="!courseId || !title || !durationMinutes || creatingTest"
        (click)="createTest()">
        {{ creatingTest ? 'Creating…' : 'Create test' }}
      </button>

      <div class="questions-section" *ngIf="createdTestId">
        <p class="status">Test "{{ title }}" created. Add its questions below.</p>

        <div class="question-list" *ngIf="savedQuestions.length > 0">
          <div class="saved-question" *ngFor="let q of savedQuestions; let i = index">
            <strong>{{ i + 1 }}. {{ q.body }}</strong>
            <span>{{ q.questionType }} · {{ q.marks }} mark{{ q.marks === 1 ? '' : 's' }}</span>
          </div>
        </div>

        <div class="form-grid">
          <label class="full">
            Question
            <textarea rows="2" [(ngModel)]="draftBody" placeholder="Question text…"></textarea>
          </label>
          <label>
            Type
            <select [(ngModel)]="draftType">
              <option *ngFor="let t of questionTypes" [value]="t">{{ t }}</option>
            </select>
          </label>
          <label>
            Marks
            <input type="number" min="1" [(ngModel)]="draftMarks" />
          </label>
        </div>

        <div class="options-section" *ngIf="draftType === 'MCQ'">
          <label class="full">Options</label>
          <div class="option-row" *ngFor="let opt of draftOptions; let i = index">
            <input type="text" [(ngModel)]="draftOptions[i]" placeholder="Option text" />
            <label class="correct-toggle">
              <input type="radio" name="correct-option" [checked]="draftCorrectAnswer === opt && opt !== ''" (change)="draftCorrectAnswer = opt" />
              Correct
            </label>
            <button type="button" class="btn-icon" (click)="removeOption(i)">✕</button>
          </div>
          <button type="button" class="btn btn--ghost" (click)="addOption()">+ Add option</button>
        </div>

        <label class="full" *ngIf="draftType !== 'MCQ'">
          Answer key (optional reference for grading short answers manually)
          <input type="text" [(ngModel)]="draftCorrectAnswer" placeholder="Model answer / key phrase" />
        </label>

        <div class="actions">
          <button
            type="button"
            class="btn btn--primary"
            [disabled]="!draftBody || !draftMarks || addingQuestion"
            (click)="addQuestion()">
            {{ addingQuestion ? 'Adding…' : '+ Add question' }}
          </button>
          <button type="button" class="btn btn--ghost" (click)="finishTest()">Done — start a new test</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .quiz-builder { background: #fff; border: 1px solid #e8e7ef; border-radius: 12px; padding: 18px; display: flex; flex-direction: column; gap: 14px; }
    h3 { margin: 0; font-size: 14px; }
    .hint { margin: 0; font-size: 11px; color: #6b6880; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form-grid label { display: flex; flex-direction: column; gap: 5px; font-size: 11px; color: #46435d; }
    .form-grid label.full { grid-column: 1 / -1; }
    .form-grid input, .form-grid select, .form-grid textarea {
      border: 1px solid #dedce7; border-radius: 7px; padding: 8px 10px; font: 12px 'Inter', Arial; color: #1a1830;
    }
    .checkbox-row { flex-direction: row !important; align-items: center; gap: 8px !important; }
    .btn { border: 0; border-radius: 8px; padding: 9px 16px; font: 600 12px 'Inter', Arial; cursor: pointer; align-self: flex-start; }
    .btn--primary { background: #534ab7; color: #fff; }
    .btn--primary:disabled { opacity: .5; cursor: not-allowed; }
    .btn--ghost { background: #fff; border: 1px solid #dddbe8; color: #46435d; }
    .status { font-size: 12px; color: #1d9e75; font-weight: 600; margin: 0; }
    .question-list { display: flex; flex-direction: column; gap: 6px; }
    .saved-question { display: flex; justify-content: space-between; gap: 10px; font-size: 12px; border-bottom: 1px solid #f0eff4; padding: 8px 0; }
    .saved-question span { color: #6b6880; white-space: nowrap; }
    .options-section { display: flex; flex-direction: column; gap: 8px; }
    .option-row { display: flex; align-items: center; gap: 10px; }
    .option-row input[type=text] { flex: 1; border: 1px solid #dedce7; border-radius: 7px; padding: 8px 10px; font: 12px 'Inter', Arial; }
    .correct-toggle { display: flex; align-items: center; gap: 4px; font-size: 11px; color: #46435d; white-space: nowrap; }
    .btn-icon { border: 0; background: transparent; color: #a32d2d; cursor: pointer; font-size: 13px; }
    .actions { display: flex; gap: 10px; }
  `]
})
export class QuizBuilderComponent implements OnInit {
  courses: Course[] = [];
  courseId: number | null = null;
  title = '';
  durationMinutes: number | null = null;
  isRemedial = false;
  creatingTest = false;
  createdTestId: number | null = null;

  questionTypes = QUESTION_TYPES;
  savedQuestions: QuestionDraft[] = [];

  draftBody = '';
  draftType = 'MCQ';
  draftMarks: number | null = 1;
  draftOptions: string[] = ['', ''];
  draftCorrectAnswer = '';
  addingQuestion = false;

  constructor(
    private teacherService: TeacherService,
    private teacherAssessmentService: TeacherAssessmentService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.teacherService.getCourses().subscribe({
      next: (courses) => (this.courses = courses),
      error: () => this.notificationService.error('Could not load your courses.')
    });
  }

  createTest(): void {
    if (!this.courseId || !this.title || !this.durationMinutes) return;
    this.creatingTest = true;
    this.teacherAssessmentService
      .createTest({
        course: { id: this.courseId },
        title: this.title,
        durationMinutes: this.durationMinutes,
        isRemedial: this.isRemedial
      })
      .subscribe({
        next: (test) => {
          this.createdTestId = test.testId;
          this.creatingTest = false;
          this.notificationService.success('Test created — now add its questions.');
        },
        error: () => {
          this.creatingTest = false;
          this.notificationService.error('Could not create the test.');
        }
      });
  }

  addOption(): void {
    this.draftOptions.push('');
  }

  removeOption(index: number): void {
    const removed = this.draftOptions[index];
    this.draftOptions.splice(index, 1);
    if (this.draftCorrectAnswer === removed) {
      this.draftCorrectAnswer = '';
    }
  }

  addQuestion(): void {
    if (!this.createdTestId || !this.draftBody || !this.draftMarks) return;
    const isMcq = this.draftType === 'MCQ';
    const options = isMcq ? this.draftOptions.filter((o) => o.trim().length > 0) : undefined;

    const question: QuestionDraft = {
      body: this.draftBody,
      questionType: this.draftType,
      marks: this.draftMarks,
      options,
      correctAnswer: this.draftCorrectAnswer || undefined
    };

    this.addingQuestion = true;
    this.teacherAssessmentService.addQuestion(this.createdTestId, question).subscribe({
      next: () => {
        this.savedQuestions.push(question);
        this.addingQuestion = false;
        this.resetDraft();
        this.notificationService.success('Question added.');
      },
      error: () => {
        this.addingQuestion = false;
        this.notificationService.error('Could not add that question.');
      }
    });
  }

  finishTest(): void {
    this.createdTestId = null;
    this.courseId = null;
    this.title = '';
    this.durationMinutes = null;
    this.isRemedial = false;
    this.savedQuestions = [];
    this.resetDraft();
  }

  private resetDraft(): void {
    this.draftBody = '';
    this.draftType = 'MCQ';
    this.draftMarks = 1;
    this.draftOptions = ['', ''];
    this.draftCorrectAnswer = '';
  }
}
